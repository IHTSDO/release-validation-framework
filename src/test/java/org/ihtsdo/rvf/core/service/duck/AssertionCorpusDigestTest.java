package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every assertion finds on RVF's regression fixture, recorded per assertion in
 * {@code src/test/resources/duck/assertion-digests.tsv}.
 *
 * <p>Runs anywhere, with no MySQL: any change to the engine, binder, materialiser or a
 * transpilation has to show which assertions it moved. Per assertion, because two offsetting
 * differences leave a total unchanged.
 *
 * <p>The store is not the bundled one, which follows the corpus branch the build checks out:
 * a recording of it would fail on every corpus commit, and a check that is always
 * regenerated checks nothing. The build publishes this test's store with the same publisher
 * from a fixed corpus commit, {@code duck.digest.corpus.ref} in {@code pom.xml}, which must be
 * the commit the recording names. Move both together, with the regenerated recording.
 *
 * <p>The recorded value is a status and a digest over the sorted {@code concept_id},
 * {@code component_id}, {@code table_name} and {@code details} of every row the assertion
 * inserted, so "same answer" is distinguished from "same number of rows".
 *
 * <p>The fixture is {@code SnomedCT_RegressionTest_20130731} with {@code _20130131} as the
 * previous release, unchanged from what the MySQL regression tests use. No dependency release
 * is supplied, so the assertions that compare against one are recorded as NOT_RUN.
 *
 * <p>An assertion that fails to execute fails the build: an unexecuted check compared with
 * its own recording would otherwise read as a pass.
 *
 * <p>To regenerate the recording, run this class with {@code -Dduck.digests.write=true}
 * (e.g. {@code mvn test -Dtest=AssertionCorpusDigestTest -Dduck.digests.write=true}) and
 * commit the diff with the reason each moved assertion moved.
 */
class AssertionCorpusDigestTest {

	/** Sanity floor on the corpus size, so a truncated store fails loudly. */
	private static final int EXPECTED_ASSERTIONS = 300;

	/** Published by the build (pom.xml, duck-digest-store) from duck.digest.corpus.ref. */
	private static final Path STORE = Path.of("target/duck-digest/store.json");
	private static final Path PROSPECTIVE = Path.of("src/test/resources/SnomedCT_RegressionTest_20130731");
	private static final Path PREVIOUS = Path.of("src/test/resources/SnomedCT_RegressionTest_20130131");
	private static final Path GOLDEN = Path.of("src/test/resources/duck/assertion-digests.tsv");

	private static final long RUN_ID = 1L;
	private static final String QA_RESULT = "rvf_results.qa_result";

	/** Digest of no rows, so "ran and found nothing" is a recorded value. */
	private static final String EMPTY = DuckStore.sha256Hex("");

	/** One assertion's outcome: whether it ran, and what it found. */
	private record Outcome(String file, String status, long findings, String digest) {

		String line(String uuid) {
			return String.join("\t", uuid, file, status, String.valueOf(findings), digest);
		}
	}

	private static final Map<String, Outcome> ACTUAL = new TreeMap<>();
	/** The failure message of every assertion that did not execute, by uuid. */
	private static final Map<String, String> ERRORS = new TreeMap<>();
	private static Map<String, Outcome> golden;
	private static String storeCorpusRef;
	private static String goldenCorpusRef;

	@BeforeAll
	static void runTheWholeCorpus() throws Exception {
		DuckStore store = DuckStore.read(STORE);
		storeCorpusRef = store.corpusRef();
		Map<String, String> tableColumns = store.tableColumns();

		try (Connection con = DriverManager.getConnection("jdbc:duckdb:")) {
			DuckMaterialiser.materialise(con, PROSPECTIVE, "prospective", tableColumns);
			DuckMaterialiser.materialise(con, PREVIOUS, "previous", tableColumns);
			session(con);
			createResultTable(con);

			// Every declared table with no rows, standing in for "no dependency" as a run does:
			// a statement whose only use of the dependency is an anti-join runs against it.
			Path noDependency = Files.createTempDirectory("rvf-empty-dependency");
			try {
				DuckMaterialiser.materialise(con, noDependency, "empty", tableColumns);
			} finally {
				Files.deleteIfExists(noDependency);
			}

			DuckBinder binder = new DuckBinder(store.sentinels(), new DuckBinder.Config(
					RUN_ID, "prospective", "previous", null, QA_RESULT,
					null, List.of(), "20130731", "empty"));
			DuckDbAssertionExecutionService service = new DuckDbAssertionExecutionService(store, binder, con);
			// A setup failure throws: nothing after it would be trustworthy.
			service.prepareSchema();

			List<TestRunItem> items = service.execute(corpusInExecutionOrder(store));
			Map<String, String> digests = digestsByAssertion(con);
			Map<String, Long> counts = findingsByAssertion(con);
			for (TestRunItem item : items) {
				String uuid = String.valueOf(item.getAssertionUuid());
				String file = store.assertions().get(uuid).file();
				String status = status(item);
				if ("ERROR".equals(status)) {
					ERRORS.put(uuid, file + ": " + item.getFailureMessage());
				}
				ACTUAL.put(uuid, new Outcome(file, status, counts.getOrDefault(uuid, 0L),
						digests.getOrDefault(uuid, EMPTY)));
			}
		}

		if (Boolean.getBoolean("duck.digests.write")) {
			writeGolden(store);
		}
		golden = readGolden();
		goldenCorpusRef = readGoldenCorpusRef();
	}

	private static void writeGolden(DuckStore store) throws Exception {
		DuckStore.PackRecord identity = store.identity()
				.orElseThrow(() -> new IllegalStateException("the digest store has no identity"));
		long ran = ACTUAL.values().stream().filter(o -> "RAN".equals(o.status())).count();
		long producing = ACTUAL.values().stream().filter(o -> o.findings() > 0).count();
		StringBuilder sb = new StringBuilder();
		sb.append("# What each assertion finds against SnomedCT_RegressionTest_20130731\n")
				.append("# with SnomedCT_RegressionTest_20130131 as the previous release and no dependency.\n")
				.append("# Generated by AssertionCorpusDigestTest with -Dduck.digests.write=true from\n")
				.append("#   store ").append(identity.name()).append(' ').append(identity.version())
				.append(", ").append(identity.assertions()).append(" assertions, ").append(identity.digest()).append('\n')
				.append(CORPUS_COMMIT).append(identity.corpusRef())
				.append(", transpiled by sqlglot ").append(store.transpilerVersion()).append('\n')
				.append("#   ").append(ran).append(" ran (").append(producing).append(" with findings), ")
				.append(ACTUAL.size() - ran).append(" not run\n")
				.append("#\n")
				.append("# uuid\tfile\tstatus\tfindings\tsha256(sorted rows)\n")
				.append("#\n")
				.append("# status RAN     - executed; the digest is what it found\n")
				.append("#        NOT_RUN - needs a release this fixture does not supply\n")
				.append("#\n")
				.append("# Regenerate with -Dduck.digests.write=true and commit the diff with the reason\n")
				.append("# each moved assertion moved.\n");
		ACTUAL.forEach((uuid, o) -> sb.append(o.line(uuid)).append('\n'));
		Files.createDirectories(GOLDEN.getParent());
		Files.writeString(GOLDEN, sb.toString());
	}

	/**
	 * The corpus, with the {@code resource} assertions first.
	 *
	 * <p>They build the shared tables other assertions read ({@code res_edited_active_concepts},
	 * {@code tmp_pt}, ...), as a run executes them first.
	 */
	private static List<Assertion> corpusInExecutionOrder(DuckStore store) {
		List<Assertion> resource = new ArrayList<>();
		List<Assertion> rest = new ArrayList<>();
		store.assertions().forEach((uuid, stored) -> {
			Assertion a = new Assertion();
			a.setUuid(UUID.fromString(uuid));
			a.setAssertionText(stored.text());
			a.setKeywords(stored.keywords());
			a.setSeverity(stored.severity());
			boolean isResource = Arrays.stream(stored.keywords().split(","))
					.map(String::trim).anyMatch("resource"::equals);
			(isResource ? resource : rest).add(a);
		});
		resource.addAll(rest);
		return resource;
	}

	private static String status(TestRunItem item) {
		String message = item.getFailureMessage();
		if (message == null || message.isBlank()) {
			return "RAN";
		}
		// Not run is a fact about the fixture; anything else is a broken assertion.
		return message.startsWith("Not run: requires") ? "NOT_RUN" : "ERROR";
	}

	/**
	 * One digest per assertion, from one ordered pass over qa_result.
	 *
	 * <p>The row text is built in SQL so the rows are sorted by the same string that is
	 * hashed.
	 */
	private static Map<String, String> digestsByAssertion(Connection con) throws Exception {
		Map<String, String> out = new LinkedHashMap<>();
		String rowText = "coalesce(concept_id::VARCHAR, '') || '|' || coalesce(component_id, '') "
				+ "|| '|' || coalesce(table_name, '') || '|' || coalesce(details, '')";
		String sql = "SELECT assertion_id, " + rowText + " AS row_text FROM " + QA_RESULT
				+ " WHERE run_id = " + RUN_ID + " ORDER BY assertion_id, row_text";
		String current = null;
		StringBuilder acc = new StringBuilder();
		try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				String id = rs.getString(1);
				if (!id.equals(current)) {
					if (current != null) {
						out.put(current, DuckStore.sha256Hex(acc.toString()));
					}
					current = id;
					acc.setLength(0);
				}
				acc.append(rs.getString(2)).append('\n');
			}
		}
		if (current != null) {
			out.put(current, DuckStore.sha256Hex(acc.toString()));
		}
		return out;
	}

	/** qa_result rows per assertion. */
	private static Map<String, Long> findingsByAssertion(Connection con) throws Exception {
		Map<String, Long> out = new HashMap<>();
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("SELECT assertion_id, count(*) FROM " + QA_RESULT
						+ " WHERE run_id = " + RUN_ID + " GROUP BY assertion_id")) {
			while (rs.next()) {
				out.put(rs.getString(1), rs.getLong(2));
			}
		}
		return out;
	}

	private static Map<String, Outcome> readGolden() throws Exception {
		Map<String, Outcome> out = new TreeMap<>();
		if (!Files.exists(GOLDEN)) {
			return out;
		}
		for (String line : Files.readAllLines(GOLDEN)) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] f = line.split("\t", -1);
			out.put(f[0], new Outcome(f[1], f[2], Long.parseLong(f[3]), f[4]));
		}
		return out;
	}

	/** The prefix of the header line naming the corpus commit the recording was made from. */
	private static final String CORPUS_COMMIT = "#   assertion corpus commit ";

	private static String readGoldenCorpusRef() throws Exception {
		return Files.readAllLines(GOLDEN).stream()
				.filter(line -> line.startsWith(CORPUS_COMMIT))
				.map(line -> line.substring(CORPUS_COMMIT.length()).split(",")[0].trim())
				.findFirst().orElse("");
	}

	// ---- the checks -----------------------------------------------------

	/** Otherwise every other check here compares two different corpora. */
	@Test
	void theStoreIsPublishedFromTheCommitTheRecordingWasMadeFrom() {
		assertEquals(goldenCorpusRef, storeCorpusRef,
				"duck.digest.corpus.ref in pom.xml and the corpus commit in " + GOLDEN + " must agree");
	}

	@Test
	void everyAssertionInTheStoreHasARecordedOutcome() {
		assertTrue(ACTUAL.size() >= EXPECTED_ASSERTIONS,
				"only " + ACTUAL.size() + " assertions ran - the store looks truncated");
		Set<String> unrecorded = new TreeSet<>(ACTUAL.keySet());
		unrecorded.removeAll(golden.keySet());
		Set<String> stale = new TreeSet<>(golden.keySet());
		stale.removeAll(ACTUAL.keySet());
		assertTrue(unrecorded.isEmpty() && stale.isEmpty(),
				() -> "the recording and the store disagree about which assertions exist.\n"
						+ describe("in the store, never recorded", unrecorded)
						+ describe("recorded, no longer in the store", stale)
						+ "Regenerate with -Dduck.digests.write=true once you know why.");
	}

	/** Every assertion executes on this fixture, unless it needs a release the fixture lacks. */
	@Test
	void everyAssertionExecutes() {
		assertTrue(ERRORS.isEmpty(), () -> ERRORS.size() + " assertion(s) failed to execute:\n  "
				+ String.join("\n  ", ERRORS.values()));
	}

	/** An assertion that stops running is the regression that looks like a pass. */
	@Test
	void noAssertionStoppedRunning() {
		Set<String> newlyNotRun = new TreeSet<>();
		ACTUAL.forEach((uuid, o) -> {
			if ("NOT_RUN".equals(o.status()) && !"NOT_RUN".equals(golden.getOrDefault(uuid, o).status())) {
				newlyNotRun.add(o.file() + " (" + uuid + ")");
			}
		});
		assertTrue(newlyNotRun.isEmpty(), () -> describe("stopped running, and used to run", newlyNotRun));
	}

	@Test
	void whatEachAssertionFindsIsUnchanged() {
		List<String> moved = new ArrayList<>();
		ACTUAL.forEach((uuid, now) -> {
			Outcome then = golden.get(uuid);
			if (then == null || now.equals(then)) {
				return;
			}
			moved.add("  " + now.file() + " (" + uuid + ")\n"
					+ "    was " + then.status() + " " + then.findings() + " findings "
					+ then.digest().substring(0, 12) + "\n"
					+ "    now " + now.status() + " " + now.findings() + " findings "
					+ now.digest().substring(0, 12));
		});
		assertTrue(moved.isEmpty(),
				() -> moved.size() + " assertion(s) changed what they find:\n"
						+ String.join("\n", moved.subList(0, Math.min(20, moved.size())))
						+ (moved.size() > 20 ? "\n  ... and " + (moved.size() - 20) + " more" : "")
						+ "\nIf each is intended, regenerate with -Dduck.digests.write=true"
						+ " and commit the diff with the reason.");
	}

	/**
	 * The comparisons above are against a recording, so a change that made every assertion
	 * find nothing would pass once the recording was regenerated. This does not compare.
	 */
	@Test
	void theFixtureStillProducesFindings() {
		long producing = ACTUAL.values().stream().filter(o -> o.findings() > 0).count();
		long expected = golden.values().stream().filter(o -> o.findings() > 0).count();
		assertTrue(producing > 0,
				"no assertion found anything - two empty results agreeing proves nothing");
		assertEquals(expected, producing,
				"the number of assertions producing findings moved, so the digests are"
						+ " comparing a different corpus");
	}

	/**
	 * Assertions with findings on this fixture: may rise; lowering it is a reviewed edit here,
	 * not a side effect of regenerating the recording.
	 */
	private static final int COVERAGE_FLOOR = 228;

	@Test
	void coverageDoesNotRegress() {
		long producing = ACTUAL.values().stream().filter(o -> o.findings() > 0).count();
		assertTrue(producing >= COVERAGE_FLOOR,
				() -> "assertions with findings fell to " + producing + " from a floor of "
						+ COVERAGE_FLOOR + ". Either the fixture lost content the assertions"
						+ " depend on, or the drop is intended and this floor moves with the reason.");
	}

	// ---- fixture plumbing -----------------------------------------------

	/** The session settings a DuckDB run applies; neither carries to another connection. */
	private static void session(Connection con) throws Exception {
		try (Statement st = con.createStatement()) {
			// pre-requisites.sql names its tables unqualified.
			st.execute("SET search_path='prospective'");
			// MySQL converts between types freely; the corpus relies on it.
			st.execute("SET old_implicit_casting=true");
		}
	}

	private static void createResultTable(Connection con) throws Exception {
		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS rvf_results");
			st.execute("CREATE TABLE " + QA_RESULT + " (" + DuckDbValidationService.QA_RESULT_COLUMNS + ")");
		}
	}

	private static String describe(String label, Set<String> names) {
		if (names.isEmpty()) {
			return "";
		}
		List<String> shown = new ArrayList<>(names).subList(0, Math.min(15, names.size()));
		return "  " + names.size() + " " + label + ":\n    "
				+ String.join("\n    ", shown)
				+ (names.size() > shown.size() ? "\n    ..." : "") + "\n";
	}
}
