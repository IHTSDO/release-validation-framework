package org.ihtsdo.rvf.core.service.duck;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merges assertion packs into the one store an engine executes.
 *
 * <p>A pack is a store published by {@code tools/duck-publisher} from another assertion
 * corpus: statements already transpiled, with their source hashes, its corpus's group rules
 * and its own identity. See {@code docs/assertion-packs.md}.
 *
 * <h2>Merge rules</h2>
 * <ul>
 * <li><b>Pack names are unique.</b> A requirement and a report name a pack by name alone.
 * <li><b>{@code formatVersion}, {@code runIdSentinel}, {@code qaResultToken},
 *     {@code sentinels} and the transpiler version must be identical.</b> They are the
 *     contract with {@link DuckBinder}; a pack with other sentinels would reach execution
 *     with placeholders unbound, and two sqlglot versions are two dialects.
 * <li><b>{@code knownTables} union; {@code tableColumns} union, and an overlap must
 *     agree.</b> Both derive from RVF's DDL; a disagreement means one pack's SQL reads a
 *     table shape the other does not have.
 * <li><b>Assertions are additive, and a uuid may repeat only for an identical entry.</b>
 *     A pack built from a combined corpus legitimately repeats the base's assertions; two
 *     different assertions under one uuid would have one's findings reported as the other's.
 * <li><b>Group membership is resolved per pack</b>, with that pack's own grouping files,
 *     and recorded on each assertion of the merged store. A repeated assertion belongs to
 *     the union of its groups. A pack without grouping files is refused: no group would
 *     select its assertions.
 * <li><b>{@code ports} union by macro name, and a redefinition is refused</b>, so one pack
 *     cannot change the meaning of another pack's SQL.
 * <li><b>{@code prerequisites} union by file, and a file with a different hash is refused</b>,
 *     for the same reason.
 * <li><b>Every pack's {@code requires} must hold</b> against the packs in the merge.
 * </ul>
 *
 * <p>Every refusal names the packs and the key, and all of them are reported at once.
 */
public final class DuckStorePacks {

	/**
	 * A pack and its identity, which the merged store records so a report can say which
	 * assertions produced it.
	 */
	public record Pack(String name, String version, String digest, DuckStore store) {

		/** How this pack reads in a message. */
		public String label() {
			return name + (version == null || version.isBlank() ? "" : "@" + version);
		}
	}

	/** The merge was refused. Carries every conflict, not just the first. */
	public static class ConflictException extends RuntimeException {

		private final List<String> conflicts;

		ConflictException(List<String> conflicts) {
			super("assertion packs cannot be merged:\n  " + String.join("\n  ", conflicts));
			this.conflicts = List.copyOf(conflicts);
		}

		public List<String> getConflicts() {
			return conflicts;
		}
	}

	/**
	 * The name a port defines: {@code CREATE OR REPLACE MACRO foo(...)}, {@code ... TABLE x ...}.
	 * Keyed on the name, so the same macro with two bodies is a conflict rather than two ports.
	 */
	private static final Pattern PORT_NAME = Pattern.compile(
			"CREATE\\s+(?:OR\\s+REPLACE\\s+)?(MACRO|TABLE|FUNCTION|PROCEDURE)\\s+([\\w.\"`]+)",
			Pattern.CASE_INSENSITIVE);

	/** {@code YYYY.MM.DD}, the only version shape {@code atLeast} can order. */
	private static final Pattern DATE_VERSION = Pattern.compile("(\\d{4})\\.(\\d{2})\\.(\\d{2})");

	private static final String GROUPS = "groups";

	private DuckStorePacks() {
	}

	/**
	 * Merges packs in order, the first being the base.
	 *
	 * @throws ConflictException if any rule above is broken
	 */
	public static DuckStore merge(List<Pack> packs) throws IOException {
		if (packs.isEmpty()) {
			throw new IllegalArgumentException("no packs to merge - an empty store reports every release as clean");
		}
		Pack base = packs.get(0);
		ObjectNode out = (ObjectNode) base.store().tree().deepCopy();
		List<String> conflicts = new ArrayList<>();

		// A requirement, a report and a refresh all name a pack by its name alone.
		Map<String, String> byName = new LinkedHashMap<>();
		for (Pack pack : packs) {
			String earlier = byName.putIfAbsent(pack.name(), pack.label());
			if (earlier != null) {
				conflicts.add("two packs in this merge are named " + pack.name() + " (" + earlier + " and "
						+ pack.label() + ") - a requirement or a report naming it could not tell them apart");
			}
		}

		ArrayNode provenance = out.putArray("packs");
		for (Pack pack : packs) {
			ObjectNode entry = provenance.addObject();
			entry.put("name", pack.name());
			entry.put("version", pack.version());
			entry.put("digest", pack.digest());
			entry.put("corpusRef", pack.store().corpusRef());
			entry.put("assertions", pack.store().assertions().size());
		}

		// Each definition with the label of the pack that made it, so a conflict names its owner.
		Map<String, String> ports = portsByName(base, conflicts);
		Map<String, String> portOwners = owners(ports.keySet(), base);
		Map<String, JsonNode> prerequisites = prerequisitesByFile(base);
		Map<String, String> prerequisiteOwners = owners(prerequisites.keySet(), base);
		Set<String> knownTables = new LinkedHashSet<>(base.store().knownTables());
		Map<String, String> tableColumns = new LinkedHashMap<>(base.store().tableColumns());
		Map<String, String> tableOwners = owners(tableColumns.keySet(), base);
		ObjectNode assertions = out.putObject("assertions");
		Map<String, String> assertionOwners = new LinkedHashMap<>();
		addAssertions(assertions, assertionOwners, base, conflicts);

		for (Pack pack : packs.subList(1, packs.size())) {
			DuckStore store = pack.store();
			mustMatch(conflicts, base, pack, "formatVersion", base.store().formatVersion(), store.formatVersion());
			mustMatch(conflicts, base, pack, "runIdSentinel", base.store().runIdSentinel(), store.runIdSentinel());
			mustMatch(conflicts, base, pack, "qaResultToken", base.store().qaResultToken(), store.qaResultToken());
			mustMatch(conflicts, base, pack, "sentinels", base.store().sentinels(), store.sentinels());
			mustMatch(conflicts, base, pack, "transpiler version",
					base.store().transpilerVersion(), store.transpilerVersion());

			knownTables.addAll(store.knownTables());
			store.tableColumns().forEach((table, columns) -> {
				String existing = tableColumns.putIfAbsent(table, columns);
				tableOwners.putIfAbsent(table, pack.label());
				if (existing != null && !existing.equals(columns)) {
					conflicts.add("table " + table + " is declared differently by " + tableOwners.get(table)
							+ " and " + pack.label() + " - one pack's SQL reads a shape the other does not have");
				}
			});

			portsByName(pack, conflicts).forEach((name, statement) -> {
				String existing = ports.putIfAbsent(name, statement);
				portOwners.putIfAbsent(name, pack.label());
				if (existing != null && !existing.equals(statement)) {
					conflicts.add(pack.label() + " redefines " + name + ", already defined by " + portOwners.get(name)
							+ " - a redefinition changes the meaning of the other pack's SQL");
				}
			});

			prerequisitesByFile(pack).forEach((file, node) -> {
				JsonNode existing = prerequisites.putIfAbsent(file, node);
				prerequisiteOwners.putIfAbsent(file, pack.label());
				if (existing != null && !hashOf(existing).equals(hashOf(node))) {
					conflicts.add("prerequisite " + file + " differs between " + prerequisiteOwners.get(file)
							+ " and " + pack.label());
				}
			});

			addAssertions(assertions, assertionOwners, pack, conflicts);
		}

		checkRequirements(packs, conflicts);

		if (!conflicts.isEmpty()) {
			throw new ConflictException(conflicts);
		}

		ArrayNode tables = out.putArray("knownTables");
		knownTables.forEach(tables::add);
		ObjectNode columns = out.putObject("tableColumns");
		tableColumns.forEach(columns::put);
		ArrayNode portArray = out.putArray("ports");
		ports.values().forEach(portArray::add);
		ArrayNode prereqArray = out.putArray("prerequisites");
		prerequisites.values().forEach(prereqArray::add);
		// Membership is now on each assertion; one pack's rules must not be re-applied to
		// another pack's assertions. Nor does the merge have one pack identity of its own:
		// its provenance is "packs".
		out.remove("grouping");
		out.remove("pack");

		// Every input's requirements, so a later merge against this store re-checks them.
		ArrayNode requires = out.putArray("requires");
		Set<String> seen = new LinkedHashSet<>();
		for (Pack pack : packs) {
			for (JsonNode need : pack.store().tree().path("requires")) {
				if (seen.add(need.toString())) {
					requires.add(need);
				}
			}
		}
		if (requires.isEmpty()) {
			out.remove("requires");
		}

		return DuckStore.parse(out.toString());
	}

	/** {@code key -> label} for every key, all owned by {@code pack}. */
	private static Map<String, String> owners(Set<String> keys, Pack pack) {
		Map<String, String> owners = new LinkedHashMap<>();
		keys.forEach(key -> owners.put(key, pack.label()));
		return owners;
	}

	/**
	 * Adds a pack's assertions to the merged set, each carrying the groups its pack resolves,
	 * and records which pack first added each one in {@code owners}.
	 */
	private static void addAssertions(ObjectNode merged, Map<String, String> owners, Pack pack,
			List<String> conflicts) {
		Map<String, Set<String>> groups;
		try {
			groups = DuckAssertionSource.resolveGroups(pack.store());
		} catch (IOException e) {
			conflicts.add(pack.label() + ": " + e.getMessage());
			return;
		}
		JsonNode incoming = pack.store().tree().path("assertions");
		for (String uuid : incoming.propertyNames()) {
			ObjectNode entry = (ObjectNode) incoming.path(uuid).deepCopy();
			entry.remove(GROUPS);
			JsonNode existing = merged.get(uuid);
			if (existing == null) {
				ArrayNode array = entry.putArray(GROUPS);
				groups.getOrDefault(uuid, Set.of()).forEach(array::add);
				merged.set(uuid, entry);
				owners.put(uuid, pack.label());
				continue;
			}
			ObjectNode existingEntry = (ObjectNode) existing;
			ObjectNode comparable = existingEntry.deepCopy();
			comparable.remove(GROUPS);
			if (!comparable.equals(entry)) {
				conflicts.add("assertion " + uuid + " is different in " + owners.get(uuid) + " ("
						+ existing.path("file").asString("") + ") and " + pack.label() + " ("
						+ entry.path("file").asString("") + ")");
				continue;
			}
			Set<String> union = new LinkedHashSet<>();
			existingEntry.path(GROUPS).forEach(g -> union.add(g.asString("")));
			union.addAll(groups.getOrDefault(uuid, Set.of()));
			ArrayNode array = existingEntry.putArray(GROUPS);
			union.forEach(array::add);
		}
	}

	/**
	 * Every pack's requirements against what the merge holds, including packs nested in a
	 * merged store used as an input.
	 */
	private static void checkRequirements(List<Pack> packs, List<String> conflicts) throws IOException {
		Map<String, DuckStore.PackRecord> present = new LinkedHashMap<>();
		for (Pack pack : packs) {
			present.put(pack.name(), new DuckStore.PackRecord(pack.name(), pack.version(), pack.digest(),
					pack.store().corpusRef(), pack.store().assertions().size()));
			for (DuckStore.PackRecord nested : pack.store().packs()) {
				present.putIfAbsent(nested.name(), nested);
			}
		}
		for (Pack pack : packs) {
			for (DuckStore.Requirement need : pack.store().requirements()) {
				DuckStore.PackRecord target = present.get(need.pack());
				if (target == null) {
					conflicts.add(pack.label() + " requires " + need.describe() + ", but this merge has no pack called "
							+ need.pack() + " - it holds " + present.keySet());
					continue;
				}
				if (need.kind() == DuckStore.Requirement.Kind.DIGEST) {
					if (!need.value().equals(target.digest())) {
						conflicts.add(pack.label() + " requires " + need.describe() + ", but " + label(target)
								+ " is " + target.digest());
					}
					continue;
				}
				Integer ordered = compareDateVersions(target.version(), need.value());
				if (ordered == null) {
					// Lexicographic order would put 2026.9.1 above 2026.10.1, so a version that
					// is not YYYY.MM.DD is refused rather than compared.
					conflicts.add(pack.label() + " requires " + need.describe() + " and " + label(target)
							+ " is " + target.version() + ", but versions are compared as YYYY.MM.DD "
							+ "dates and one of these is not one, so the requirement cannot be checked");
				} else if (ordered < 0) {
					conflicts.add(pack.label() + " requires " + need.describe() + ", but this merge has "
							+ label(target) + " - the older corpus may not define what this pack calls");
				}
			}
		}
	}

	private static String label(DuckStore.PackRecord pack) {
		return pack.name() + (pack.version() == null || pack.version().isBlank() ? "" : "@" + pack.version());
	}

	/** {@code left <=> right} as dates, or null when either is not one. */
	private static Integer compareDateVersions(String left, String right) {
		Matcher l = DATE_VERSION.matcher(left == null ? "" : left);
		Matcher r = DATE_VERSION.matcher(right == null ? "" : right);
		if (!l.matches() || !r.matches()) {
			return null;
		}
		for (int group = 1; group <= 3; group++) {
			int diff = Integer.parseInt(l.group(group)) - Integer.parseInt(r.group(group));
			if (diff != 0) {
				return diff < 0 ? -1 : 1;
			}
		}
		return 0;
	}

	private static Map<String, String> portsByName(Pack pack, List<String> conflicts) {
		Map<String, String> byName = new LinkedHashMap<>();
		for (String statement : pack.store().ports()) {
			Matcher m = PORT_NAME.matcher(statement);
			// An unrecognised port is keyed on its own text: carried, not dropped or guessed at.
			String key = m.find() ? m.group(2).toLowerCase(Locale.ROOT) : "verbatim:" + statement;
			String previous = byName.put(key, statement);
			if (previous != null && !previous.equals(statement)) {
				conflicts.add(pack.label() + " defines " + key + " twice, differently");
			}
		}
		return byName;
	}

	private static Map<String, JsonNode> prerequisitesByFile(Pack pack) {
		Map<String, JsonNode> byFile = new LinkedHashMap<>();
		for (JsonNode node : pack.store().tree().path("prerequisites")) {
			byFile.put(node.path("file").asString(""), node);
		}
		return byFile;
	}

	private static String hashOf(JsonNode prerequisite) {
		return prerequisite.path("sha256").asString("");
	}

	private static void mustMatch(List<String> conflicts, Pack base, Pack pack, String what,
			Object expected, Object actual) {
		if (!Objects.equals(expected, actual)) {
			conflicts.add(what + " differs: " + base.label() + " has " + expected + ", " + pack.label() + " has " + actual);
		}
	}
}
