package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.otf.utils.SnomedUtilsBase;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Against a real in-process DuckDB: a reversed limit/offset bind or a positional column
 * lookup landing on the wrong column both execute without error, so only real queries
 * catch them.
 */
class DuckFailuresExtractorTest {

	private static final String QA_RESULT = "rvf_results.qa_result";

	private Connection con;
	private final List<Assertion> assertionList = new ArrayList<>();
	private final Supplier<List<Assertion>> assertions = () -> assertionList;
	private WhitelistService whitelistService;

	@BeforeEach
	void setUp() throws Exception {
		con = DriverManager.getConnection("jdbc:duckdb:");
		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA prospective");
			// setModuleAndFullFields reads moduleId by position (4th), as in the RF2 file.
			st.execute("CREATE TABLE prospective.concept_s(id BIGINT, effectivetime VARCHAR, "
					+ "active VARCHAR, moduleid VARCHAR, definitionstatusid BIGINT)");
			st.execute("INSERT INTO prospective.concept_s VALUES "
					+ "(100,'20260101','1','mod-included',900000000000074008),"
					+ "(200,'20260101','1','mod-excluded',900000000000074008),"
					+ "(300,'20260101','1','mod-excluded',900000000000074008)");

			st.execute("CREATE SCHEMA rvf_results");
			st.execute("CREATE TABLE rvf_results.qa_result(run_id BIGINT, assertion_id VARCHAR, "
					+ "concept_id BIGINT, details VARCHAR, component_id VARCHAR, "
					+ "table_name VARCHAR, skip_module_check BOOLEAN)");
		}
		whitelistService = mock(WhitelistService.class);
	}

	@AfterEach
	void tearDown() throws Exception {
		con.close();
	}

	private void insertQaResult(long runId, String assertionUuid, long conceptId, String componentId,
			String tableName, boolean skipModuleCheck) throws Exception {
		try (Statement st = con.createStatement()) {
			st.execute("INSERT INTO rvf_results.qa_result VALUES (" + runId + ",'" + assertionUuid + "',"
					+ conceptId + ",'detail',"
					+ (componentId == null ? "NULL" : "'" + componentId + "'") + ","
					+ (tableName == null ? "NULL" : "'" + tableName + "'") + "," + skipModuleCheck + ")");
		}
	}

	private DuckFailuresExtractor extractor() {
		return extractor(1000);
	}

	private DuckFailuresExtractor extractor(int whitelistBatchSize) {
		return new DuckFailuresExtractor(con, QA_RESULT, whitelistService, assertions, whitelistBatchSize,
				1000, List.of("prospective"));
	}

	private static TestRunItem item(String uuid) {
		TestRunItem item = new TestRunItem();
		item.setAssertionUuid(UUID.fromString(uuid));
		return item;
	}

	private static Assertion assertion(String uuid) {
		Assertion a = new Assertion();
		a.setUuid(UUID.fromString(uuid));
		a.setAssertionId(1L);
		return a;
	}

	@Test
	void failureCountsAreGroupedByAssertionUuidNotByANumericId() throws Exception {
		insertQaResult(7L, "11111111-1111-1111-1111-111111111111", 1L, null, null, false);
		insertQaResult(7L, "11111111-1111-1111-1111-111111111111", 2L, null, null, false);

		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);

		List<TestRunItem> items = List.of(
				item("11111111-1111-1111-1111-111111111111"),
				item("22222222-2222-2222-2222-222222222222"));
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(10);

		extractor.extractTestResults(items, config);

		assertEquals(2L, items.get(0).getFailureCount());
		// Ran, no rows, no failure message: a pass, not testsIncomplete.
		assertEquals(0L, items.get(1).getFailureCount());
		assertTrue(items.get(1).getFirstNInstances().isEmpty());
	}

	@Test
	void aFailureMessageWithNoQaResultRowsIsTestsIncompleteNotAPass() throws Exception {
		// -1 marks testsIncomplete; 0 would read as a pass.
		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);

		TestRunItem failed = item("33333333-3333-3333-3333-333333333333");
		failed.setFailureMessage("statement timeout");

		extractor.extractTestResults(List.of(failed), new MysqlExecutionConfig(7L));

		assertEquals(-1L, failed.getFailureCount());
		assertTrue(failed.getFirstNInstances().isEmpty());
	}

	@Test
	void failureExportMaxCapsFirstNInstancesButNotTheReportedTotal() throws Exception {
		String uuid = "44444444-4444-4444-4444-444444444444";
		for (long i = 1; i <= 5; i++) {
			insertQaResult(7L, uuid, i, null, null, false);
		}

		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(3);

		extractor.extractTestResults(List.of(testItem), config);

		assertEquals(5L, testItem.getFailureCount());
		assertEquals(3, testItem.getFirstNInstances().size());
	}

	@Test
	void pagingFetchesEveryBatchWithoutLossOrDuplication() throws Exception {
		// A batch size below the total makes a reversed limit/offset bind observable: it
		// would silently return fewer rows.
		String uuid = "55555555-5555-5555-5555-555555555555";
		Set<Long> expected = new HashSet<>();
		for (long i = 1; i <= 5; i++) {
			insertQaResult(7L, uuid, i, null, null, false);
			expected.add(i);
		}

		DuckFailuresExtractor extractor = extractor(2);
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		assertionList.clear();
		assertionList.addAll(List.of(assertion(uuid)));
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(Collections.emptyList());

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);

		extractor.extractTestResults(List.of(testItem), config);

		assertEquals(5L, testItem.getFailureCount());
		Set<Long> actual = new HashSet<>();
		for (FailureDetail detail : testItem.getFirstNInstances()) {
			actual.add(Long.valueOf(detail.getConceptId()));
		}
		assertEquals(expected, actual);
	}

	@Test
	void moduleFilteringKeepsIncludedModulesAndBypassesForSkipModuleCheck() throws Exception {
		String uuid = "66666666-6666-6666-6666-666666666666";
		// concept 100 -> mod-included: kept because it is in includedModules.
		insertQaResult(7L, uuid, 100L, "100", "prospective.concept_s", false);
		// concept 200 -> mod-excluded, no bypass: filtered out.
		insertQaResult(7L, uuid, 200L, "200", "prospective.concept_s", false);
		// concept 300 -> mod-excluded, but skipModuleCheck=true bypasses the filter.
		insertQaResult(7L, uuid, 300L, "300", "prospective.concept_s", true);

		// The module filter applies only to common-authoring/common-edition assertions;
		// the store-backed source sets the groups on the Assertion itself.
		Assertion assertionInGroup = assertion(uuid);
		assertionInGroup.setGroups(Set.of("common-authoring"));

		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		assertionList.clear();
		assertionList.addAll(List.of(assertionInGroup));
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(Collections.emptyList());

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);
		config.setExtensionValidation(true);
		config.setIncludedModules(List.of("mod-included"));

		extractor.extractTestResults(List.of(testItem), config);

		// 3 total, 1 filtered out (concept 200), none whitelisted -> 2 remain.
		assertEquals(2L, testItem.getFailureCount());
		Set<String> keptComponentIds = new HashSet<>();
		for (FailureDetail detail : testItem.getFirstNInstances()) {
			keptComponentIds.add(detail.getComponentId());
		}
		assertEquals(Set.of("100", "300"), keptComponentIds);
	}

	@Test
	void setModuleAndFullFieldsReadsModuleIdByPositionFromTheReleaseSchema() throws Exception {
		String uuid = "77777777-7777-7777-7777-777777777777";
		insertQaResult(7L, uuid, 100L, "100", "prospective.concept_s", false);

		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		assertionList.clear();
		assertionList.addAll(List.of(assertion(uuid)));
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(Collections.emptyList());

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);

		extractor.extractTestResults(List.of(testItem), config);

		assertEquals(1, testItem.getFirstNInstances().size());
		FailureDetail detail = testItem.getFirstNInstances().get(0);
		// Position 4 in concept_s (id, effectivetime, active, moduleid, ...).
		assertEquals("mod-included", detail.getModuleId());
		// Every column but id and effectiveTime, comma-joined.
		assertEquals("1,mod-included,900000000000074008", detail.getFullComponent());
	}

	@Test
	void aTableNameThatIsNotAKnownTableIsNeverConcatenatedIntoAQuery() throws Exception {
		// table_name comes from assertion SQL and cannot be a bind parameter, so it is
		// checked against the catalog before being concatenated.
		String uuid = "88888888-8888-8888-8888-888888888888";
		insertQaResult(7L, uuid, 100L, "100",
				"prospective.concept_s where 1=0 union all select * from prospective.concept_s --",
				false);

		DuckFailuresExtractor extractor = extractor();
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		assertionList.clear();
		assertionList.addAll(List.of(assertion(uuid)));
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(Collections.emptyList());

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(100);

		extractor.extractTestResults(List.of(testItem), config);

		// The finding still surfaces; only its enrichment is refused.
		assertEquals(1L, testItem.getFailureCount());
		assertEquals(1, testItem.getFirstNInstances().size());
		assertNull(testItem.getFirstNInstances().get(0).getModuleId());
	}

	@Test
	void aQaResultTableNameThatIsNotAnIdentifierIsRejectedAtConstruction() {
		assertThrows(IllegalArgumentException.class, () -> new DuckFailuresExtractor(
				con, "qa_result; drop table concept_s", whitelistService, assertions, 1000, 1000,
				List.of("prospective")));
	}

	@Test
	void anFsnLookupSchemaThatIsNotAnIdentifierIsRejectedAtConstruction() {
		assertThrows(IllegalArgumentException.class, () -> new DuckFailuresExtractor(
				con, QA_RESULT, whitelistService, assertions, 1000, 1000,
				List.of("prospective; drop table concept_s")));
	}

	private void createDescriptions(String schema, String... rows) throws Exception {
		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
			st.execute("CREATE TABLE IF NOT EXISTS " + schema + ".description_s(id BIGINT, effectivetime VARCHAR, "
					+ "active VARCHAR, moduleid BIGINT, conceptid BIGINT, languagecode VARCHAR, typeid BIGINT, "
					+ "term VARCHAR, casesignificanceid BIGINT)");
			for (String row : rows) {
				st.execute("INSERT INTO " + schema + ".description_s VALUES (" + row + ")");
			}
		}
	}

	private static String fsn(long id, String effectiveTime, String active, long conceptId, String language,
			String term) {
		return description(id, effectiveTime, active, conceptId, language, FSN_TYPE, term);
	}

	private static String description(long id, String effectiveTime, String active, long conceptId,
			String language, long typeId, String term) {
		return id + ",'" + effectiveTime + "','" + active + "',900000000000207008," + conceptId + ",'"
				+ language + "'," + typeId + ",'" + term + "',900000000000448009";
	}

	private static final long FSN_TYPE = 900000000000003001L;
	private static final long SYNONYM_TYPE = 900000000000013009L;
	private static final long CONCEPT_A = 698642008L;
	private static final long CONCEPT_B = 697990000L;
	/** A description id: an SCTID, but not a concept's. */
	private static final long DESCRIPTION_ID = 2974827019L;

	@Test
	void conceptFsnIsTheActiveEnglishMostRecentFsn() throws Exception {
		createDescriptions("prospective",
				fsn(1001L, "20250101", "0", CONCEPT_A, "en", "Inactive term (procedure)"),
				fsn(1002L, "20250101", "1", CONCEPT_A, "fr", "Terme français (procédure)"),
				fsn(1003L, "20200101", "1", CONCEPT_A, "en", "Older term (procedure)"),
				fsn(1004L, "20230101", "1", CONCEPT_A, "en", "Current term (procedure)"),
				description(1005L, "20250101", "1", CONCEPT_A, "en", SYNONYM_TYPE, "A synonym"));
		String uuid = "99999999-9999-9999-9999-999999999999";
		insertQaResult(7L, uuid, CONCEPT_A, null, null, false);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(10);
		extractor().extractTestResults(List.of(testItem), config);

		assertEquals("Current term (procedure)", testItem.getFirstNInstances().get(0).getConceptFsn());
	}

	@Test
	void conceptFsnLeavesNonConceptIdsAndUnknownConceptsAlone() throws Exception {
		createDescriptions("prospective",
				fsn(1001L, "20230101", "1", CONCEPT_A, "en", "Concept A (procedure)"),
				// Keyed on the description id: must not be picked up for a non-concept id.
				fsn(1002L, "20230101", "1", DESCRIPTION_ID, "en", "Not a concept (procedure)"));
		assertTrue(SnomedUtilsBase.isSctid(String.valueOf(DESCRIPTION_ID)));
		assertFalse(SnomedUtilsBase.isConceptSctid(String.valueOf(DESCRIPTION_ID)));
		String uuid = "99999999-9999-9999-9999-999999999998";
		insertQaResult(7L, uuid, DESCRIPTION_ID, null, null, false);
		insertQaResult(7L, uuid, CONCEPT_B, null, null, false);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(10);
		extractor().extractTestResults(List.of(testItem), config);

		assertEquals(2, testItem.getFirstNInstances().size());
		for (FailureDetail detail : testItem.getFirstNInstances()) {
			assertNull(detail.getConceptFsn(), detail.getConceptId());
		}
	}

	@Test
	void conceptFsnSchemasAreTriedInOrderAcrossBatches() throws Exception {
		createDescriptions("prospective", fsn(1001L, "20230101", "1", CONCEPT_A, "en", "A from release (procedure)"));
		createDescriptions("dependency",
				fsn(2001L, "20230101", "1", CONCEPT_A, "en", "A from dependency (procedure)"),
				fsn(2002L, "20230101", "1", CONCEPT_B, "en", "B from dependency (finding)"));
		String uuid = "99999999-9999-9999-9999-999999999997";
		insertQaResult(7L, uuid, CONCEPT_A, null, null, false);
		insertQaResult(7L, uuid, CONCEPT_B, null, null, false);
		when(whitelistService.isWhitelistDisabled()).thenReturn(false);
		assertionList.add(assertion(uuid));
		when(whitelistService.checkComponentFailuresAgainstWhitelist(any())).thenReturn(Collections.emptyList());

		TestRunItem testItem = item(uuid);
		MysqlExecutionConfig config = new MysqlExecutionConfig(7L);
		config.setFailureExportMax(10);
		// FSN batch size 1: every concept is its own lookup.
		new DuckFailuresExtractor(con, QA_RESULT, whitelistService, assertions, 1000, 1,
				List.of("prospective", "dependency")).extractTestResults(List.of(testItem), config);

		assertEquals(2, testItem.getFirstNInstances().size());
		for (FailureDetail detail : testItem.getFirstNInstances()) {
			String expected = detail.getConceptId().equals(String.valueOf(CONCEPT_A))
					? "A from release (procedure)" : "B from dependency (finding)";
			assertEquals(expected, detail.getConceptFsn());
		}
	}
}
