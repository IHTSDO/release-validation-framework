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
 * Imports assertion groups from {@link AssertionGroupingXml#GROUPS_RESOURCE_FILENAME} and
 * {@link AssertionGroupingXml#POLICIES_RESOURCE_FILENAME}, assigning each {@link Assertion} to zero
 * or more {@link AssertionGroup}s according to {@code group} element rules.
 *
 * <p>Layout (see {@link AssertionGroupingXml} for element and attribute names):
 * <ul>
 *   <li>{@code policies.xml} &mdash; {@code policyValues/policy} named policies with
 *       {@code assertionUuids}, {@code assertionTextPhrases}, and/or {@code categoryWithCentrePairs}.</li>
 *   <li>{@code groups.xml} &mdash; {@code assertionGroupingStrategy/group} elements referencing
 *       policies via {@code excludeByPolicy} and {@code includeByPolicy}.</li>
 * </ul>
 *
 * <p>For a given assertion and group definition, membership is decided in order:
 * <ol>
 *   <li><strong>Exclusions</strong> &mdash; if any applies, the assertion is not in the group:
 *       UUID listed under a named policy in {@code excludeByPolicy}, or assertion text matches a
 *       named {@code assertionTextPhrases} policy (comma-separated policy names);
 *       assertion keywords (comma-separated) contain any token from {@code excludeCategories}.</li>
 *   <li><strong>{@code includeByPolicy}</strong> &mdash; assertion is included when any named policy
 *       matches by UUID list, or by {@code categoryWithCentrePairs} (category and centre both present
 *       in assertion keywords, category is a default validation category, centre is not).</li>
 *   <li><strong>{@code includeStandaloneCategories}</strong> &mdash; overlap between the group's token
 *       list and the assertion's keyword tokens, with special handling for the default category
 *       keywords ({@code file-centric-validation}, {@code component-centric-validation},
 *       {@code release-type-validation}).</li>
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

    private Map<String, List<String>> manifestPolicyUuids = Map.of();
    private Map<String, List<String>> manifestPolicyTextPhrases = Map.of();
    private Map<String, List<List<String>>> manifestPolicyCategoryCentrePairs = Map.of();
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
     * Loads policies and groups from the assertions package XML files, then rebuilds group
     * membership in the database.
     *
     * @param groupsInputStream {@link AssertionGroupingXml#GROUPS_RESOURCE_FILENAME} content
     * @param policiesInputStream {@link AssertionGroupingXml#POLICIES_RESOURCE_FILENAME} content
     */
    public void importAssertionGroups(InputStream groupsInputStream, InputStream policiesInputStream) {
        manifestPolicyUuids = new HashMap<>();
        manifestPolicyTextPhrases = new HashMap<>();
        manifestPolicyCategoryCentrePairs = new HashMap<>();
        manifestGroupElements = List.of();
        loadPoliciesConfiguration(policiesInputStream);
        loadGroupsConfiguration(groupsInputStream);
        importAssertionGroupsCore();
    }

    private SAXBuilder createSecureSaxBuilder() {
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return saxBuilder;
    }

    private void loadPoliciesConfiguration(InputStream policiesInputStream) {
        if (policiesInputStream == null) {
            return;
        }
        try {
            Document doc = createSecureSaxBuilder().build(policiesInputStream);
            Element policyValuesEl = doc.getRootElement();
            if (!AssertionGroupingXml.POLICY_VALUES_ELEMENT.equals(policyValuesEl.getName())) {
                XPathFactory xpf = XPathFactory.instance();
                String expr = "//" + AssertionGroupingXml.POLICY_VALUES_ELEMENT;
                XPathExpression<Element> path = xpf.compile(expr, new ElementFilter(AssertionGroupingXml.POLICY_VALUES_ELEMENT));
                List<Element> found = path.evaluate(doc);
                if (found.isEmpty()) {
                    LOGGER.warn("No {} element in {}", AssertionGroupingXml.POLICY_VALUES_ELEMENT,
                            AssertionGroupingXml.POLICIES_RESOURCE_FILENAME);
                    return;
                }
                policyValuesEl = found.get(0);
            }
            loadPoliciesFromElement(policyValuesEl);
        } catch (JDOMException | IOException e) {
            LOGGER.warn("Failed to parse policies from {}: {}", AssertionGroupingXml.POLICIES_RESOURCE_FILENAME, e.getMessage());
        }
    }

    private void loadPoliciesFromElement(Element policyValuesEl) {
        for (Element policy : policyValuesEl.getChildren(AssertionGroupingXml.POLICY_ELEMENT)) {
            String name = policy.getAttributeValue(AssertionGroupingXml.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            String uuids = policy.getAttributeValue(AssertionGroupingXml.ATTR_ASSERTION_UUIDS);
            if (uuids != null && !uuids.isBlank()) {
                manifestPolicyUuids.put(name, parseCommaUuidList(uuids));
            }
            String textPhrases = policy.getAttributeValue(AssertionGroupingXml.ATTR_ASSERTION_TEXT_PHRASES);
            if (textPhrases != null && !textPhrases.isBlank()) {
                manifestPolicyTextPhrases.put(name, parsePipeSeparatedPhrases(textPhrases));
            }
            String categoryCentrePairs = policy.getAttributeValue(AssertionGroupingXml.ATTR_CATEGORY_WITH_CENTRE_PAIRS);
            if (categoryCentrePairs != null && !categoryCentrePairs.isBlank()) {
                manifestPolicyCategoryCentrePairs.put(name, parseCategoryWithCentrePairs(categoryCentrePairs));
            }
        }
    }

    private void loadGroupsConfiguration(InputStream groupsInputStream) {
        if (groupsInputStream == null) {
            return;
        }
        try {
            Document doc = createSecureSaxBuilder().build(groupsInputStream);
            XPathFactory xpf = XPathFactory.instance();
            String stratExpr = "//" + AssertionGroupingXml.STRATEGY_ELEMENT;
            XPathExpression<Element> stratPath = xpf.compile(stratExpr, new ElementFilter(AssertionGroupingXml.STRATEGY_ELEMENT));
            List<Element> strategies = stratPath.evaluate(doc);
            if (strategies.isEmpty()) {
                LOGGER.warn("No {} element in {}", AssertionGroupingXml.STRATEGY_ELEMENT,
                        AssertionGroupingXml.GROUPS_RESOURCE_FILENAME);
                return;
            }
            Element strategy = strategies.get(0);
            List<Element> groups = new ArrayList<>();
            for (Element g : strategy.getChildren(AssertionGroupingXml.GROUP_ELEMENT)) {
                String gn = g.getAttributeValue(AssertionGroupingXml.ATTR_NAME);
                if (gn != null && !gn.isBlank()) {
                    groups.add(g);
                }
            }
            manifestGroupElements = groups;
        } catch (JDOMException | IOException e) {
            LOGGER.warn("Failed to parse groups from {}: {}", AssertionGroupingXml.GROUPS_RESOURCE_FILENAME, e.getMessage());
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
        List<String> fromManifest = manifestPolicyUuids.get(policyKey);
        if (fromManifest == null || fromManifest.isEmpty()) {
            return List.of();
        }
        return fromManifest;
    }

    /** Pipe-separated phrases declared for a policy name, or empty if absent. */
    private List<String> policyTextPhrasesFromManifest(String policyKey) {
        List<String> fromManifest = manifestPolicyTextPhrases.get(policyKey);
        if (fromManifest == null || fromManifest.isEmpty()) {
            return List.of();
        }
        return fromManifest;
    }

    private static List<String> parsePipeSeparatedPhrases(String pipeSeparated) {
        return Arrays.stream(pipeSeparated.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Each entry is a [category, centre] pair from a pipe-separated {@code category,centre} list. */
    private static List<List<String>> parseCategoryWithCentrePairs(String pipeSeparated) {
        List<List<String>> pairs = new ArrayList<>();
        for (String raw : pipeSeparated.split("\\|")) {
            List<String> pair = splitCsv(raw.trim());
            if (pair.size() == 2) {
                pairs.add(pair);
            }
        }
        return pairs;
    }

    private List<List<String>> policyCategoryCentrePairsFromManifest(String policyKey) {
        List<List<String>> fromManifest = manifestPolicyCategoryCentrePairs.get(policyKey);
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

    private boolean matchesCategoryCentrePolicy(Assertion assertion, String policyName) {
        List<List<String>> pairs = policyCategoryCentrePairsFromManifest(policyName);
        if (pairs.isEmpty()) {
            return false;
        }
        List<String> categoryTokens = splitCsv(assertion.getKeywords());
        for (List<String> categoryWithCentres : pairs) {
            if (defaultStandaloneCategories.contains(categoryWithCentres.get(0))
                    && !defaultStandaloneCategories.contains(categoryWithCentres.get(1))
                    && new HashSet<>(categoryTokens).containsAll(categoryWithCentres)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if the assertion should be excluded: a named {@code excludeByPolicy} matches by UUID or
     * assertion text phrase, or assertion keywords contain any {@code excludeCategories} token.
     */
    private boolean violatesExcludeByPolicy(Assertion assertion, String excludeByPolicyCsv, String excludeCategories) {
        if (excludedByNamedPolicies(assertion, excludeByPolicyCsv)) {
            return true;
        }
        return assertionKeywordsMatchExcludedCategories(assertion, excludeCategories);
    }

    private boolean excludedByNamedPolicies(Assertion assertion, String excludeByPolicyCsv) {
        if (!StringUtils.hasLength(excludeByPolicyCsv)) {
            return false;
        }
        String uuid = assertion.getUuid().toString();
        String assertionText = assertion.getAssertionText();
        for (String raw : splitCsv(excludeByPolicyCsv)) {
            String policyName = raw.trim();
            if (uuidInPolicyList(policyName, uuid)) {
                return true;
            }
            if (assertionTextMatchesPolicyPhrases(assertionText, policyName)) {
                return true;
            }
        }
        return false;
    }

    private boolean assertionTextMatchesPolicyPhrases(String text, String policyName) {
        List<String> phrases = policyTextPhrasesFromManifest(policyName);
        if (phrases.isEmpty()) {
            return false;
        }
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

    /** True if any named {@code includeByPolicy} matches by UUID or category-with-centre pairs. */
    private boolean matchesIncludeByPolicy(Assertion assertion, String includeByPolicyCsv) {
        String uuid = assertion.getUuid().toString();
        for (String raw : splitCsv(includeByPolicyCsv)) {
            String policyName = raw.trim();
            if (uuidInPolicyList(policyName, uuid)) {
                return true;
            }
            if (matchesCategoryCentrePolicy(assertion, policyName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code assertion} belongs in the group described by {@code group}'s XML attributes:
     * exclusions first, then {@code includeByPolicy} and {@code includeStandaloneCategories}.
     */
    private boolean assertionMatchesGroupRule(Assertion assertion, Element group) {
        if (violatesExcludeByPolicy(assertion, group.getAttributeValue(AssertionGroupingXml.ATTR_EXCLUDE_BY_POLICY), group.getAttributeValue(AssertionGroupingXml.ATTR_EXCLUDE_CATEGORIES))) {
            return false;
        }

        String includeByPolicyCsv = group.getAttributeValue(AssertionGroupingXml.ATTR_INCLUDE_BY_POLICY);
        if (StringUtils.hasLength(includeByPolicyCsv) && matchesIncludeByPolicy(assertion, includeByPolicyCsv)) {
            return true;
        }

        return matchesIncludeStandaloneCategories(assertion, group);
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
            throw new IllegalStateException(AssertionGroupingXml.GROUPS_RESOURCE_FILENAME
                    + " must define <assertionGroupingStrategy> with one or more <group> elements.");
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
