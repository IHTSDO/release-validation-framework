package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.TestRunItem;
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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Extension validation: the two-phase split and the dependency combine.
 *
 * <p>The fixture is arranged so that every count distinguishes one specific way
 * of getting the merge wrong, and no count is reachable by two routes:
 *
 * <ul>
 * <li><b>Which schema each phase ran against.</b> {@code newComponents} carries
 *     the {@code release-type-validation} keyword and compares the prospective
 *     against the previous release. Against the extension it finds 2; against
 *     the combined schema it would also find the two international concepts and
 *     report 4. {@code orphanDescriptions} carries a different keyword and finds
 *     0 against the combined schema but 2 against the extension alone. One
 *     number each way, so a run that routed either phase to the wrong schema
 *     cannot produce both.
 * <li><b>The merge rule.</b> Concept 1 is newer in the extension, 5 is newer in
 *     the dependency, 7 is a TIE, 2 is extension-only and 100/101 are
 *     dependency-only. Counting the survivors by module gives 2 extension rows
 *     and 4 dependency rows - a single pair of numbers that pins all five cases,
 *     including MySQL's {@code >=}/{@code >} asymmetry that hands ties to the
 *     dependency.
 * </ul>
 */
class DuckDbValidationServiceExtensionTest {

	private static final String UUID_NEW_COMPONENTS = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_ORPHANS = "22222222-2222-2222-2222-222222222222";
	private static final String UUID_EXT_MODULE = "33333333-3333-3333-3333-333333333333";
	private static final String UUID_DEP_MODULE = "44444444-4444-4444-4444-444444444444";
	private static final String UUID_RESOURCE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private static final String UUID_RESOURCE_ROWS = "66666666-6666-6666-6666-666666666666";

	private static final String EXT_MODULE = "1000001000000105";
	private static final String DEP_MODULE = "900000000000207008";

	/** {@code rvf.empty-release-file}'s default. */
	private static final String EMPTY_RELEASE = "empty-rf2-snapshot.zip";

	private static final String STORE = """
			{
			 "formatVersion": 1,
			 "generator": {"sqlglot": "30.18.0", "publisher": "sha256:test"},
			 "sentinels": [
			  {"placeholder": "<RUNID>", "sentinel": "424242424242424242"},
			  {"placeholder": "<ASSERTIONUUID>", "sentinel": "rvfph_assertionuuid_"},
			  {"placeholder": "<PROSPECTIVE>", "sentinel": "rvfph_prospective_"},
			  {"placeholder": "<PREVIOUS>", "sentinel": "rvfph_previous_"},
			  {"placeholder": "<DEPENDENCY>", "sentinel": "rvfph_dependency_"}
			 ],
			 "tableColumns": {
			  "concept_s": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, definitionstatusid BIGINT",
			  "concept_d": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, definitionstatusid BIGINT",
			  "description_s": "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, conceptid BIGINT, languagecode VARCHAR, typeid BIGINT, term VARCHAR, casesignificanceid BIGINT"
			 },
			 "prerequisites": [],
			 "ports": [],
			 "assertions": {
			  "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa": {
			   "file": "resource.sql", "text": "Build the shared seed table",
			   "keywords": "resource", "severity": "",
			   "statements": [
			    "CREATE OR REPLACE TABLE rvfph_prospective_.res_seed AS SELECT id, moduleid FROM rvfph_prospective_.concept_s"]},
			  "66666666-6666-6666-6666-666666666666": {
			   "file": "resource-rows.sql", "text": "Rows in the shared seed table",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'seed', id, 'res_seed' from rvfph_prospective_.res_seed"]},
			  "11111111-1111-1111-1111-111111111111": {
			   "file": "new-components.sql", "text": "Concepts new since the previous release",
			   "keywords": "release-type-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', p.id, 'new', p.id, 'concept_s' from rvfph_prospective_.res_seed p where not exists (select 1 from rvfph_previous_.concept_s v where v.id = p.id)"]},
			  "22222222-2222-2222-2222-222222222222": {
			   "file": "orphans.sql", "text": "Descriptions whose concept is missing",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', d.conceptid, 'orphan', d.id, 'description_s' from rvfph_prospective_.description_s d where not exists (select 1 from rvfph_prospective_.concept_s c where c.id = d.conceptid)"]},
			  "33333333-3333-3333-3333-333333333333": {
			   "file": "ext-module.sql", "text": "Surviving concepts in the extension module",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'ext', id, 'concept_s' from rvfph_prospective_.concept_s where moduleid = 1000001000000105"]},
			  "44444444-4444-4444-4444-444444444444": {
			   "file": "dep-module.sql", "text": "Surviving concepts in the dependency module",
			   "keywords": "component-centric-validation", "severity": "",
			   "statements": [
			    "insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) select 424242424242424242, 'rvfph_assertionuuid_', id, 'dep', id, 'concept_s' from rvfph_prospective_.concept_s where moduleid = 900000000000207008"]}
			 }
			}
			""";

	private static final String GROUPS_XML = """
			<assertionGroupingStrategy>
			 <group name="extension" includeStandaloneCategories="release-type-validation,component-centric-validation" />
			</assertionGroupingStrategy>
			""";

	@TempDir
	private Path root;

	private DuckStore store;
	private Path work;
	private Path prospective;
	private Path previous;
	private Path dependency;
	private ValidationReportService reportService;
	private WhitelistService whitelistService;

	@BeforeEach
	void setUp() throws Exception {
		ObjectNode tree = (ObjectNode) DuckStore.MAPPER.readTree(STORE);
		ArrayNode grouping = tree.putArray("grouping");
		grouping.addObject().put("file", "groups.xml").put("sha256", TestStores.sha(GROUPS_XML)).put("text", GROUPS_XML);
		grouping.addObject().put("file", "policies.xml").put("sha256", TestStores.sha(TestStores.POLICIES_XML))
				.put("text", TestStores.POLICIES_XML);
		store = DuckStore.parse(tree.toString());

		work = root.resolve("work");
		Files.createDirectories(work);

		prospective = root.resolve("prospective");
		writeRf2(prospective, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260831	1	%s	900000000000074008
				2	20260831	1	%s	900000000000074008
				5	20260101	1	%s	900000000000074008
				7	20260831	1	%s	900000000000074008
				""".formatted(EXT_MODULE, EXT_MODULE, EXT_MODULE, EXT_MODULE));
		// 901 and 902 hang off dependency concepts: orphans until the combine.
		writeRf2(prospective, "Snapshot/Terminology/sct2_Description_Snapshot-en_XX1000000_20260831.txt", """
				id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
				900	20260831	1	%s	1	en	900000000000013009	Local term	900000000000448009
				901	20260831	1	%s	100	en	900000000000013009	Term on intl concept	900000000000448009
				902	20260831	1	%s	101	en	900000000000013009	Another intl term	900000000000448009
				""".formatted(EXT_MODULE, EXT_MODULE, EXT_MODULE));
		previous = root.resolve("previous");
		writeRf2(previous, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260731.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260731	1	%s	900000000000074008
				5	20260101	1	%s	900000000000074008
				""".formatted(EXT_MODULE, EXT_MODULE));

		dependency = root.resolve("dependency");
		writeRf2(dependency, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260801.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260101	1	%s	900000000000074008
				5	20260831	1	%s	900000000000074008
				7	20260831	1	%s	900000000000074008
				100	20260801	1	%s	900000000000074008
				101	20260801	1	%s	900000000000074008
				""".formatted(DEP_MODULE, DEP_MODULE, DEP_MODULE, DEP_MODULE, DEP_MODULE));
		reportService = mock(ValidationReportService.class);
		whitelistService = mock(WhitelistService.class);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);
	}

	@Test
	void releaseTypeAssertionsRunAgainstTheExtensionAndTheRestAgainstTheCombinedSchema() {
		ValidationReport report = runExtension();

		// Concepts 2 and 7 are absent from the previous extension; against the combined
		// schema the two dependency concepts would count as new too, giving 4.
		assertEquals(2L, item(report, UUID_NEW_COMPONENTS).getFailureCount(),
				"release-type assertions see the extension alone");

		// Both descriptions on dependency concepts resolve only against the combined snapshot.
		assertEquals(0L, item(report, UUID_ORPHANS).getFailureCount(),
				"non-release-type assertions see the extension merged with its dependency");
	}

	/**
	 * Resource assertions build tables in the schema a phase addresses, so the split runs
	 * them once per phase, as MysqlValidationService does. {@code newComponents} reads
	 * {@code res_seed}, so a phase that skipped them reports a failure message, not a count.
	 */
	@Test
	void resourceAssertionsRunBeforeEachPhaseAndInThatPhasesSchema() {
		ValidationReport report = runExtension();

		// Phase 1: res_seed existed in the prospective schema.
		assertNull(item(report, UUID_NEW_COMPONENTS).getFailureMessage(),
				"the release-type phase ran before its resource tables were built");
		assertEquals(2L, item(report, UUID_NEW_COMPONENTS).getFailureCount());

		// Phase 2: res_seed was rebuilt in the combined schema: six concepts, not four.
		assertEquals(6L, item(report, UUID_RESOURCE_ROWS).getFailureCount(),
				"the combined phase must rebuild its own resource tables");
	}

	@Test
	void theMergeKeepsTheLatestRowPerComponentAndGivesTiesToTheDependency() {
		ValidationReport report = runExtension();

		// 1 (extension newer) and 2 (extension only).
		assertEquals(2L, item(report, UUID_EXT_MODULE).getFailureCount());
		// 5 (dependency newer), 7 (tie, which MySQL gives to the dependency), 100 and 101.
		assertEquals(4L, item(report, UUID_DEP_MODULE).getFailureCount());
	}

	@Test
	void anExtensionRunWithNoDependencyReportsAFailureRatherThanResults() {
		ValidationStatusReport status = run(new DuckDbValidationService.ReleaseDirectories(
				prospective, previous, null));

		assertTrue(status.getFailureMessages().stream()
						.anyMatch(m -> m.contains("dependency")),
				"expected a dependency failure message, got " + status.getFailureMessages());
		// Not even the release-type assertions that ran before the combine: alone they
		// would read as a complete run's report.
		assertEquals(0, status.getResultReport().getTotalTestsRun(),
				"a run that could not combine must report no results at all");
	}

	@Test
	void aStandAloneProductSkipsTheCombineAndRunsEverythingAgainstTheExtension() {
		MysqlExecutionConfig config = extensionConfig();
		config.setStandAloneProduct(true);
		ValidationStatusReport status = run(config, new DuckDbValidationService.ReleaseDirectories(
				prospective, previous, dependency));

		ValidationReport report = status.getResultReport();
		assertEquals(List.of(), status.getFailureMessages());
		// No combine, so both descriptions on dependency concepts are orphans.
		assertEquals(2L, item(report, UUID_ORPHANS).getFailureCount());
		assertEquals(0L, item(report, UUID_DEP_MODULE).getFailureCount());
	}

	private ValidationReport runExtension() {
		ValidationStatusReport status = run(new DuckDbValidationService.ReleaseDirectories(
				prospective, previous, dependency));
		assertEquals(List.of(), status.getFailureMessages());
		return status.getResultReport();
	}

	private ValidationStatusReport run(DuckDbValidationService.ReleaseDirectories releases) {
		return run(extensionConfig(), releases);
	}

	private ValidationStatusReport run(MysqlExecutionConfig config,
			DuckDbValidationService.ReleaseDirectories releases) {
		// Built as ValidationRunner builds it: the no-arg constructor leaves collections null.
		ValidationRunConfig runConfig = new ValidationRunConfig();
		runConfig.setRunId(config.getExecutionId());
		ValidationStatusReport status = new ValidationStatusReport(runConfig);
		DuckAssertionService assertionService;
		try {
			assertionService = new DuckAssertionService(
					new DuckAssertionService.Corpus(store, DuckAssertionSource.from(store)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		new DuckDbValidationService(reportService, whitelistService, new ReleaseAcquisitionService(),
				assertionService, work.toString(), EMPTY_RELEASE, "qa_result", 0, "", "", 0, 1000, 1000)
				.runValidations(config, releases, "storage/", status);
		return status;
	}

	private MysqlExecutionConfig extensionConfig() {
		MysqlExecutionConfig config = new MysqlExecutionConfig(11L);
		config.setGroupNames(List.of("extension"));
		config.setExtensionValidation(true);
		config.setReleaseAsAnEdition(false);
		config.setFailureExportMax(10);
		return config;
	}

	private static TestRunItem item(ValidationReport report, String uuid) {
		UUID wanted = UUID.fromString(uuid);
		for (List<TestRunItem> bucket : List.of(report.getAssertionsFailed(),
				report.getAssertionsPassed(), report.getAssertionsWarning(),
				report.getAssertionsSkipped())) {
			for (TestRunItem candidate : bucket) {
				if (wanted.equals(candidate.getAssertionUuid())) {
					return candidate;
				}
			}
		}
		throw new AssertionError(uuid + " was not reported at all");
	}

	private static void writeRf2(Path releaseRoot, String relativePath, String content)
			throws Exception {
		Path file = releaseRoot.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}
}
