package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.ReleaseAcquisitionService;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A whole validation against a real DuckDB and a real RF2 release on disk.
 *
 * <p>Only progress reporting and the whitelist are mocked: there is no MySQL and no Spring
 * context. The failures this class can have are failures of order between materialising,
 * setup, resource assertions, assertions and extraction, which a stubbed step would hide.
 *
 * <p>Each expected count can come out right for only one reason:
 * <ul>
 * <li>{@code inactive} calls a macro defined by the store's ports, so 2 means the ports ran.
 * <li>{@code versionBound} reads a table the resource assertion builds from a view the
 *     pre-requisites create, and filters on {@code <VERSION>}: 1 means pre-requisites ran
 *     first, resource assertions ran before the others, and {@code <VERSION>} bound to the
 *     release's own effectiveTime.
 * <li>{@code needsPrevious} names {@code <PREVIOUS>}, so its outcome changes when a previous
 *     release is supplied.
 * </ul>
 */
class DuckDbValidationServiceTest {

	private static final String UUID_RESOURCE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private static final String UUID_INACTIVE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_CLEAN = "22222222-2222-2222-2222-222222222222";
	private static final String UUID_NEEDS_PREVIOUS = "33333333-3333-3333-3333-333333333333";
	private static final String UUID_VERSION_BOUND = "44444444-4444-4444-4444-444444444444";
	private static final String UUID_NOT_IN_DEPENDENCY = "55555555-5555-5555-5555-555555555555";
	private static final String UUID_COMPARES_DEPENDENCY = "66666666-6666-6666-6666-666666666666";
	private static final String UUID_DELTA_ROWS = "77777777-7777-7777-7777-777777777777";
	private static final String UUID_FULL_ROWS = "88888888-8888-8888-8888-888888888888";
	private static final String UUID_PARTLY_NEEDS_PREVIOUS = "99999999-9999-9999-9999-999999999999";
	private static final String UUID_READS_A_FILE = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	private static final String UUID_WRITES_A_FILE = "cccccccc-cccc-cccc-cccc-cccccccccccc";

	/** {@code rvf.empty-release-file}'s default. */
	private static final String EMPTY_RELEASE = "empty-rf2-snapshot.zip";

	private static final String GOOD_PREREQUISITE =
			"CREATE OR REPLACE VIEW concept_active AS SELECT * FROM concept_s WHERE active = '1'";

	private static final String MODULE = "900000000000207008";

	/**
	 * The pre-requisite is unqualified, as the real pre-requisites.sql is: it resolves only
	 * because the service sets search_path to the prospective schema first.
	 */
	private static final String STORE = """
			{
			 "formatVersion": 1,
			 "generator": {"sqlglot": "30.18.0", "publisher": "sha256:test"},
			 "sentinels": [
			  {"placeholder": "<RUNID>", "sentinel": "424242424242424242"},
			  {"placeholder": "<ASSERTIONUUID>", "sentinel": "rvfph_assertionuuid_"},
			  {"placeholder": "<PROSPECTIVE>", "sentinel": "rvfph_prospective_"},
			  {"placeholder": "<PREVIOUS>", "sentinel": "rvfph_previous_"},
			  {"placeholder": "<DEPENDENCY>", "sentinel": "rvfph_dependency_"},
			  {"placeholder": "<VERSION>", "sentinel": "rvfph_version_"}
			 ],
			 "tableColumns": {
			  "concept_s": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, definitionstatusid BIGINT",
			  "concept_d": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, definitionstatusid BIGINT",
			  "concept_f": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, definitionstatusid BIGINT",
			  "description_s": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, conceptid BIGINT, languagecode VARCHAR, typeid BIGINT, term VARCHAR, casesignificanceid BIGINT"
			 },
			 "prerequisites": [
			  {"file": "pre-requisites.sql", "sha256": "0123456789abcdef", "statements": [
			   "CREATE OR REPLACE VIEW concept_active AS SELECT * FROM concept_s WHERE active = '1'"]}
			 ],
			 "ports": [
			  "CREATE OR REPLACE MACRO isInactive(a) AS (a = '0')"
			 ],
			 "assertions": {
			  "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa": {
			   "file": "resource.sql", "sha256": "aaaaaaaaaaaaaaaa", "text": "Build the shared active-concept table",
			   "keywords": "resource", "severity": "",
			   "statements": [
			    "CREATE OR REPLACE TABLE rvfph_prospective_.res_edited_concepts AS SELECT id, effectivetime FROM rvfph_prospective_.concept_active"]},
			  "11111111-1111-1111-1111-111111111111": {
			   "file": "inactive.sql", "sha256": "1111111111111111", "text": "No inactive concepts",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'inactive concept', id, 'prospective.concept_s' from rvfph_prospective_.concept_s where isInactive(active)"]},
			  "22222222-2222-2222-2222-222222222222": {
			   "file": "clean.sql", "sha256": "2222222222222222", "text": "Nothing wrong here",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'never' from rvfph_prospective_.concept_s where 1 = 0"]},
			  "33333333-3333-3333-3333-333333333333": {
			   "file": "needs-previous.sql", "sha256": "3333333333333333", "text": "Every concept existed in the previous release",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', p.id, 'new concept', p.id, 'prospective.concept_s' from rvfph_prospective_.concept_s p where not exists (select 1 from rvfph_previous_.concept_s v where v.id = p.id)"]},
			  "44444444-4444-4444-4444-444444444444": {
			   "file": "version-bound.sql", "sha256": "4444444444444444", "text": "Active concepts at this release version",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'active at version', id, 'prospective.concept_s' from rvfph_prospective_.res_edited_concepts where effectivetime = 'rvfph_version_'"]},
			  "55555555-5555-5555-5555-555555555555": {
			   "file": "not-in-dependency.sql", "sha256": "5555555555555555", "text": "Concepts the dependency does not have",
			   "keywords": "dependency-comparison", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', p.id, 'not in dependency' from rvfph_prospective_.concept_s AS p LEFT JOIN rvfph_dependency_.concept_s AS d ON d.id = p.id WHERE d.id IS NULL"]},
			  "66666666-6666-6666-6666-666666666666": {
			   "file": "compares-dependency.sql", "sha256": "6666666666666666", "text": "Concepts whose status differs from the dependency",
			   "keywords": "dependency-comparison", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', p.id, 'status differs' from rvfph_prospective_.concept_s AS p JOIN rvfph_dependency_.concept_s AS d ON d.id = p.id WHERE d.active <> p.active"]},
			  "77777777-7777-7777-7777-777777777777": {
			   "file": "delta-rows.sql", "sha256": "7777777777777777", "text": "Every concept delta row",
			   "keywords": "release-types", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'delta row' from rvfph_prospective_.concept_d"]},
			  "88888888-8888-8888-8888-888888888888": {
			   "file": "full-rows.sql", "sha256": "8888888888888888", "text": "Every concept full row",
			   "keywords": "release-types", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'full row' from rvfph_prospective_.concept_f"]},
			  "99999999-9999-9999-9999-999999999999": {
			   "file": "partly-needs-previous.sql", "sha256": "9999999999999999", "text": "Inactive concepts, and concepts new since the previous release",
			   "keywords": "partial", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', id, 'inactive' from rvfph_prospective_.concept_s where active = '0'",
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', p.id, 'new' from rvfph_prospective_.concept_s p where not exists (select 1 from rvfph_previous_.concept_s v where v.id = p.id)"]},
			  "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb": {
			   "file": "reads-a-file.sql", "sha256": "bbbbbbbbbbbbbbbb", "text": "Reads a file outside the run",
			   "keywords": "escape", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details) select 424242424242424242, 'rvfph_assertionuuid_', 0, content from read_text('READ_FILE')"]},
			  "cccccccc-cccc-cccc-cccc-cccccccccccc": {
			   "file": "writes-a-file.sql", "sha256": "cccccccccccccccc", "text": "Writes a file outside the run",
			   "keywords": "escape", "severity": "",
			   "statements": [
			    "COPY (SELECT id FROM rvfph_prospective_.concept_s) TO 'OUTSIDE_FILE'"]}
			 }
			}
			""";

	private static final String GROUPS_XML = """
			<assertionGroupingStrategy>
			 <group name="component-centric-validation" includeStandaloneCategories="component-centric-validation" />
			 <group name="dependency-comparison" includeStandaloneCategories="dependency-comparison" />
			 <group name="release-types" includeStandaloneCategories="release-types" />
			 <group name="partial" includeStandaloneCategories="partial" />
			 <group name="escape" includeStandaloneCategories="escape" />
			</assertionGroupingStrategy>
			""";

	private static final String POLICIES_XML = "<policyValues/>";

	private static final String CORPUS_REF = "0123456789abcdef0123456789abcdef01234567";

	@TempDir
	private Path root;

	private Path work;
	private Path prospective;
	private DuckStore store;
	private ValidationReportService reportService;
	private WhitelistService whitelistService;

	@BeforeEach
	void setUp() throws Exception {
		store = store(STORE, false);

		work = root.resolve("work");
		Files.createDirectories(work);

		// Two active concepts, one at an older effectiveTime, and two inactive.
		prospective = root.resolve("prospective");
		writeRf2(prospective, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260831.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260831	1	%1$s	900000000000074008
				2	20260831	0	%1$s	900000000000074008
				3	20260831	0	%1$s	900000000000074008
				4	20250131	1	%1$s	900000000000074008
				""".formatted(MODULE));

		reportService = mock(ValidationReportService.class);
		whitelistService = mock(WhitelistService.class);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);
	}

	/** The store with the grouping files a published store carries, and optionally a pack identity. */
	private static DuckStore store(String json, boolean withIdentity) throws Exception {
		ObjectNode tree = (ObjectNode) DuckStore.MAPPER.readTree(json);
		ArrayNode grouping = tree.putArray("grouping");
		grouping.addObject().put("file", "groups.xml").put("sha256", TestStores.sha(GROUPS_XML)).put("text", GROUPS_XML);
		grouping.addObject().put("file", "policies.xml").put("sha256", TestStores.sha(POLICIES_XML)).put("text", POLICIES_XML);
		if (withIdentity) {
			ObjectNode pack = tree.putObject("pack");
			pack.put("name", "test-pack").put("version", "2026.10.13").put("corpusRef", CORPUS_REF)
					.put("assertions", tree.get("assertions").size())
					.put("digest", DuckStore.parse(tree.toString()).assertionDigest());
		}
		return DuckStore.parse(tree.toString());
	}

	@Test
	void aFullRunReportsEveryAssertionWithItsOwnFailureCount() throws Exception {
		ValidationRunConfig runConfig = new ValidationRunConfig();
		runConfig.setRunId(7L);
		runConfig.setGroupsList(List.of("component-centric-validation"));
		runConfig.setStorageLocation("s3-not-reached/");
		runConfig.setLocalProspectiveFile(zip(prospective, root.resolve("prospective.zip")).toFile());
		ValidationStatusReport statusReport = statusReport(runConfig);

		// The config-driven entry point: acquisition's execution config, then unpacking the zip.
		service().runRF2Validations(runConfig, statusReport);

		assertEquals(List.of(), statusReport.getFailureMessages(),
				"a run that completes has nothing to report as a system failure");
		ValidationReport report = statusReport.getResultReport();

		// The ported macro resolved, so the two inactive concepts were found.
		assertEquals(2L, item(report, UUID_INACTIVE).getFailureCount());
		assertEquals(2, item(report, UUID_INACTIVE).getFirstNInstances().size());
		// Ran, found nothing - not the same as did not run.
		assertEquals(0L, item(report, UUID_CLEAN).getFailureCount());
		assertNull(item(report, UUID_CLEAN).getFailureMessage());
		// Reads the resource table built from a pre-requisite view, filtered on <VERSION>:
		// 1 of the 2 active concepts is at 20260831.
		assertEquals(1L, item(report, UUID_VERSION_BOUND).getFailureCount());
		// The resource assertion produces no findings and is reported.
		assertEquals(0L, item(report, UUID_RESOURCE).getFailureCount());

		assertEquals(5, report.getTotalTestsRun(), "every selected assertion is reported");
		assertTrue(report.getAssertionsPassed().stream().allMatch(i -> i.getTestType() == TestType.SQL),
				"the DuckDB engine still reports SQL assertions as SQL");
		assertEquals(1, statusReport.getTotalRF2FilesLoaded(),
				"the report names the RF2 files the release actually contributed");
		assertEquals(List.of(), report.getAssertionPacks(), "a store without an identity claims none");
		try (Stream<Path> left = Files.list(work)) {
			assertEquals(List.of(), left.toList(), "the unpacked release and the run database are removed");
		}
	}

	@Test
	void theReportRecordsTheIdentityOfTheStoreThatRan() throws Exception {
		store = store(STORE, true);
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(executionConfig(8L),
				DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		assertEquals(List.of(new ValidationReport.AssertionPackRecord("test-pack", "2026.10.13",
						store.assertionDigest(), CORPUS_REF, 12)),
				statusReport.getResultReport().getAssertionPacks());
	}

	@Test
	void progressIsWrittenPerPhaseAndPerTenAssertions() throws Exception {
		service().runValidations(executionConfig(9L),
				DuckDbValidationService.ReleaseDirectories.of(prospective), "storage/",
				statusReport(new ValidationRunConfig()));

		ArgumentCaptor<String> progress = ArgumentCaptor.forClass(String.class);
		verify(reportService, atLeastOnce()).writeProgress(progress.capture(), eq("storage/"));
		// MysqlValidationService's text, padding included: one resource assertion, then the group's four.
		assertEquals(List.of("Start executing assertions...", "[1] of [ 1] assertions are completed.",
				"Start executing assertions...", "[4] of [ 4] assertions are completed."), progress.getAllValues());
	}

	@Test
	void anAssertionNeedingAPreviousReleaseIsNotRunRatherThanPassed() throws Exception {
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(executionConfig(11L),
				DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		TestRunItem notRun = item(report, UUID_NEEDS_PREVIOUS);
		// -1, never 0: zero would read as "compared and found no differences".
		assertEquals(-1L, notRun.getFailureCount());
		assertNotNull(notRun.getFailureMessage());
		assertTrue(notRun.getFailureMessage().contains("<PREVIOUS>"), notRun.getFailureMessage());
		assertEquals(1, report.getTotalTestsIncomplete());
		assertFalse(report.getAssertionsPassed().stream()
						.anyMatch(i -> UUID_NEEDS_PREVIOUS.equals(String.valueOf(i.getAssertionUuid()))),
				"an assertion nothing executed must never appear in the passed list");
	}

	@Test
	void supplyingAPreviousReleaseRunsTheAssertionThatNeedsIt() throws Exception {
		// The previous release holds concepts 1 and 2, so 3 and 4 are new.
		Path previous = root.resolve("previous");
		writeRf2(previous, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20250131.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20250131	1	%1$s	900000000000074008
				2	20250131	1	%1$s	900000000000074008
				""".formatted(MODULE));
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(executionConfig(12L),
				new DuckDbValidationService.ReleaseDirectories(prospective, previous, null),
				null, statusReport);

		TestRunItem ran = item(statusReport.getResultReport(), UUID_NEEDS_PREVIOUS);
		assertNull(ran.getFailureMessage(), "the release it needed was supplied");
		assertEquals(2L, ran.getFailureCount(), "concepts 3 and 4 are not in the previous release");
		assertEquals(0, statusReport.getResultReport().getTotalTestsIncomplete());
	}

	@Test
	void aDeltaOnlyRunRebuildsTheSnapshotFromThePreviousReleaseAndTheDelta() throws Exception {
		Path previous = root.resolve("previous");
		writeRf2(previous, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20250131.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20250131	1	%1$s	900000000000074008
				2	20250131	1	%1$s	900000000000074008
				3	20250131	0	%1$s	900000000000074008
				4	20250131	0	%1$s	900000000000074008
				""".formatted(MODULE));
		Path delta = root.resolve("delta");
		writeRf2(delta, "Delta/Terminology/sct2_Concept_Delta_INT_20260831.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				2	20260831	0	%1$s	900000000000074008
				3	20260831	1	%1$s	900000000000074008
				5	20260831	0	%1$s	900000000000074008
				""".formatted(MODULE));
		MysqlExecutionConfig config = executionConfig(16L);
		config.setRf2DeltaOnly(true);
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, new DuckDbValidationService.ReleaseDirectories(delta, previous, null),
				null, statusReport);

		assertEquals(List.of(), statusReport.getFailureMessages());
		// Rebuilt: 1 active, 2 inactive (delta), 3 active (delta), 4 inactive, 5 inactive (new).
		// Without the delete of superseded rows this is 4; from the delta or previous alone, 2.
		assertEquals(3L, item(statusReport.getResultReport(), UUID_INACTIVE).getFailureCount());
	}

	@Test
	void withNoDependencyAnAntiJoinRunsAgainstEmptyTablesAndAComparisonIsNotRun() throws Exception {
		MysqlExecutionConfig config = executionConfig(17L);
		config.setGroupNames(List.of("dependency-comparison"));
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective),
				null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		assertEquals(List.of(), statusReport.getFailureMessages());
		// An anti-join gets the same answer from an empty dependency as from an absent one.
		TestRunItem antiJoin = item(report, UUID_NOT_IN_DEPENDENCY);
		assertNull(antiJoin.getFailureMessage());
		assertEquals(4L, antiJoin.getFailureCount());
		// A comparison against nothing would be a false pass.
		TestRunItem comparison = item(report, UUID_COMPARES_DEPENDENCY);
		assertEquals(-1L, comparison.getFailureCount());
		assertTrue(comparison.getFailureMessage().contains("<DEPENDENCY>"), comparison.getFailureMessage());
	}

	@Test
	void aFailedSetupStatementAbandonsTheRunInsteadOfReportingResults() throws Exception {
		// Everything downstream of a failed pre-requisite would still produce plausible counts.
		store = store(STORE.replace(GOOD_PREREQUISITE,
				"CREATE OR REPLACE VIEW concept_active AS SELECT * FROM no_such_table"), false);
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(executionConfig(13L),
				DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		assertEquals(1, statusReport.getFailureMessages().size());
		assertTrue(statusReport.getFailureMessages().get(0).contains("setup failed"),
				statusReport.getFailureMessages().get(0));
		assertTrue(statusReport.getReportSummary().containsKey(TestType.SQL.name()));
		assertEquals(0, statusReport.getResultReport().getTotalTestsRun(),
				"no assertion result may be reported from a half-built schema");
	}

	@Test
	void theRunsDatabaseFileIsDeletedWhenItFinishes() throws Exception {
		service().runValidations(executionConfig(14L),
				DuckDbValidationService.ReleaseDirectories.of(prospective), null,
				statusReport(new ValidationRunConfig()));

		try (Stream<Path> files = Files.list(work)) {
			assertEquals(List.of(), files.map(p -> p.getFileName().toString()).sorted().toList(),
					"the per-run database, its write-ahead log and its spill directory all go");
		}
	}

	@Test
	void aMissingReleaseDirectoryIsReportedAsAFailedRunNotAnException() throws Exception {
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(executionConfig(15L),
				DuckDbValidationService.ReleaseDirectories.of(root.resolve("no-such-release")),
				null, statusReport);

		assertEquals(1, statusReport.getFailureMessages().size());
		assertTrue(statusReport.getFailureMessages().get(0).contains("Prospective Release"),
				statusReport.getFailureMessages().get(0));
		assertEquals(0, statusReport.getResultReport().getTotalTestsRun());
	}

	@Test
	void severalDependenciesAreValidatedAgainstTheirUnion() throws Exception {
		// Concepts 1 and 2 are in one dependency, 3 in the other; 4 is in neither.
		Path first = root.resolve("dependency-a");
		writeRf2(first, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260801.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260801	1	%1$s	900000000000074008
				2	20260801	1	%1$s	900000000000074008
				""".formatted(MODULE));
		Path second = root.resolve("dependency-b");
		writeRf2(second, "Snapshot/Terminology/sct2_Concept_Snapshot_XX_20260801.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				3	20260801	1	%1$s	900000000000074008
				""".formatted(MODULE));
		File firstZip = zip(first, root.resolve("dependency-a.zip")).toFile();
		File secondZip = zip(second, root.resolve("dependency-b.zip")).toFile();

		ValidationRunConfig runConfig = new ValidationRunConfig();
		runConfig.setRunId(18L);
		runConfig.setGroupsList(List.of("dependency-comparison"));
		runConfig.setReleaseAsAnEdition(true);
		runConfig.setLocalProspectiveFile(zip(prospective, root.resolve("prospective.zip")).toFile());
		runConfig.addExtensionDependency(firstZip.getName()).addExtensionDependency(secondZip.getName());
		runConfig.addLocalReleaseFile(firstZip);
		runConfig.addLocalReleaseFile(secondZip);
		ValidationStatusReport statusReport = statusReport(runConfig);

		service().runRF2Validations(runConfig, statusReport);

		assertEquals(List.of(), statusReport.getFailureMessages());
		ValidationReport report = statusReport.getResultReport();
		// 1 with the union; 3 with the second dependency alone, 2 with the first alone.
		assertEquals(1L, item(report, UUID_NOT_IN_DEPENDENCY).getFailureCount(),
				"only concept 4 is in neither dependency");
		// 2 and 3 are inactive here and active in a dependency, one in each.
		assertEquals(2L, item(report, UUID_COMPARES_DEPENDENCY).getFailureCount());
	}

	@Test
	void aDeltaOnlyRunWithNoPreviousReleaseValidatesTheDeltaAsTheSnapshot() throws Exception {
		Path delta = root.resolve("delta");
		writeRf2(delta, "Delta/Terminology/sct2_Concept_Delta_INT_20260831.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				2	20260831	0	%1$s	900000000000074008
				3	20260831	1	%1$s	900000000000074008
				5	20260831	0	%1$s	900000000000074008
				""".formatted(MODULE));
		MysqlExecutionConfig config = executionConfig(19L);
		config.setRf2DeltaOnly(true);
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(delta), null, statusReport);

		assertEquals(List.of(), statusReport.getFailureMessages());
		// MySQL copies the snapshot from an empty schema and applies the delta: 2 and 5.
		// An empty snapshot would report 0, a clean pass.
		assertEquals(2L, item(statusReport.getResultReport(), UUID_INACTIVE).getFailureCount());
	}

	@Test
	void aSnapshotOnlyFirstTimeReleaseDerivesItsDeltaAndFullTables() throws Exception {
		MysqlExecutionConfig config = executionConfig(20L);
		config.setGroupNames(List.of("release-types"));
		config.setFirstTimeRelease(true);
		config.setEffectiveTime("2026-08-31");
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		assertEquals(List.of(), statusReport.getFailureMessages());
		// The snapshot rows at this release's effective time: 1, 2 and 3, not 4 (20250131).
		assertEquals(3L, item(report, UUID_DELTA_ROWS).getFailureCount());
		assertEquals(4L, item(report, UUID_FULL_ROWS).getFailureCount(), "the full table is the snapshot");
	}

	@Test
	void aSnapshotOnlySubsequentReleaseDerivesItsTablesInMysqlsOrder() throws Exception {
		MysqlExecutionConfig config = executionConfig(21L);
		config.setGroupNames(List.of("release-types"));
		config.setPreviousEffectiveTime("20250131");
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		assertEquals(List.of(), statusReport.getFailureMessages());
		// ReleaseDataManager derives the delta from the full table before it derives the full
		// table from the snapshot, so a package with neither has an empty delta.
		assertEquals(0L, item(report, UUID_DELTA_ROWS).getFailureCount());
		assertEquals(4L, item(report, UUID_FULL_ROWS).getFailureCount());
	}

	@Test
	void aPackageWithoutFullFilesDerivesItsFullTablesAndKeepsItsDelta() throws Exception {
		writeRf2(prospective, "Delta/Terminology/sct2_Concept_Delta_INT_20260831.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260831	1	%1$s	900000000000074008
				2	20260831	0	%1$s	900000000000074008
				""".formatted(MODULE));
		MysqlExecutionConfig config = executionConfig(22L);
		config.setGroupNames(List.of("release-types"));
		config.setPreviousEffectiveTime("20250131");
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		assertEquals(List.of(), statusReport.getFailureMessages());
		assertEquals(2L, item(report, UUID_DELTA_ROWS).getFailureCount(), "the package's own delta, untouched");
		assertEquals(4L, item(report, UUID_FULL_ROWS).getFailureCount());
	}

	@Test
	void aFirstTimeReleaseComparesWithAnEmptyPreviousReleaseAsMysqlDoes() throws Exception {
		MysqlExecutionConfig config = executionConfig(23L);
		config.setFirstTimeRelease(true);
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		TestRunItem compared = item(statusReport.getResultReport(), UUID_NEEDS_PREVIOUS);
		assertNull(compared.getFailureMessage(), "a first-time release has nothing before it, which is an answer");
		assertEquals(4L, compared.getFailureCount(), "every concept is new");
	}

	@Test
	void aPreviousReleaseNamedAsTheEmptyReleaseIsAnEmptyPreviousRelease() throws Exception {
		ValidationRunConfig runConfig = new ValidationRunConfig();
		runConfig.setRunId(24L);
		runConfig.setGroupsList(List.of("component-centric-validation"));
		runConfig.setPreviousRelease(EMPTY_RELEASE);
		runConfig.setLocalProspectiveFile(zip(prospective, root.resolve("prospective.zip")).toFile());
		ValidationStatusReport statusReport = statusReport(runConfig);

		service().runRF2Validations(runConfig, statusReport);

		TestRunItem compared = item(statusReport.getResultReport(), UUID_NEEDS_PREVIOUS);
		assertNull(compared.getFailureMessage());
		assertEquals(4L, compared.getFailureCount());
	}

	@Test
	void anAssertionWithSomeStatementsSkippedCountsTheRestAndSaysSo() throws Exception {
		MysqlExecutionConfig config = executionConfig(25L);
		config.setGroupNames(List.of("partial"));
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		TestRunItem partly = item(statusReport.getResultReport(), UUID_PARTLY_NEEDS_PREVIOUS);
		assertNull(partly.getFailureMessage(), "what ran is a result, not a failure to run");
		assertEquals(2L, partly.getFailureCount(), "the two inactive concepts");
		assertNotNull(partly.getNote());
		assertTrue(partly.getNote().startsWith("Partly run: 1 of 2 statement(s) skipped"), partly.getNote());
		assertTrue(partly.getNote().contains("<PREVIOUS>"), partly.getNote());
		assertEquals(0, statusReport.getResultReport().getTotalTestsIncomplete());
	}

	@Test
	void anAssertionCannotReadOrWriteFilesOutsideTheRun() throws Exception {
		Path outside = root.resolve("outside").resolve("leak.csv");
		Files.createDirectories(outside.getParent());
		store = store(STORE.replace("READ_FILE", "/etc/hostname").replace("OUTSIDE_FILE", outside.toString()), false);
		MysqlExecutionConfig config = executionConfig(26L);
		config.setGroupNames(List.of("escape"));
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service().runValidations(config, DuckDbValidationService.ReleaseDirectories.of(prospective), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		for (String uuid : List.of(UUID_READS_A_FILE, UUID_WRITES_A_FILE)) {
			TestRunItem refused = item(report, uuid);
			assertEquals(-1L, refused.getFailureCount());
			assertTrue(refused.getFailureMessage().contains("Permission Error"), refused.getFailureMessage());
		}
		assertFalse(Files.exists(outside), "nothing may be written outside the run's directories");
	}

	@Test
	void anAssertionCannotReadAnotherRunsFilesOrWriteIntoTheReleaseCache() throws Exception {
		// Both live in the work directory: another run's unpacked release, and the cache a
		// later run would attach as its previous release.
		Path otherRun = work.resolve("rvf-999");
		Files.createDirectories(otherRun);
		Path secret = otherRun.resolve("secret.txt");
		Files.writeString(secret, "another run's release");
		Path cache = work.resolve("release-cache");
		Path planted = cache.resolve("0123456789abcdef.duckdb");
		store = store(STORE.replace("READ_FILE", secret.toString()).replace("OUTSIDE_FILE", planted.toString()), false);
		Path previous = root.resolve("previous");
		writeRf2(previous, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20250131.txt",
				"""
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20250131	1	%1$s	900000000000074008
				2	20250131	1	%1$s	900000000000074008
				""".formatted(MODULE));
		MysqlExecutionConfig config = executionConfig(28L);
		config.setGroupNames(List.of("component-centric-validation", "escape"));
		ValidationStatusReport statusReport = statusReport(new ValidationRunConfig());

		service("", cache, 1).runValidations(config,
				new DuckDbValidationService.ReleaseDirectories(prospective, previous, null), null, statusReport);

		ValidationReport report = statusReport.getResultReport();
		for (String uuid : List.of(UUID_READS_A_FILE, UUID_WRITES_A_FILE)) {
			TestRunItem refused = item(report, uuid);
			assertEquals(-1L, refused.getFailureCount());
			assertTrue(refused.getFailureMessage().contains("Permission Error"), refused.getFailureMessage());
		}
		assertFalse(Files.exists(planted), "nothing may be written into the release cache");
		// The previous release came from the cache, attached before the lockdown.
		try (Stream<Path> files = Files.list(cache)) {
			assertEquals(1, files.filter(p -> p.toString().endsWith(".duckdb")).count());
		}
		TestRunItem ran = item(report, UUID_NEEDS_PREVIOUS);
		assertNull(ran.getFailureMessage(), ran.getFailureMessage());
		assertEquals(2L, ran.getFailureCount(), "the cached previous release is still readable");
	}

	/**
	 * DuckDB rejects a bare number only when a run opens its connection, after the release
	 * has been unpacked and loaded; the service rejects it at startup instead.
	 */
	@Test
	void aMemoryLimitWithoutAUnitIsRejectedAtStartup() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> service("4"));
		assertTrue(e.getMessage().contains("must carry a unit"), e.getMessage());
		assertTrue(e.getMessage().contains("4GB"), "the message has to show the fix");
	}

	@Test
	void unitsDuckDbAcceptsArePassedThrough() {
		for (String ok : new String[] {"4GB", "4GiB", "512MB", "1TB", "2.5GB", " 8GB "}) {
			assertNotNull(service(ok), "should accept " + ok);
		}
	}

	@Test
	void anAbsentMemoryLimitKeepsDuckDbsOwnHeuristic() {
		assertNotNull(service(""));
		assertNotNull(service(null));
	}

	private DuckDbValidationService service() {
		return service("");
	}

	private DuckDbValidationService service(String memoryLimit) {
		return service(memoryLimit, null, 0);
	}

	private DuckDbValidationService service(String memoryLimit, Path cacheDirectory, double cacheMaxGb) {
		DuckAssertionService assertionService;
		try {
			assertionService = new DuckAssertionService(
					new DuckAssertionService.Corpus(store, DuckAssertionSource.from(store), List.of()));
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
		return new DuckDbValidationService(reportService, whitelistService, new ReleaseAcquisitionService(),
				assertionService, work.toString(), EMPTY_RELEASE, "qa_result", 0, memoryLimit,
				cacheDirectory == null ? "" : cacheDirectory.toString(), cacheMaxGb, 1000, 1000);
	}

	private static MysqlExecutionConfig executionConfig(long runId) {
		MysqlExecutionConfig config = new MysqlExecutionConfig(runId);
		config.setGroupNames(List.of("component-centric-validation"));
		config.setIncludedModules(List.of());
		config.setFailureExportMax(10);
		return config;
	}

	private static ValidationStatusReport statusReport(ValidationRunConfig runConfig) {
		ValidationStatusReport statusReport = new ValidationStatusReport(runConfig);
		statusReport.setResultReport(new ValidationReport());
		return statusReport;
	}

	/** The item for this assertion, wherever in the report it ended up. */
	private static TestRunItem item(ValidationReport report, String uuid) {
		UUID wanted = UUID.fromString(uuid);
		return Stream.of(report.getAssertionsPassed(), report.getAssertionsFailed(),
						report.getAssertionsWarning(), report.getAssertionsSkipped())
				.flatMap(List::stream)
				.filter(i -> wanted.equals(i.getAssertionUuid()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no report item for assertion " + uuid));
	}

	private static void writeRf2(Path releaseDir, String relative, String content) throws Exception {
		Path file = releaseDir.resolve(relative);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private static Path zip(Path dir, Path zipFile) throws Exception {
		try (OutputStream out = Files.newOutputStream(zipFile); ZipOutputStream zip = new ZipOutputStream(out);
				Stream<Path> files = Files.walk(dir)) {
			for (Path file : files.filter(Files::isRegularFile).toList()) {
				zip.putNextEntry(new ZipEntry(dir.relativize(file).toString()));
				Files.copy(file, zip);
				zip.closeEntry();
			}
		}
		return zipFile;
	}
}
