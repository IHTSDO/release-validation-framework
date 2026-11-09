package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.importer.AssertionGroupImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The assertions of a {@link DuckStore} and their group membership, with no database.
 *
 * <p>Answers the questions a validation run asks of {@code AssertionService}: which
 * assertions are in these groups, and which are the resource assertions.
 *
 * <p>Group membership comes from the store, never from the corpus checkout beside the
 * application: a store published from one corpus commit and grouped by another commit's
 * {@code groups.xml} would run a set of assertions neither commit describes. A store carries
 * its corpus's {@code groups.xml} and {@code policies.xml}, and
 * {@link AssertionGroupImporter#resolveGroups} applies the same rules the MySQL importer
 * does.
 *
 * <p>Keyed on assertion UUID. The numeric {@code assertionId} is assigned by the MySQL
 * import, which does not happen in this mode, so it is left null rather than synthesised.
 *
 * <p>Immutable: resolve once per store, share across runs.
 */
public final class DuckAssertionSource {

	static final String GROUPS_XML = "groups.xml";
	static final String POLICIES_XML = "policies.xml";

	private final List<Assertion> assertions;
	private final Map<String, Assertion> byUuid;
	private final Map<String, Set<String>> groupsByUuid;

	private DuckAssertionSource(List<Assertion> assertions, Map<String, Set<String>> groupsByUuid) {
		this.assertions = List.copyOf(assertions);
		this.groupsByUuid = Map.copyOf(groupsByUuid);
		Map<String, Assertion> index = new LinkedHashMap<>();
		assertions.forEach(a -> index.put(a.getUuid().toString(), a));
		this.byUuid = Map.copyOf(index);
	}

	/**
	 * Builds the source for a store.
	 *
	 * @throws IOException if some assertion's group membership cannot be resolved from the store
	 */
	public static DuckAssertionSource from(DuckStore store) throws IOException {
		List<Assertion> assertions = new ArrayList<>();
		for (DuckStore.StoredAssertion stored : store.assertions().values()) {
			assertions.add(toAssertion(stored));
		}
		Map<String, Set<String>> groups = resolveGroups(store, assertions);
		// The @Transient groups field is what the failures extractor reads to decide
		// whether a failure is whitelist-eligible, as on the MySQL path. A concurrent set:
		// these objects are shared between requests, and AssertionController adds groups
		// to them (an idempotent add, since every group is already present).
		assertions.forEach(a -> {
			Set<String> resolved = groups.getOrDefault(a.getUuid().toString(), Set.of());
			Set<String> live = ConcurrentHashMap.newKeySet(Math.max(1, resolved.size()));
			live.addAll(resolved);
			a.setGroups(live);
		});
		return new DuckAssertionSource(assertions, groups);
	}

	/**
	 * {@code uuid -> group names} for every assertion in the store, by the store's own
	 * grouping files.
	 *
	 * @throws IOException if the store has no grouping files
	 */
	private static Map<String, Set<String>> resolveGroups(DuckStore store, List<Assertion> assertions)
			throws IOException {
		Map<String, DuckStore.GroupingFile> grouping = store.grouping();
		DuckStore.GroupingFile groups = grouping.get(GROUPS_XML);
		DuckStore.GroupingFile policies = grouping.get(POLICIES_XML);
		if (groups == null || policies == null) {
			throw new IOException("The assertion store carries no " + GROUPS_XML + " and "
					+ POLICIES_XML + ", so no group would select its assertions. Republish the store "
					+ "with tools/duck-publisher.");
		}
		try (InputStream groupsXml = utf8(groups.text()); InputStream policiesXml = utf8(policies.text())) {
			return AssertionGroupImporter.resolveGroups(groupsXml, policiesXml, assertions);
		} catch (IllegalStateException e) {
			throw new IOException("The assertion store's group rules cannot be applied: " + e.getMessage(), e);
		}
	}

	private static InputStream utf8(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Every assertion in these groups, de-duplicated, in store order.
	 *
	 * <p>Store order, so two runs of the same store execute the same sequence. An unknown
	 * group name selects nothing, as {@code AssertionService.getAssertionGroupsByNames} does.
	 */
	public List<Assertion> getAssertionsInGroups(Collection<String> groupNames) {
		Set<String> wanted = new LinkedHashSet<>(groupNames);
		List<Assertion> out = new ArrayList<>();
		for (Assertion assertion : assertions) {
			Set<String> in = groupsByUuid.getOrDefault(assertion.getUuid().toString(), Set.of());
			if (in.stream().anyMatch(wanted::contains)) {
				out.add(assertion);
			}
		}
		return out;
	}

	/**
	 * Mirrors {@code AssertionService.getAssertionsByKeyWords}: {@code exactMatch} compares the
	 * whole keywords field, as the repository's derived query does, so {@code ("resource", true)}
	 * does not find an assertion keyworded {@code "resource,INT"}.
	 */
	public List<Assertion> getAssertionsByKeyWords(String keyWord, boolean exactMatch) {
		List<Assertion> out = new ArrayList<>();
		for (Assertion assertion : assertions) {
			String keywords = assertion.getKeywords();
			if (exactMatch ? keywords.equals(keyWord) : keywords.contains(keyWord)) {
				out.add(assertion);
			}
		}
		return out;
	}

	/**
	 * Every assertion, in store order, as a new mutable list, as the JPA implementation returns.
	 *
	 * <p>Callers append to the list they are given ({@code GET /assertions?includeDroolsRules=true}
	 * does); a copy keeps that from editing the corpus every later run executes. The
	 * assertion objects themselves are shared.
	 */
	public List<Assertion> findAll() {
		return new ArrayList<>(assertions);
	}

	/** The assertion with this uuid, or null, as {@code findAssertionByUUID} does. */
	public Assertion getAssertionByUuid(UUID uuid) {
		return byUuid.get(uuid.toString());
	}

	/** The group names this assertion belongs to; empty for one in no group. */
	public Set<String> groupsOf(UUID uuid) {
		return groupsByUuid.getOrDefault(uuid.toString(), Set.of());
	}

	/** Every group name that selects at least one assertion, sorted. */
	public Set<String> populatedGroupNames() {
		Set<String> names = new TreeSet<>();
		groupsByUuid.values().forEach(names::addAll);
		return names;
	}

	private static Assertion toAssertion(DuckStore.StoredAssertion stored) {
		Assertion assertion = new Assertion();
		assertion.setUuid(UUID.fromString(stored.uuid()));
		assertion.setAssertionText(stored.text());
		assertion.setKeywords(stored.keywords());
		assertion.setSeverity(stored.severity());
		return assertion;
	}
}
