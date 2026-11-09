package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules of {@link DuckStorePacks#merge}, one per test, on tiny hand-written stores so
 * each test pins one rule.
 */
class DuckStorePacksTest {

	private static final String UUID_A = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_B = "22222222-2222-2222-2222-222222222222";
	private static final String UUID_C = "33333333-3333-3333-3333-333333333333";

	private static final String MACRO_A = "CREATE OR REPLACE MACRO substring_index(s, d, n) AS s";
	private static final String MACRO_A_DIFFERENT = "CREATE OR REPLACE MACRO substring_index(s, d, n) AS d";
	private static final String MACRO_B = "CREATE OR REPLACE MACRO extension_macro(c) AS c";

	private static final String KEYWORDS = "component-centric-validation";

	/** A pack with one assertion, one port and one prerequisite. */
	private static PackFixtures.Builder pack(String uuid, String file, String statement, String port,
			String prerequisiteHash) {
		PackFixtures.Builder builder = PackFixtures.store()
				.table("concept_s", "id BIGINT, active VARCHAR")
				.prerequisite("pre-requisites.sql", prerequisiteHash,
						"CREATE OR REPLACE TABLE rvfph_prospective_.concept_active AS SELECT * FROM concept_s")
				.assertion(uuid, file, "aaaa", KEYWORDS, statement);
		return port == null ? builder : builder.port(port);
	}

	private static DuckStorePacks.Pack packOf(String name, String version, PackFixtures.Builder store)
			throws IOException {
		return new DuckStorePacks.Pack(name, version, "sha256:" + name.hashCode(), store.parse());
	}

	private static String groupsXml(String groupName) {
		return """
				<assertionGroupingStrategy>
				 <group name="%s" includeStandaloneCategories="component-centric-validation"/>
				</assertionGroupingStrategy>
				""".formatted(groupName);
	}

	@Test
	void twoPacksCombineIntoOneStore() throws Exception {
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "2026.09.01",
						pack(UUID_A, "a.sql", "INSERT INTO qa_result SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.02",
						pack(UUID_B, "b.sql", "INSERT INTO qa_result SELECT 2", MACRO_B, "h1"))));

		assertEquals(2, merged.assertions().size());
		assertEquals(2, merged.ports().size(), "both macros, neither dropped");
		assertEquals(1, merged.prerequisiteStatements().size(), "the same prerequisite file appears once");
		assertEquals("b.sql", merged.assertions().get(UUID_B).file());
	}

	@Test
	void theMergedStoreIsAWorkingStore() throws Exception {
		// The engine must accept the result, not just the JSON parser.
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1"))));

		DuckStore reread = DuckStore.parse(merged.toJson());
		assertEquals(2, reread.assertions().size(), "a merged store must survive a round trip");
		assertEquals("qa_result", reread.qaResultToken());
		assertEquals(List.of("concept_s"), reread.knownTables());
		assertEquals("id BIGINT, active VARCHAR", reread.tableColumns().get("concept_s"));
		assertEquals(2, DuckAssertionSource.from(reread).findAll().size());
	}

	@Test
	void aRepeatedAssertionIsFineWhenItIsIdentical() throws Exception {
		// A pack built from a combined corpus repeats the base's assertions.
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("combined", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1"))));
		assertEquals(1, merged.assertions().size());
	}

	@Test
	void twoDifferentAssertionsUnderOneUuidAreRefused() {
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", pack(UUID_A, "different.sql", "SELECT 99", MACRO_A, "h1")))));
		assertEquals(1, conflict.getConflicts().size());
		assertTrue(conflict.getMessage().contains(UUID_A), conflict.getMessage());
		assertTrue(conflict.getMessage().contains("different.sql"), conflict.getMessage());
	}

	@Test
	void redefiningAnotherPacksMacroIsRefused() {
		// Appended, the second definition would win and silently change the first pack's SQL.
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_A_DIFFERENT, "h1")))));
		assertTrue(conflict.getMessage().contains("substring_index"), conflict.getMessage());
		assertTrue(conflict.getMessage().contains("redefines"), conflict.getMessage());
	}

	@Test
	void anIdenticalMacroInBothPacksIsNotAConflict() throws Exception {
		// Every pack from one publisher carries the same prelude.
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_A, "h1"))));
		assertEquals(1, merged.ports().size(), "one definition, not two");
	}

	@Test
	void aDifferentTableShapeIsRefused() {
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", PackFixtures.store()
								.table("concept_s", "id VARCHAR, active VARCHAR")
								.prerequisite("pre-requisites.sql", "h1", "SELECT 1")
								.assertion(UUID_B, "b.sql", KEYWORDS, "SELECT 2")))));
		assertTrue(conflict.getMessage().contains("concept_s"), conflict.getMessage());
	}

	@Test
	void aPackWhoseTableShapeCarriesSqlIsRefusedBeforeItCanMerge() {
		// The shapes become DDL and read_csv calls; this one would write a file if run.
		PackFixtures.Builder rogue = PackFixtures.store()
				.table("concept_s", "id BIGINT); COPY (SELECT 1) TO '/tmp/rvf-pack-escape.csv'; CREATE TABLE z (a INT")
				.prerequisite("pre-requisites.sql", "h1", "SELECT 1")
				.assertion(UUID_B, "b.sql", KEYWORDS, "SELECT 2");
		IOException refused = assertThrows(IOException.class, () -> packOf("rogue", "1", rogue));
		assertTrue(refused.getMessage().contains("tableColumns declares concept_s"), refused.getMessage());
	}

	@Test
	void theSamePrerequisiteFileWithADifferentHashIsRefused() {
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h2")))));
		assertTrue(conflict.getMessage().contains("pre-requisites.sql"), conflict.getMessage());
	}

	@Test
	void aConflictNamesThePackThatOwnsTheEarlierDefinitionNotTheBase() {
		// The second pack introduces the macro, the table shape, the prerequisite and the
		// assertion; the third disagrees with each. The base defines none of them.
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", PackFixtures.store()
								.assertion(UUID_C, "c.sql", KEYWORDS, "SELECT 3")),
						packOf("first-pack", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("second-pack", "1", PackFixtures.store()
								.table("concept_s", "id VARCHAR, active VARCHAR")
								.prerequisite("pre-requisites.sql", "h2", "SELECT 1")
								.port(MACRO_A_DIFFERENT)
								.assertion(UUID_A, "other.sql", KEYWORDS, "SELECT 99")))));
		assertEquals(4, conflict.getConflicts().size(), conflict.getMessage());
		for (String line : conflict.getConflicts()) {
			assertTrue(line.contains("first-pack@1") && line.contains("second-pack@1"), line);
			assertFalse(line.contains("international"), line);
		}
	}

	@Test
	void twoPacksWithOneNameAreRefused() {
		// A requirement or a report naming the pack could not tell which was meant.
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("extension-pack", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1")),
						packOf("extension-pack", "2", pack(UUID_C, "c.sql", "SELECT 3", MACRO_B, "h1")))));
		assertEquals(1, conflict.getConflicts().size(), conflict.getMessage());
		assertTrue(conflict.getMessage().contains("named extension-pack (extension-pack@1 and extension-pack@2)"),
				conflict.getMessage());
	}

	@Test
	void aDifferentSentinelIsRefused() throws Exception {
		String other = pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1").json()
				.replace("rvfph_prospective_", "rvfph_other_");
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						new DuckStorePacks.Pack("rogue", "1", "sha256:r", DuckStore.parse(other)))));
		assertTrue(conflict.getMessage().contains("sentinels"), conflict.getMessage());
	}

	@Test
	void aDifferentTranspilerVersionIsRefused() {
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1").transpiler("31.0.0")))));
		assertTrue(conflict.getMessage().contains("transpiler"), conflict.getMessage());
	}

	@Test
	void everyConflictIsReportedNotJustTheFirst() {
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("rogue", "1", pack(UUID_A, "different.sql", "SELECT 99", MACRO_A_DIFFERENT, "h2")
								.transpiler("31.0.0")))));
		assertTrue(conflict.getConflicts().size() >= 4,
				"expected transpiler, prerequisite, port and assertion conflicts, got " + conflict.getConflicts());
	}

	@Test
	void eachPacksAssertionsAreGroupedByThatPacksOwnRules() throws Exception {
		// The base's groups.xml must not decide membership of the extension's assertions.
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
						.grouping(groupsXml("base-group"), PackFixtures.POLICIES_XML)),
				packOf("extension-pack", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1")
						.grouping(groupsXml("extension-group"), PackFixtures.POLICIES_XML))));

		assertEquals(List.of("base-group"), merged.assertions().get(UUID_A).groups());
		assertEquals(List.of("extension-group"), merged.assertions().get(UUID_B).groups());
		Map<String, Set<String>> resolved = DuckAssertionSource.resolveGroups(merged);
		assertEquals(Set.of("base-group"), resolved.get(UUID_A));
		assertEquals(Set.of("extension-group"), resolved.get(UUID_B));
	}

	@Test
	void aRepeatedAssertionBelongsToTheUnionOfItsGroups() throws Exception {
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
						.grouping(groupsXml("base-group"), PackFixtures.POLICIES_XML)),
				packOf("combined", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
						.grouping(groupsXml("extension-group"), PackFixtures.POLICIES_XML))));
		assertEquals(List.of("base-group", "extension-group"), merged.assertions().get(UUID_A).groups());
	}

	@Test
	void aPackWithoutGroupingFilesIsAConflict() {
		// No group would ever select its assertions.
		var conflict = assertThrows(DuckStorePacks.ConflictException.class, () ->
				DuckStorePacks.merge(List.of(
						packOf("international", "1", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
						packOf("ungrouped", "1", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1").noGrouping()))));
		assertTrue(conflict.getConflicts().stream().anyMatch(c -> c.startsWith("ungrouped@1")),
				conflict.getMessage());
	}

	@Test
	void theMergedStoreRecordsWhatWentIntoItAndDropsPerPackIdentity() throws Exception {
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "2026.09.01",
						pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1").identity("international", "2026.09.01", "abc123")),
				packOf("extension-pack", "2026.09.02",
						pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1").identity("extension-pack", "2026.09.02", "def456"))));

		List<DuckStore.PackRecord> packs = merged.packs();
		assertEquals(2, packs.size());
		assertEquals("international", packs.get(0).name());
		assertEquals("abc123", packs.get(0).corpusRef());
		assertEquals("2026.09.02", packs.get(1).version());
		assertEquals("def456", packs.get(1).corpusRef());
		assertEquals(1, packs.get(1).assertions());
		assertTrue(packs.get(1).digest().startsWith("sha256:"), packs.get(1).digest());

		// One pack's rules must not be re-applied to another's assertions, and the merge has no single identity.
		assertTrue(merged.grouping().isEmpty());
		assertFalse(merged.tree().has("grouping"));
		assertFalse(merged.tree().has("pack"));
		assertTrue(merged.identity().isEmpty());
		assertEquals(2, DuckStore.parse(merged.toJson()).packs().size());
	}

	@Test
	void anUnmergedStoreHasNoPacks() throws Exception {
		DuckStore store = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1").parse();
		assertTrue(store.packs().isEmpty());
		assertNull(store.assertions().get(UUID_A).groups(), "membership comes from the store's own grouping");
	}

	@Test
	void aChangedPrerequisiteBreaksTheDigest() throws Exception {
		// pre-requisites.sql builds the tables every assertion reads.
		String tampered = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
				.identity("international", "2026.07.27", "abc123").json()
				.replace("\"sha256\":\"h1\"", "\"sha256\":\"h2\"");
		IOException e = assertThrows(IOException.class, () -> DuckStore.parse(tampered).identity());
		assertTrue(e.getMessage().contains("declares digest"), e.getMessage());
	}

	@Test
	void changedGroupingBreaksTheDigest() throws Exception {
		// The grouping files decide what a run selects, so they are part of the identity.
		PackFixtures.Builder builder = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
				.identity("international", "2026.07.27", "abc123");
		String original = builder.json();
		String tampered = original.replace(PackFixtures.sha256Hex(PackFixtures.POLICIES_XML), "0".repeat(64));
		assertTrue(DuckStore.parse(original).identity().isPresent());
		assertThrows(IOException.class, () -> DuckStore.parse(tampered).identity());
	}

	private static PackFixtures.Builder requiring(String uuid, String file, String requires) {
		return pack(uuid, file, "SELECT 1", MACRO_B, "h1").requires(requires);
	}

	@Test
	void aSatisfiedRequirementMergesSilently() throws Exception {
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"atLeast\": \"2026.07.01\"}"))));
		assertEquals(2, merged.assertions().size());
	}

	@Test
	void anOlderBaseIsRefusedNamingBothVersions() {
		// A pack calling a macro its base gained later would otherwise fail at run time.
		var e = assertThrows(DuckStorePacks.ConflictException.class, () -> DuckStorePacks.merge(List.of(
				packOf("international", "2026.06.01", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"atLeast\": \"2026.07.27\"}")))));
		String message = String.join("\n", e.getConflicts());
		assertTrue(message.contains("extension-pack@2026.09.01"), message);
		assertTrue(message.contains("international at least 2026.07.27"), message);
		assertTrue(message.contains("international@2026.06.01"), message);
	}

	@Test
	void anEqualVersionSatisfiesAtLeast() throws Exception {
		assertEquals(2, DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"atLeast\": \"2026.07.27\"}"))))
				.assertions().size());
	}

	@Test
	void aVersionThatIsNotADateIsRefusedRatherThanSorted() {
		// Lexicographic order would put 2026.9.1 above 2026.10.1.
		var e = assertThrows(DuckStorePacks.ConflictException.class, () -> DuckStorePacks.merge(List.of(
				packOf("international", "v2", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"atLeast\": \"2026.07.27\"}")))));
		assertTrue(String.join("", e.getConflicts()).contains("cannot be checked"), e.getMessage());
	}

	@Test
	void aRequirementOnAPackNotInTheMergeIsRefused() {
		var e = assertThrows(DuckStorePacks.ConflictException.class, () -> DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"other-pack\", \"atLeast\": \"2026.01.01\"}")))));
		String message = String.join("", e.getConflicts());
		assertTrue(message.contains("no pack called other-pack"), message);
		assertTrue(message.contains("international"), "names what is here: " + message);
	}

	@Test
	void anExactDigestRequirementIsCheckedToo() {
		var e = assertThrows(DuckStorePacks.ConflictException.class, () -> DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"digest\": \"sha256:nope\"}")))));
		String message = String.join("", e.getConflicts());
		assertTrue(message.contains("international at digest sha256:nope"), message);
		assertTrue(message.contains("sha256:" + "international".hashCode()), "names the actual digest: " + message);
	}

	@Test
	void aRequirementThisRuntimeCannotUnderstandIsRefusedNotIgnored() {
		// An ignored requirement would read as a checked combination.
		IOException e = assertThrows(IOException.class, () -> requiring(UUID_B, "b.sql",
				"{\"pack\": \"international\", \"atMost\": \"2026.09.01\"}").parse().requirements());
		assertTrue(e.getMessage().contains("atMost"), e.getMessage());
		assertTrue(e.getMessage().contains("does not understand"), e.getMessage());
	}

	@Test
	void aRequirementMustSayExactlyOneOfAtLeastOrDigest() {
		assertTrue(assertThrows(IOException.class, () -> requiring(UUID_B, "b.sql",
				"{\"pack\": \"international\"}").parse().requirements())
				.getMessage().contains("neither"));
		assertTrue(assertThrows(IOException.class, () -> requiring(UUID_B, "b.sql",
				"{\"pack\": \"international\", \"atLeast\": \"2026.01.01\", \"digest\": \"sha256:x\"}")
				.parse().requirements())
				.getMessage().contains("both"));
	}

	@Test
	void aMergedStoreKeepsEveryInputsRequirementsAndCanSatisfyThem() throws Exception {
		// A merged store can be the base of a later merge: requirements survive, and are
		// satisfied by the packs nested in it.
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")),
				packOf("extension-pack", "2026.09.01", requiring(UUID_B, "b.sql",
						"{\"pack\": \"international\", \"atLeast\": \"2026.07.01\"}"))));

		assertEquals(1, merged.requirements().size(), "the pack's requirement survived");
		assertEquals("international", merged.requirements().get(0).pack());

		DuckStore again = DuckStorePacks.merge(List.of(
				new DuckStorePacks.Pack("combined", "2026.09.02", "sha256:combined", merged),
				packOf("second-extension", "2026.09.03", pack(UUID_C, "c.sql", "SELECT 3", null, "h1"))));
		assertEquals(3, again.assertions().size());
	}

	@Test
	void anUnmergedStoreStatesItsOwnIdentity() throws Exception {
		PackFixtures.Builder builder = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
				.identity("international", "2026.07.27", "abc123");
		List<DuckStore.PackRecord> provenance = builder.parse().provenance();
		assertEquals(1, provenance.size());
		assertEquals("international", provenance.get(0).name());
		assertEquals("2026.07.27", provenance.get(0).version());
		assertEquals("abc123", provenance.get(0).corpusRef());
		assertEquals(builder.digest(), provenance.get(0).digest());
		assertEquals(1, provenance.get(0).assertions());
	}

	@Test
	void anEditedStoreIsRefusedRatherThanBelieved() {
		String tampered = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
				.identity("international", "2026.07.27", "abc123").json()
				.replace("\"sha256\":\"aaaa\"", "\"sha256\":\"bbbb\"");
		IOException e = assertThrows(IOException.class, () -> DuckStore.parse(tampered).identity());
		assertTrue(e.getMessage().contains("declares digest"), e.getMessage());
		assertTrue(e.getMessage().contains("international@2026.07.27"), e.getMessage());
	}

	@Test
	void aStoreWithoutIdentityIsReadableAndSaysNothing() throws Exception {
		DuckStore store = pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1").parse();
		assertTrue(store.identity().isEmpty());
		assertTrue(store.provenance().isEmpty());
	}

	@Test
	void aMergedStoreReportsItsPacksRatherThanTheBaseIdentity() throws Exception {
		DuckStore merged = DuckStorePacks.merge(List.of(
				packOf("international", "2026.07.27", pack(UUID_A, "a.sql", "SELECT 1", MACRO_A, "h1")
						.identity("international", "2026.07.27", "abc123")),
				packOf("extension-pack", "2026.09.01", pack(UUID_B, "b.sql", "SELECT 2", MACRO_B, "h1"))));
		assertEquals(List.of("international", "extension-pack"),
				merged.provenance().stream().map(DuckStore.PackRecord::name).toList());
	}

	@Test
	void mergingNothingIsRefused() {
		// An empty store reports every release as clean.
		assertThrows(IllegalArgumentException.class, () -> DuckStorePacks.merge(List.of()));
	}
}
