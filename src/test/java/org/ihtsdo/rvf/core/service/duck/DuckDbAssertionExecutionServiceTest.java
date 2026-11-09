package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Against a real in-process DuckDB: what matters is whether the bound SQL executes, whether
 * qa_result is keyed the way the extractor reads it, and whether an assertion that could
 * not run says so.
 */
class DuckDbAssertionExecutionServiceTest {

	private static final String STORE = """
			{
			 "formatVersion": 1,
			 "sentinels": [
			  {"placeholder": "<RUNID>", "sentinel": "424242424242424242"},
			  {"placeholder": "<ASSERTIONUUID>", "sentinel": "rvfph_assertionuuid_"},
			  {"placeholder": "<PROSPECTIVE>", "sentinel": "rvfph_prospective_"},
			  {"placeholder": "<PREVIOUS>", "sentinel": "rvfph_previous_"}
			 ],
			 "tableColumns": {},
			 "ports": ["CREATE OR REPLACE MACRO rvfph_prospective_.isOne(x) AS x = 1",
			  "create table rvfph_prospective_.derived_from_prereq as select count(*) n from rvfph_prospective_.concept_active"],
			 "prerequisites": [
			  {"file": "pre-requisites.sql", "statements": [
			   "create table rvfph_prospective_.concept_active as select id from rvfph_prospective_.concept_s where active = '1'"]}
			 ],
			 "assertions": {
			  "11111111-1111-1111-1111-111111111111": {
			   "file": "finds-two.sql", "text": "Two inactive concepts", "keywords": "k",
			   "statements": [
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'inactive' from rvfph_prospective_.concept_s where active = '0'"]},
			  "22222222-2222-2222-2222-222222222222": {
			   "file": "needs-previous.sql", "text": "Needs previous", "keywords": "k",
			   "statements": [
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'x' from rvfph_previous_.concept_s"]},
			  "55555555-5555-5555-5555-555555555555": {
			   "file": "partly-needs-previous.sql", "text": "Partly needs previous", "keywords": "k",
			   "statements": [
				"create table rvfph_prospective_.tmp_active_desc as select id from rvfph_previous_.concept_s",
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'y' from rvfph_prospective_.tmp_active_desc"]},
			  "66666666-6666-6666-6666-666666666666": {
			   "file": "only-writer-needs-previous.sql", "text": "Only writer needs previous", "keywords": "k",
			   "statements": [
				"create table rvfph_prospective_.tmp_all as select id from rvfph_prospective_.concept_s",
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', t.id, 'z' from rvfph_prospective_.tmp_all t join rvfph_previous_.concept_s p on p.id = t.id"]},
			  "33333333-3333-3333-3333-333333333333": {
			   "file": "broken.sql", "text": "Broken SQL", "keywords": "k",
			   "statements": ["select * from rvfph_prospective_.no_such_table"]},
			  "44444444-4444-4444-4444-444444444444": {
			   "file": "uses-port.sql", "text": "Uses a ported macro", "keywords": "k",
			   "statements": [
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'one' from rvfph_prospective_.concept_s where isOne(id)"]}
			 }
			}
			""";

	private Connection con;
	private DuckDbAssertionExecutionService service;

	@BeforeEach
	void setUp() throws Exception {
		con = DriverManager.getConnection("jdbc:duckdb:");
		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA prospective");
			st.execute("SET search_path='prospective'");
			st.execute("SET old_implicit_casting=true");
			st.execute("CREATE TABLE prospective.concept_s(id BIGINT, active VARCHAR)");
			st.execute("INSERT INTO prospective.concept_s VALUES (1,'1'),(2,'0'),(3,'0')");
			st.execute("CREATE SCHEMA rvf_results");
			st.execute("CREATE TABLE rvf_results.qa_result(run_id BIGINT, assertion_id VARCHAR, "
					+ "concept_id BIGINT, details VARCHAR, component_id VARCHAR, "
					+ "table_name VARCHAR, skip_module_check VARCHAR)");
		}
		DuckStore store = DuckStore.parse(STORE);
		// No previous release, which is what makes the skip path testable.
		DuckBinder binder = new DuckBinder(store.sentinels(), new DuckBinder.Config(
				7L, "prospective", null, null, "rvf_results.qa_result", null, List.of(), "20260831"));
		service = new DuckDbAssertionExecutionService(store, binder, con);
	}

	@AfterEach
	void tearDown() throws Exception {
		con.close();
	}

	/** qa_result rows per assertion id for run 7. */
	private Map<String, Long> findings() throws SQLException {
		Map<String, Long> counts = new HashMap<>();
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select assertion_id, count(*) from rvf_results.qa_result "
						+ "where run_id = 7 group by assertion_id")) {
			while (rs.next()) {
				counts.put(rs.getString(1), rs.getLong(2));
			}
		}
		return counts;
	}

	private static Assertion assertion(String uuid, long id, String text) {
		Assertion a = new Assertion();
		a.setUuid(UUID.fromString(uuid));
		a.setAssertionId(id);
		a.setAssertionText(text);
		a.setKeywords("k");
		a.setSeverity("ERROR");
		return a;
	}

	@Test
	void prepareSchemaAppliesEverySetupStatement() {
		assertEquals(3, service.prepareSchema());
	}

	@Test
	void aFailedSetupStatementAbortsTheRunRatherThanDegradingIt() throws Exception {
		// A missing macro or table changes only the assertions that use it, so a run on a
		// half-built schema would look normal; any setup failure is fatal.
		DuckStore broken = DuckStore.parse(STORE.replace(
				"\"ports\": [", "\"ports\": [\"create table x as select * from no_such_relation\", "));
		DuckDbAssertionExecutionService svc = new DuckDbAssertionExecutionService(broken,
				new DuckBinder(broken.sentinels(), new DuckBinder.Config(7L, "prospective",
						null, null, "rvf_results.qa_result", null, List.of(), "20260831")),
				con);

		DuckDbAssertionExecutionService.SetupFailedException e = assertThrows(
				DuckDbAssertionExecutionService.SetupFailedException.class, svc::prepareSchema);
		assertEquals(1, e.getFailures().size());
		assertTrue(e.getFailures().get(0).contains("no_such_relation"), e.getFailures().get(0));
		assertTrue(e.getMessage().contains("no assertion result from this run is trustworthy"), e.getMessage());
	}

	@Test
	void portsAreAppliedAfterPrerequisitesSoAPortCanUseAPrerequisiteTable() throws Exception {
		// A port may read a table a pre-requisite builds (transitiveClosureTable reads
		// relationship_active), so pre-requisites must run first.
		service.prepareSchema();
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(
						"select n from prospective.derived_from_prereq")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
	}

	@Test
	void anAssertionWritesToQaResultUnderItsAssertionIdNotItsUuid() throws Exception {
		service.prepareSchema();
		List<TestRunItem> items = service.execute(
				List.of(assertion("11111111-1111-1111-1111-111111111111", 501L, "Two inactive concepts")));

		assertEquals(1, items.size());
		assertNull(items.get(0).getFailureMessage());
		assertNotNull(items.get(0).getRunTime());
		// Keyed on the UUID, which is what DuckFailuresExtractor reads; a mismatch would make
		// every assertion silently report a pass.
		assertEquals(Map.of("11111111-1111-1111-1111-111111111111", 2L), findings());
	}

	@Test
	void anAssertionNeedingAnUnsuppliedReleaseSaysSoRatherThanPassing() {
		service.prepareSchema();
		TestRunItem item = service.execute(
				List.of(assertion("22222222-2222-2222-2222-222222222222", 502L, "Needs previous"))).get(0);
		// A zero failure count would report a PASS for a check nothing ran.
		assertNotNull(item.getFailureMessage());
		assertTrue(item.getFailureMessage().contains("<PREVIOUS>"), item.getFailureMessage());
		assertTrue(item.getFailureMessage().startsWith("Not run:"), item.getFailureMessage());
	}

	@Test
	void anAssertionWhoseOnlyFindingWriterIsSkippedIsNotRunRatherThanPassed() {
		// Its other statements run, but only a qa_result writer can report a finding, so
		// counting executed statements would wrongly read this as a pass.
		service.prepareSchema();
		TestRunItem item = service.execute(List.of(
				assertion("66666666-6666-6666-6666-666666666666", 506L, "Only writer needs previous")))
				.get(0);

		assertNotNull(item.getFailureMessage(),
				"an assertion that could not have reported anything must not read as a pass");
		assertTrue(item.getFailureMessage().startsWith("Not run:"), item.getFailureMessage());
		assertTrue(item.getFailureMessage().contains("<PREVIOUS>"), item.getFailureMessage());
	}

	@Test
	void aPartlyDependentAssertionNamesTheMissingReleaseNotJustTheMissingTable() {
		// A skipped statement built the table a later one reads; the error must name the
		// missing release, not just the missing table.
		service.prepareSchema();
		TestRunItem item = service.execute(List.of(
				assertion("55555555-5555-5555-5555-555555555555", 505L, "Partly needs previous")))
				.get(0);

		String message = item.getFailureMessage();
		assertNotNull(message);
		assertTrue(message.contains("<PREVIOUS>"), message);
		assertTrue(message.contains("builds what this one reads"), message);
		// Still an execution error, not "not run": a qa_result writer did run.
		assertTrue(message.startsWith("Error executing DuckDB statement"), message);
	}

	@Test
	void aFailingStatementIsReportedAgainstItsOwnAssertionAndDoesNotStopTheRest() throws Exception {
		service.prepareSchema();
		List<TestRunItem> items = service.execute(List.of(
				assertion("33333333-3333-3333-3333-333333333333", 503L, "Broken SQL"),
				assertion("11111111-1111-1111-1111-111111111111", 501L, "Two inactive concepts")));

		assertNotNull(items.get(0).getFailureMessage());
		assertNull(items.get(1).getFailureMessage());
		assertEquals(Map.of("11111111-1111-1111-1111-111111111111", 2L), findings());
	}

	@Test
	void anAssertionCanUseAPortedMacro() throws Exception {
		service.prepareSchema();
		TestRunItem item = service.execute(
				List.of(assertion("44444444-4444-4444-4444-444444444444", 504L, "Uses a ported macro"))).get(0);
		assertNull(item.getFailureMessage());
		assertEquals(Map.of("44444444-4444-4444-4444-444444444444", 1L), findings());
	}

	@Test
	void anAssertionMissingFromTheStoreIsReportedNotSilentlyPassed() {
		TestRunItem item = service.execute(
				List.of(assertion("99999999-9999-9999-9999-999999999999", 599L, "Not published"))).get(0);
		assertNotNull(item.getFailureMessage());
		assertTrue(item.getFailureMessage().contains("No precompiled statements"), item.getFailureMessage());
	}
}
