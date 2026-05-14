package org.ihtsdo.rvf.importer;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Imports assertion groups from {@code manifest.xml} by reading the first
 * {@code assertionGroupingStrategy} block and assigning each {@link Assertion} to zero or more
 * {@link AssertionGroup}s according to {@code group} element rules.
 *
 * <p>Manifest layout (see {@link AssertionGroupingXml} for element and attribute names):
 * <ul>
 *   <li>{@code policyValues/policy} &mdash; named lists of assertion UUIDs referenced by
 *       {@code excludeByPolicy} and {@code includeByPolicy} on each group.</li>
 *   <li>{@code group} &mdash; one element per group name; groups without a non-blank {@code name}
 *       are skipped when loading the manifest.</li>
 * </ul>
 *
 * <p>For a given assertion and group definition, membership is decided in order:
 * <ol>
 *   <li><strong>Exclusions</strong> &mdash; if any applies, the assertion is not in the group:
 *       UUID listed under a named policy in {@code excludeByPolicy} (comma-separated policy names);
 *       assertion text contains any {@code excludeAssertionKeywords} phrase (pipe-separated);
 *       assertion keywords (comma-separated) contain any token from {@code excludeCategories}.</li>
 *   <li><strong>{@code includeByPolicy}</strong> &mdash; if the attribute is set and the assertion UUID
 *       appears in any named policy list, the assertion is included.</li>
 *   <li><strong>{@code includeStandaloneCategories}</strong> &mdash; overlap between the group's token
 *       list and the assertion's keyword tokens, with special handling for the default category
 *       keywords ({@code file-centric-validation}, {@code component-centric-validation},
 *       {@code release-type-validation}).</li>
 *   <li><strong>{@code includeCategoryWithCentre}</strong> &mdash; pipe-separated {@code category,centre}
 *       pairs; a pair matches when both tokens appear in the assertion keywords, the category is one
 *       of the default three, and the centre token is not one of those defaults.</li>
 * </ol>
 * If no inclusion rule matches, the assertion is not added to that group.
 *
 * <p>On import, existing groups are cleared of assertions, missing groups are created, and only groups
 * listed in the manifest are populated; other groups in the database are removed from the working set
 * for this pass.
 */
@Service
@Transactional
public class AssertionGroupImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionGroupImporter.class);

    private final AssertionService assertionService;

    private Map<String, List<String>> manifestPolicyOverrides = Map.of();
    private List<Element> manifestGroupElements = List.of();
    /** Keyword tokens treated as default "category" dimensions for standalone and centre matching. */
    private final List<String> defaultStandaloneCategories = List.of("file-centric-validation", "component-centric-validation", "release-type-validation");

    /**
     * @param assertionService persistence and group membership API
     */
    @Autowired
    public AssertionGroupImporter(AssertionService assertionService) {
        this.assertionService = assertionService;
    }

    /**
     * Parses {@code assertionGroupingStrategy} from the manifest stream, then rebuilds group
     * membership in the database for every group defined in the manifest.
     *
     * @param manifestInputStream XML manifest; if null or unparsable, policy and group definitions
     *                            may be empty and the subsequent import step will throw if no
     *                            groups were loaded
     */
    public void importAssertionGroups(InputStream manifestInputStream) {
        loadManifestConfiguration(manifestInputStream);
        importAssertionGroupsCore();
    }

    /**
     * Loads the first {@code assertionGroupingStrategy} in the document: policy UUID lists and
     * non-blank {@code group} elements. Parsing errors are logged and leave configuration empty.
     *
     * @param manifestInputStream manifest XML stream, may be null
     */
    private void loadManifestConfiguration(InputStream manifestInputStream) {
        manifestPolicyOverrides = new HashMap<>();
        manifestGroupElements = List.of();
        if (manifestInputStream == null) {
            return;
        }
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            Document doc = saxBuilder.build(manifestInputStream);
            XPathFactory xpf = XPathFactory.instance();
            String stratExpr = "//" + AssertionGroupingXml.STRATEGY_ELEMENT;
            XPathExpression<Element> stratPath = xpf.compile(stratExpr, new ElementFilter(AssertionGroupingXml.STRATEGY_ELEMENT));
            List<Element> strategies = stratPath.evaluate(doc);
            if (strategies.isEmpty()) {
                return;
            }
            Element strategy = strategies.get(0);
            Element policyValuesEl = strategy.getChild(AssertionGroupingXml.POLICY_VALUES_ELEMENT);
            if (policyValuesEl != null) {
                for (Element policy : policyValuesEl.getChildren(AssertionGroupingXml.POLICY_ELEMENT)) {
                    String name = policy.getAttributeValue(AssertionGroupingXml.ATTR_NAME);
                    String uuids = policy.getAttributeValue(AssertionGroupingXml.ATTR_ASSERTION_UUIDS);
                    if (name != null && uuids != null && !uuids.isBlank()) {
                        manifestPolicyOverrides.put(name, parseCommaUuidList(uuids));
                    }
                }
            }
            List<Element> groups = new ArrayList<>();
            for (Element g : strategy.getChildren(AssertionGroupingXml.GROUP_ELEMENT)) {
                String gn = g.getAttributeValue(AssertionGroupingXml.ATTR_NAME);
                if (gn != null && !gn.isBlank()) {
                    groups.add(g);
                }
            }
            manifestGroupElements = groups;
        } catch (JDOMException | IOException e) {
            LOGGER.warn("Failed to parse assertionGroupingStrategy from manifest: {}", e.getMessage());
        }
    }

    /** Splits a comma-separated UUID list from manifest policy definitions. */
    private static List<String> parseCommaUuidList(String csv) {
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /** Splits a comma-separated attribute value into trimmed non-empty tokens. */
    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** UUIDs declared for a policy name in the loaded manifest, or empty if absent. */
    private List<String> policyUuidsFromManifest(String policyKey) {
        List<String> fromManifest = manifestPolicyOverrides.get(policyKey);
        if (fromManifest == null || fromManifest.isEmpty()) {
            return List.of();
        }
        return fromManifest;
    }

    private static String normalizePolicyToken(String token) {
        return token == null ? "" : token.trim();
    }

    /** @return whether {@code uuid} is listed under the named manifest policy */
    private boolean uuidInPolicyList(String policyName, String uuid) {
        return policyUuidsFromManifest(normalizePolicyToken(policyName)).contains(uuid);
    }

    /**
     * Inclusion via {@code includeStandaloneCategories}: after optional default-category shortcut,
     * any remaining token from the group list that appears in assertion keywords (excluding default
     * categories from both sides for the overlap pass) yields a match.
     */
    private boolean matchesIncludeStandaloneCategories(Assertion assertion, Element group) {
        String includeStandaloneCategoriesCsv = group.getAttributeValue(AssertionGroupingXml.ATTR_INCLUDE_STANDALONE_CATEGORIES);
        if (includeStandaloneCategoriesCsv == null || includeStandaloneCategoriesCsv.isBlank()) {
            return false;
        }
        String assertionKeywords = assertion.getKeywords();
        List<String> assertionKeywordList = new ArrayList<>(splitCsv(assertionKeywords));
        List<String> tokenList = new ArrayList<>(splitCsv(includeStandaloneCategoriesCsv));
        for (String t : defaultStandaloneCategories) {
            String s = t.trim();
            if (assertionKeywords.equals(s) && tokenList.contains(s)) {
                return true;
            }
        }
        assertionKeywordList.removeAll(defaultStandaloneCategories);
        tokenList.removeAll(defaultStandaloneCategories);
        for (String t : tokenList) {
            String s = t.trim();
            if (assertionKeywordList.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inclusion via {@code includeCategoryWithCentre}: {@code group}'s attribute is a {@code |}-separated
     * list of {@code category,centre} pairs (comma within each pair). A pair matches when both tokens
     * appear in the assertion's comma-separated keywords, {@code category} is one of the default
     * standalone category keywords, and {@code centre} is not one of those defaults.
     */
    private boolean matchesConjunctionCentresAndCategories(Assertion assertion, Element group) {
        String assertionKeywords = assertion.getKeywords();
        String includeCategoryWithCentreCsv = group.getAttributeValue(AssertionGroupingXml.ATTR_INCLUDE_CATEGORY_WITH_CENTRE);
        List<String> tokenList = Arrays.stream(includeCategoryWithCentreCsv.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        List<String> categoryTokens = splitCsv(assertionKeywords);
        for (String raw : tokenList) {
            String t = raw.trim();
            List<String> categoryWithCentres = splitCsv(t);
            if (categoryWithCentres.size() == 2
                    && defaultStandaloneCategories.contains(categoryWithCentres.get(0))
                    && !defaultStandaloneCategories.contains(categoryWithCentres.get(1))
                    && new HashSet<>(categoryTokens).containsAll(categoryWithCentres)) {
                return true;
            }
        }

        return false;
    }

    /**
     * True if the assertion should be excluded: UUID hits an {@code excludeByPolicy} policy,
     * assertion text contains any {@code excludeAssertionKeywords} phrase, or assertion keywords
     * contain any {@code excludeCategories} token.
     */
    private boolean violatesExcludeByPolicy(Assertion assertion, String excludeByPolicyCsv, String excludeAssertionKeywords, String excludeCategories) {
        if (uuidExcludedByNamedPolicies(assertion.getUuid().toString(), excludeByPolicyCsv)) {
            return true;
        }
        if (assertionTextMatchesExcludedPhrases(assertion.getAssertionText(), excludeAssertionKeywords)) {
            return true;
        }
        return assertionKeywordsMatchExcludedCategories(assertion, excludeCategories);
    }

    private boolean uuidExcludedByNamedPolicies(String uuid, String excludeByPolicyCsv) {
        if (!StringUtils.hasLength(excludeByPolicyCsv)) {
            return false;
        }
        for (String raw : splitCsv(excludeByPolicyCsv)) {
            String token = raw.trim();
            if (uuidInPolicyList(token, uuid)) {
                return true;
            }
        }
        return false;
    }

    private boolean assertionTextMatchesExcludedPhrases(String text, String excludeAssertionKeywords) {
        if (!StringUtils.hasLength(excludeAssertionKeywords)) {
            return false;
        }
        List<String> phrases = Arrays.stream(excludeAssertionKeywords.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        String lower = text.toLowerCase();
        for (String phrase : phrases) {
            if (lower.contains(phrase.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean assertionKeywordsMatchExcludedCategories(Assertion assertion, String excludeCategories) {
        if (!StringUtils.hasLength(excludeCategories)) {
            return false;
        }
        List<String> assertionKeywordList = splitCsv(assertion.getKeywords());
        for (String raw : splitCsv(excludeCategories)) {
            String token = raw.trim();
            if (assertionKeywordList.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** True if the assertion UUID appears in any comma-separated named policy from {@code includeByPolicyCsv}. */
    private boolean matchesIncludeByPolicy(Assertion assertion, String includeByPolicyCsv) {
        String uuid = assertion.getUuid().toString();
        for (String raw : splitCsv(includeByPolicyCsv)) {
            String token = raw.trim();
            if (uuidInPolicyList(token, uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code assertion} belongs in the group described by {@code group}'s XML attributes:
     * exclusions first, then {@code includeByPolicy}, {@code includeStandaloneCategories}, and
     * {@code includeCategoryWithCentre} as documented on this class.
     */
    private boolean assertionMatchesGroupRule(Assertion assertion, Element group) {
        if (violatesExcludeByPolicy(assertion, group.getAttributeValue(AssertionGroupingXml.ATTR_EXCLUDE_BY_POLICY), group.getAttributeValue(AssertionGroupingXml.ATTR_ASSERTION_KEYWORDS), group.getAttributeValue(AssertionGroupingXml.ATTR_EXCLUDE_CATEGORIES))) {
            return false;
        }

        String includeByPolicyCsv = group.getAttributeValue(AssertionGroupingXml.ATTR_INCLUDE_BY_POLICY);
        if (StringUtils.hasLength(includeByPolicyCsv) && matchesIncludeByPolicy(assertion, includeByPolicyCsv)) {
            return true;
        }

        if (matchesIncludeStandaloneCategories(assertion, group)) {
            return true;
        }

        String includeCategoryWithCentreCsv = group.getAttributeValue(AssertionGroupingXml.ATTR_INCLUDE_CATEGORY_WITH_CENTRE);
        if (StringUtils.hasLength(includeCategoryWithCentreCsv)) {
            return matchesConjunctionCentresAndCategories(assertion, group);
        }

        return false;
    }

    /**
     * Clears existing manifest-defined groups, creates missing groups, drops assertions from groups
     * not in the manifest, and repopulates membership from the in-memory group elements loaded
     * from the manifest.
     *
     * @throws IllegalStateException if no group definitions were loaded from the manifest
     */
    private void importAssertionGroupsCore() {
        if (manifestGroupElements.isEmpty()) {
            throw new IllegalStateException("manifest.xml must define <assertionGroupingStrategy> with one or more <group> elements.");
        }
        List<AssertionGroup> allGroups = assertionService.getAllAssertionGroups();
        if (allGroups == null) {
            allGroups = new ArrayList<>();
        }
        List<String> existingGroups = new ArrayList<>();
        for (AssertionGroup group : allGroups) {
            LOGGER.info("Validation group is already created: {}", group.getName());
            group.removeAllAssertionsFromGroup();
            existingGroups.add(group.getName());
        }

        List<String> allGroupNames = manifestGroupElements.stream().map(ge -> ge.getAttributeValue(AssertionGroupingXml.ATTR_NAME)).collect(Collectors.toCollection(ArrayList::new));

        for (String groupName : allGroupNames) {
            if (!existingGroups.contains(groupName)) {
                LOGGER.info("creating assertion group: {}", groupName);
                AssertionGroup group = new AssertionGroup();
                group.setName(groupName);
                allGroups.add(assertionService.createAssertionGroup(group));
            }
        }

        List<Assertion> allAssertions = assertionService.findAll();
        Set<String> nameSet = new LinkedHashSet<>(allGroupNames);
        allGroups = allGroups.stream().filter(ag -> nameSet.contains(ag.getName())).collect(Collectors.toList());

        Map<String, Element> groupDefByName = manifestGroupElements.stream()
                .collect(Collectors.toMap(ge -> ge.getAttributeValue(AssertionGroupingXml.ATTR_NAME), ge -> ge, (a, b) -> a));

        for (AssertionGroup group : allGroups) {
            Element def = groupDefByName.get(group.getName());
            if (def == null) {
                LOGGER.warn("Missing <group> definition for name {}", group.getName());
                continue;
            }
            int added = 0;
            for (Assertion assertion : allAssertions) {
                if (assertionMatchesGroupRule(assertion, def)) {
                    assertionService.addAssertionToGroup(assertion, group);
                    added++;
                }
            }
            LOGGER.info("Total assertions added {} for assertion group {}", added, group.getName());
        }
    }
}
