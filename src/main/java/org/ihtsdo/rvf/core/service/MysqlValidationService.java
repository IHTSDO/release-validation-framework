package org.ihtsdo.rvf.core.service;


import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ExceptionUtils;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class MysqlValidationService {

	public static final String START_EXECUTING_ASSERTIONS = "Start executing assertions...";
	@Autowired
	private AssertionService assertionService;

	@Autowired
	private AssertionExecutionService assertionExecutionService;

	@Value("${rvf.assertion.execution.BatchSize}")
	private int batchSize;

	@Autowired
	private ValidationReportService reportService;

	@Autowired
	private MysqlFailuresExtractor mysqlFailuresExtractor;

	@Autowired
	private ValidationVersionLoader releaseVersionLoader;

	@Autowired
	private ReleaseDataManager releaseDataManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(MysqlValidationService.class);

	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";

	private final Set<String> schemasToRemove = new HashSet<>();

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	// VAL-439: maps assertion UUID → MySQL full table name (from manifest.xml assertion SQL files).
	// Used by computeChecksumSkipUuids to decide which assertions can be skipped when their
	// underlying full table is unchanged between the previous and prospective schemas.
	private static final Map<String, String> ASSERTION_UUID_TO_FULL_TABLE = new LinkedHashMap<>();
	static {
		ASSERTION_UUID_TO_FULL_TABLE.put("84f5edda-1249-4d79-87da-e248e61f06a6", "concept_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("492bacf0-7d88-45ba-ab26-974150ed9541", "description_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("433adc93-fdd5-462a-b1fa-11fb4045a9ec", "textdefinition_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("fd8ed390-94e9-4fd9-9e20-902a24273dca", "stated_relationship_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("b46ec8df-1bfd-4825-be49-f09ed8a0076b", "relationship_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("7d105b20-ce21-49c2-b16d-b49df13fdfea", "relationship_concrete_values_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("3d4342fe-4698-4c6d-b519-d8d649c842e0", "owlexpressionrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("653ce147-780e-4b3d-8afe-bd8d116c8c17", "associationrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("1f6b5876-7714-4362-a065-3906d059a122", "attributevaluerefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("673cf38a-a913-4e0e-be32-356ac433ede8", "simplerefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("c19b9008-bbc9-4209-a3d7-896d414df565", "simplemaprefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("f0b4c712-781a-4ca5-872e-883cf1949f12", "langrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("5c37bee7-62ad-41f9-93e3-12eb4803e613", "extendedmaprefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("6c37bee7-62ad-41f9-93e3-12eb4803e618", "expressionassociationrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("7c37bee7-62ad-41f9-93e3-12eb4803e618", "mapcorrelationoriginrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("87af4df0-c506-41ed-9089-6a359f16b34f", "mrcmdomainrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("4fdf8c6a-8b58-4bf7-a8b5-71176ede7ac0", "mrcmattributedomainrefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("becfbeaa-5eab-4a4a-90ca-7c893b7495b2", "mrcmattributerangerefset_f");
		ASSERTION_UUID_TO_FULL_TABLE.put("0f7b6979-7a9d-45d4-956b-972d452f1638", "mrcmmodulescoperefset_f");
	}

	public ValidationStatusReport runRF2MysqlValidations(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws BusinessServiceException, ExecutionException, InterruptedException {
		// Clean up the prospective databases if any
		for (String legacyVersion : this.schemasToRemove) {
			this.releaseDataManager.dropSchema(legacyVersion);
		}
		this.schemasToRemove.clear();
		this.releaseDataManager.truncateQAResult();

		MysqlExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		String reportStorage = validationConfig.getStorageLocation();
		String lastItemLoadAttempted = "Item Unknown";
		try {
			// prepare release data for testing
			lastItemLoadAttempted = "Previous Release - " + executionConfig.getPreviousVersion();
			releaseVersionLoader.loadPreviousVersion(validationConfig.getPreviousRelease(), validationConfig.getReleaseFileToCreationTimeMap(), executionConfig);
			if (releaseVersionLoader.isUnknownVersion(executionConfig.getPreviousVersion())) {
				statusReport.addFailureMessage("Failed to load previous release " + executionConfig.getPreviousVersion());
			} else if (!CollectionUtils.isEmpty(executionConfig.getExcludedRF2Files())) { // Keep this schema if there is no changes from the package
				schemasToRemove.add(executionConfig.getPreviousVersion());
			}

			// load dependency release
			releaseVersionLoader.loadDependencyVersion(validationConfig.getExtensionDependencies(), validationConfig.getReleaseFileToCreationTimeMap(), executionConfig, schemasToRemove);
			lastItemLoadAttempted = "Dependency Release - " + executionConfig.getExtensionDependencyVersion();
			if (releaseVersionLoader.isUnknownVersion(executionConfig.getExtensionDependencyVersion())) {
				statusReport.addFailureMessage("Failed to load dependency release " + executionConfig.getExtensionDependencyVersion());
			}

			// load prospective version
			lastItemLoadAttempted = "Prospective Release - " + executionConfig.getProspectiveVersion();
			releaseVersionLoader.loadProspectiveVersion(validationConfig.getLocalProspectiveFile(), statusReport, executionConfig, validationConfig.getStorageLocation());
			if (releaseVersionLoader.isUnknownVersion(executionConfig.getProspectiveVersion())) {
				statusReport.addFailureMessage("Failed to load prospective release " + executionConfig.getProspectiveVersion());
			} else  {
				schemasToRemove.add(executionConfig.getProspectiveVersion());
			}
		} catch (Exception e) {
			String errorMsg = String.format("Failed to load data (%s) into MySql", lastItemLoadAttempted);
			String errorMsgWithCause = ExceptionUtils.getExceptionCause(errorMsg, e);
			LOGGER.error(errorMsgWithCause, e);
			statusReport.addFailureMessage(errorMsgWithCause);
			statusReport.getReportSummary().put(TestType.SQL.name(), errorMsgWithCause);
			return statusReport;
		}
		if (executionConfig.isExtensionValidation() && !executionConfig.isReleaseAsAnEdition()) {
			LOGGER.info("Run extension release validation with config {}", executionConfig);
			runExtensionReleaseValidation(statusReport, validationConfig, executionConfig);
		} else {
			LOGGER.info("Run international/edition release validation with config {}", executionConfig);
			runAssertionTests(statusReport, executionConfig, reportStorage);
		}
		return statusReport;
	}


	/** For extension release validation we need to test the release-type validations first using previous extension against current extension
	 * first then loading the international snapshot for the file-centric and component-centric validations.
	 * load previous published version
	 * @param statusReport status report
	 * @param validationConfig validation config
	 * @param executionConfig execution config
	 */
	private void runExtensionReleaseValidation(ValidationStatusReport statusReport, ValidationRunConfig validationConfig, MysqlExecutionConfig executionConfig) throws ExecutionException, InterruptedException {
		final long timeStart = System.currentTimeMillis();
		// run release-type validations
		List<Assertion> assertions = getAssertions(executionConfig.getGroupNames());
		LOGGER.debug("Total assertions found {}", assertions.size());
		List<Assertion> releaseTypeAssertions = new ArrayList<>();
		List<Assertion> noneReleaseTypeAssertions = new ArrayList<>();
		for (Assertion assertion : assertions) {
			if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				releaseTypeAssertions.add(assertion);
			} else {
				noneReleaseTypeAssertions.add(assertion);
			}
		}

		// VAL-439: skip full-validation assertions whose underlying MySQL full tables are unchanged
		Map<String, Long> prevChecksums = releaseDataManager.computeFullTableChecksums(executionConfig.getPreviousVersion());
		Map<String, Long> currChecksums = releaseDataManager.computeFullTableChecksums(executionConfig.getProspectiveVersion());
		List<String> skipUuids = computeChecksumSkipUuids(releaseTypeAssertions, prevChecksums, currChecksums);
		Set<String> skipSet = new HashSet<>(skipUuids);

		List<Assertion> releaseTypeToRun = new ArrayList<>();
		List<TestRunItem> checksumSkippedItems = new ArrayList<>();
		for (Assertion assertion : releaseTypeAssertions) {
			if (skipSet.contains(assertion.getUuid().toString())) {
				TestRunItem skipped = new TestRunItem();
				skipped.setAssertionUuid(assertion.getUuid());
				skipped.setAssertionText(assertion.getAssertionText());
				skipped.setTestCategory(assertion.getKeywords());
				skipped.setSeverity(assertion.getSeverity());
				skipped.setTestType(TestType.SQL);
				skipped.setFailureCount(0L);
				skipped.setRunTime(0L);
				checksumSkippedItems.add(skipped);
			} else {
				releaseTypeToRun.add(assertion);
			}
		}
		if (!checksumSkippedItems.isEmpty()) {
			LOGGER.info("Checksum skip: {} release-type-full-validation assertions skipped", checksumSkippedItems.size());
			statusReport.getResultReport().addSkippedAssertions(checksumSkippedItems);
		}

		LOGGER.debug("Running release-type validations {}", releaseTypeToRun.size());
		String reportStorage = validationConfig.getStorageLocation();
		List<TestRunItem> testItems = runAssertionTests(executionConfig, releaseTypeToRun, reportStorage);
		if (!executionConfig.isStandAloneProduct()) {
			//loading international snapshot
			try {
				releaseVersionLoader.combineCurrentExtensionWithDependencySnapshot(executionConfig);
				this.schemasToRemove.add(executionConfig.getProspectiveVersion());
			} catch (BusinessServiceException e) {
				String errMsg = ExceptionUtils.getExceptionCause("Failed to prepare data for extension testing", e);
				statusReport.addFailureMessage(errMsg);
				LOGGER.error(errMsg, e);
				statusReport.getReportSummary().put(TestType.SQL.name(), errMsg);
			}
		}
		testItems.addAll(runAssertionTests(executionConfig, noneReleaseTypeAssertions, reportStorage));
		constructTestReport(statusReport, executionConfig, timeStart, testItems, assertions);
	}

	/**
	 * VAL-439: Given a list of assertions and checksum maps for the previous and current schemas,
	 * returns the UUIDs of assertions that can safely be skipped because their underlying
	 * full table has an identical checksum in both schemas.
	 *
	 * @param assertions    candidate assertions to evaluate (typically release-type assertions)
	 * @param prevChecksums table → checksum for the previous release schema
	 * @param currChecksums table → checksum for the prospective/current release schema
	 * @return list of assertion UUID strings that can be skipped
	 */
	public List<String> computeChecksumSkipUuids(List<Assertion> assertions,
			Map<String, Long> prevChecksums, Map<String, Long> currChecksums) {
		List<String> skipUuids = new ArrayList<>();
		for (Assertion assertion : assertions) {
			String uuidStr = assertion.getUuid().toString();
			String table = ASSERTION_UUID_TO_FULL_TABLE.get(uuidStr);
			if (table == null) {
				continue; // not a full-validation assertion, must run
			}
			Long prevCk = prevChecksums.get(table);
			Long currCk = currChecksums.get(table);
			if (prevCk != null && currCk != null && prevCk.equals(currCk)) {
				LOGGER.info("Checksum skip: '{}' ({}) — table '{}' unchanged (checksum {})",
						assertion.getAssertionText(), uuidStr, table, currCk);
				skipUuids.add(uuidStr);
			}
		}
		LOGGER.info("Checksum skip: {} of {} assertions can be skipped", skipUuids.size(), assertions.size());
		return skipUuids;
	}

	/**
	 * VAL-439: Computes which assertions in the given list can be skipped due to unchanged full tables,
	 * registers the skipped items on the status report, and returns them so callers can exclude them
	 * from execution.
	 */
	private List<TestRunItem> applyChecksumSkip(ValidationStatusReport statusReport,
			MysqlExecutionConfig executionConfig, List<Assertion> assertions) {
		Map<String, Long> prevChecksums = releaseDataManager.computeFullTableChecksums(executionConfig.getPreviousVersion());
		Map<String, Long> currChecksums = releaseDataManager.computeFullTableChecksums(executionConfig.getProspectiveVersion());
		List<String> skipUuids = computeChecksumSkipUuids(assertions, prevChecksums, currChecksums);
		Set<String> skipSet = new HashSet<>(skipUuids);
		List<TestRunItem> skippedItems = new ArrayList<>();
		for (Assertion assertion : assertions) {
			if (skipSet.contains(assertion.getUuid().toString())) {
				TestRunItem skipped = new TestRunItem();
				skipped.setAssertionUuid(assertion.getUuid());
				skipped.setAssertionText(assertion.getAssertionText());
				skipped.setTestCategory(assertion.getKeywords());
				skipped.setSeverity(assertion.getSeverity());
				skipped.setTestType(TestType.SQL);
				skipped.setFailureCount(0L);
				skipped.setRunTime(0L);
				skippedItems.add(skipped);
			}
		}
		if (!skippedItems.isEmpty()) {
			LOGGER.info("Checksum skip: {} assertions skipped (full tables unchanged)", skippedItems.size());
			statusReport.getResultReport().addSkippedAssertions(skippedItems);
		}
		return skippedItems;
	}

	private List<Assertion> getAssertions(List<String> groupNames) {
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(groupNames);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			assertions.addAll(group.getAssertions());
		}
		return new ArrayList<>(assertions);
	}

	private List<TestRunItem> runAssertionTests(MysqlExecutionConfig executionConfig, List<Assertion> assertions,
			String reportStorage) throws ExecutionException, InterruptedException {
        final List<Assertion> resourceAssertions = assertionService.getAssertionsByKeyWords("resource", true);
		LOGGER.info("Found total resource assertions need to be run before test {}", resourceAssertions.size());
		reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
        List<TestRunItem> result = new ArrayList<>(executeAssertions(executionConfig, resourceAssertions, reportStorage));

		reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
		LOGGER.info("Total assertions to run {}", assertions.size());
		if (batchSize == 0) {
			result.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			result.addAll(executeAssertionsConcurrently(executionConfig,assertions, batchSize, reportStorage));
		}
		return result;
	}

	private void runAssertionTests(ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, String reportStorage) throws ExecutionException, InterruptedException {
		long timeStart = System.currentTimeMillis();
		List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(executionConfig.getGroupNames());
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		List<Assertion> resourceAssertions = assertionService.getAssertionsByKeyWords("resource", true);
		LOGGER.info("Found total resource assertions need to be run before test {}", resourceAssertions.size());
		reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
		List<TestRunItem> items = executeAssertions(executionConfig, resourceAssertions, reportStorage);
		Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			assertions.addAll(group.getAssertions());
		}

		// VAL-439: skip full-validation assertions whose underlying MySQL full tables are unchanged
		List<TestRunItem> skippedItems = applyChecksumSkip(statusReport, executionConfig, new ArrayList<>(assertions));
		List<Assertion> assertionsToRun = assertions.stream()
				.filter(a -> skippedItems.stream().noneMatch(s -> s.getAssertionUuid().equals(a.getUuid())))
				.toList();

		LOGGER.info("Total assertions to run {}", assertionsToRun.size());
		if (batchSize == 0) {
			items.addAll(executeAssertions(executionConfig, assertionsToRun, reportStorage));
		} else {
			items.addAll(executeAssertionsConcurrently(executionConfig, assertionsToRun, batchSize, reportStorage));
		}
		constructTestReport(statusReport, executionConfig, timeStart, items, new ArrayList<>(assertions));
	}

	private List<TestRunItem> executeAssertionsConcurrently(MysqlExecutionConfig executionConfig, Collection<Assertion> assertions,
			int batchSize, String reportStorage) throws ExecutionException, InterruptedException {
		List<Future<Collection<TestRunItem>>> tasks = new ArrayList<>();
		List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			UUID assertionUUID = assertion.getUuid();
			if (executionConfig.getAssertionExclusionList() != null && executionConfig.getAssertionExclusionList().contains(assertionUUID.toString())) {
				continue;
			}
			if (batch == null) {
				batch = new ArrayList<>();
			}
			batch.add(assertion);
			if (counter % batchSize == 0 || counter == assertions.size()) {
				final List<Assertion> work = batch;
				LOGGER.info("Started executing assertion {} of {}", counter, assertions.size());
				final Future<Collection<TestRunItem>> future = executorService.submit(() -> assertionExecutionService.executeAssertions(work, executionConfig));
				LOGGER.info("Finished executing assertion {} of {}", counter, assertions.size());
				// reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are started.", counter, assertions.size()), reportStorage);
				tasks.add(future);
				batch = null;
			}
			counter++;
		}

		// Wait for all concurrent tasks to finish
		for (Future<Collection<TestRunItem>> task : tasks) {
			results.addAll(task.get());
		}
		return results;
	}

	private List<TestRunItem> executeAssertions(MysqlExecutionConfig executionConfig, Collection<Assertion> assertions, String reportStorage) {

		List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		for (Assertion assertion: assertions) {
			UUID assertionUUID = assertion.getUuid();
			if (executionConfig.getAssertionExclusionList() != null && executionConfig.getAssertionExclusionList().contains(assertionUUID.toString())) {
				continue;
			}

			LOGGER.info("Started executing assertion {} of {} with uuid : {}", counter, assertions.size(), assertionUUID);
			results.addAll(assertionExecutionService.executeAssertion(assertion, executionConfig));
			LOGGER.info("Finished executing assertion {} of {} with uuid : {}", counter, assertions.size(), assertion.getUuid());
			counter++;
			if (counter % 10 == 0) {
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()), reportStorage);
			}
		}
		reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()), reportStorage);
		return results;
	}

	private void constructTestReport(ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig,
									 long timeStart, List<TestRunItem> items, List<Assertion> assertions) {
		ValidationReport report = statusReport.getResultReport();
		try {
			mysqlFailuresExtractor.extractTestResults(items, executionConfig, assertions);

			// failed tests
			final List<TestRunItem> failedItems = new ArrayList<>();
			final List<TestRunItem> warningItems = new ArrayList<>();
			final List<TestRunItem> incompleteItems = new ArrayList<>();
			for (final TestRunItem item : items) {
				item.setTestType(TestType.SQL);
				if (item.getFailureCount() != 0) {
					if (item.getFailureCount() == -1L) {
						incompleteItems.add(item);
					}
					if (SeverityLevel.WARN.toString().equalsIgnoreCase(item.getSeverity())) {
						warningItems.add(item);
					} else {
						failedItems.add(item);
					}
				}
			}
			report.addFailedAssertions(failedItems);
			report.addWarningAssertions(warningItems);
			report.addIncompleteAssesrtions(incompleteItems);
			items.removeAll(failedItems);
			items.removeAll(warningItems);
			report.addPassedAssertions(items);
		} catch (SQLException | RestClientException exception) {
			report.addFailedAssertions(Collections.emptyList());
			report.addWarningAssertions(Collections.emptyList());
			report.addPassedAssertions(Collections.emptyList());
			statusReport.addFailureMessage(ExceptionUtils.getExceptionCause("Failed to extract test results",exception));
		}

		final long timeEnd = System.currentTimeMillis();
		report.addTimeTaken((timeEnd - timeStart) / 1000);
	}
}
