package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The DuckDB classes composed: source -> execution -> extraction.
 *
 * <p>Each class can pass its own tests while they disagree with each other, e.g. on how
 * qa_result is keyed; that disagreement reports every assertion as a pass, not an error.
 * Assertions from the store carry no numeric id, so everything must key on the uuid.
 */
class DuckEnginePipelineTest {

	private static final String UUID_INACTIVE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_CLEAN = "22222222-2222-2222-2222-222222222222";

	private static final String STORE = """
			{
			 "formatVersion": 1,
			 "sentinels": [
			  {"placeholder": "<RUNID>", "sentinel": "424242424242424242"},
			  {"placeholder": "<ASSERTIONUUID>", "sentinel": "rvfph_assertionuuid_"},
			  {"placeholder": "<PROSPECTIVE>", "sentinel": "rvfph_prospective_"}
			 ],
			 "tableColumns": {},
			 "ports": [],
			 "prerequisites": [],
			 "assertions": {
			  "11111111-1111-1111-1111-111111111111": {
			   "file": "inactive.sql", "text": "No inactive concepts", "keywords": "component-centric-validation",
			   "statements": [
				"insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'inactive concept', id, 'prospective.concept_s' from rvfph_prospective_.concept_s where active = '0'"]},
			  "22222222-2222-2222-2222-222222222222": {
			   "file": "clean.sql", "text": "Nothing wrong here", "keywords": "component-centric-validation",
			   "statements": [
				"insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'never' from rvfph_prospective_.concept_s where 1 = 0"]}
			 }
			}
			""";

	// Applied by the real group rule engine, so this group is what selects the assertions.
	private static final String GROUPS_XML = """
			<assertionGroupingStrategy>
			 <group name="component-centric-validation" includeStandaloneCategories="component-centric-validation" />
			</assertionGroupingStrategy>
			""";

	private static final String POLICIES_XML = "<policyValues/>";

	/** The store, carrying its grouping files as a published store does. */
	private static DuckStore store() throws Exception {
		ObjectNode root = (ObjectNode) DuckStore.MAPPER.readTree(STORE);
		var grouping = root.putArray("grouping");
		grouping.addObject().put("file", "groups.xml")
				.put("sha256", DuckStore.sha256Hex(GROUPS_XML).substring(0, 16)).put("text", GROUPS_XML);
		grouping.addObject().put("file", "policies.xml")
				.put("sha256", DuckStore.sha256Hex(POLICIES_XML).substring(0, 16)).put("text", POLICIES_XML);
		return DuckStore.parse(root.toString());
	}

	private Connection con;
	private WhitelistService whitelistService;

	@BeforeEach
	void setUp() throws Exception {
		con = DriverManager.getConnection("jdbc:duckdb:");
		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA prospective");
			st.execute("SET search_path='prospective'");
			st.execute("SET old_implicit_casting=true");
			st.execute("CREATE TABLE prospective.concept_s("
					+ "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid VARCHAR)");
			st.execute("INSERT INTO prospective.concept_s VALUES "
					+ "(1,'20260831','1','900000000000207008'), "
					+ "(2,'20260831','0','900000000000207008'), "
					+ "(3,'20260831','0','900000000000207008')");
			st.execute("CREATE SCHEMA rvf_results");
			st.execute("CREATE TABLE rvf_results.qa_result(run_id BIGINT, assertion_id VARCHAR, "
					+ "concept_id BIGINT, details VARCHAR, component_id VARCHAR, "
					+ "table_name VARCHAR, skip_module_check VARCHAR)");
		}
		whitelistService = mock(WhitelistService.class);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);
	}

	@AfterEach
	void tearDown() throws Exception {
		con.close();
	}

	@Test
	void assertionsFlowFromStoreToReportWithoutMysql() throws Exception {
		DuckStore store = store();
		DuckAssertionSource source = DuckAssertionSource.from(store);

		List<Assertion> assertions = source.getAssertionsInGroups(List.of("component-centric-validation"));
		assertEquals(2, assertions.size(), "group resolution should select both assertions");
		// The store has no numeric id, so nothing downstream may depend on one.
		assertions.forEach(a -> assertNull(a.getAssertionId()));

		DuckBinder binder = new DuckBinder(store.sentinels(), new DuckBinder.Config(
				7L, "prospective", null, null, "rvf_results.qa_result", null, List.of(), "20260831"));
		List<TestRunItem> items = new DuckDbAssertionExecutionService(store, binder, con)
				.execute(assertions);

		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);
		// source::findAll supplies assertions with their resolved groups; no MySQL involved.
		new DuckFailuresExtractor(con, "rvf_results.qa_result", whitelistService, source::findAll,
				1000, 1000, List.of("prospective"))
				.extractTestResults(items, config);

		TestRunItem inactive = items.stream()
				.filter(i -> UUID_INACTIVE.equals(String.valueOf(i.getAssertionUuid()))).findFirst().orElseThrow();
		TestRunItem clean = items.stream()
				.filter(i -> UUID_CLEAN.equals(String.valueOf(i.getAssertionUuid()))).findFirst().orElseThrow();

		// Two inactive concepts, found and attributed to the right assertion.
		assertNull(inactive.getFailureMessage());
		assertEquals(2L, inactive.getFailureCount());
		assertEquals(2, inactive.getFirstNInstances().size());

		// A pass: ran, found nothing (a keying disagreement would report both as zero).
		assertNull(clean.getFailureMessage());
		assertEquals(0L, clean.getFailureCount());
		assertTrue(clean.getFirstNInstances().isEmpty());
	}

	@Test
	void failureDetailsAreEnrichedFromTheReleaseSchemaOnTheSameConnection() throws Exception {
		DuckStore store = store();
		DuckAssertionSource source = DuckAssertionSource.from(store);
		DuckBinder binder = new DuckBinder(store.sentinels(), new DuckBinder.Config(
				7L, "prospective", null, null, "rvf_results.qa_result", null, List.of(), "20260831"));

		List<Assertion> assertions = source.getAssertionsInGroups(List.of("component-centric-validation"));
		List<TestRunItem> items = new DuckDbAssertionExecutionService(store, binder, con).execute(assertions);

		// Enrichment happens only on the whitelist-enabled path, as in MysqlFailuresExtractor.
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(List.of());

		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);
		// source::findAll supplies assertions with their resolved groups; no MySQL involved.
		new DuckFailuresExtractor(con, "rvf_results.qa_result", whitelistService, source::findAll,
				1000, 1000, List.of("prospective"))
				.extractTestResults(items, config);

		TestRunItem inactive = items.stream()
				.filter(i -> UUID_INACTIVE.equals(String.valueOf(i.getAssertionUuid()))).findFirst().orElseThrow();
		// table_name written by the assertion resolves to the release table, so moduleId is read.
		assertNotNull(inactive.getFirstNInstances().get(0).getModuleId());
		assertEquals("900000000000207008", inactive.getFirstNInstances().get(0).getModuleId());
	}
}
