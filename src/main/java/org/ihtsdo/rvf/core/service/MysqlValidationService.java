package org.ihtsdo.rvf.core.service;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(MysqlValidationService.class);
	
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
	
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	public ValidationStatusReport runRF2MysqlValidations(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws BusinessServiceException, ExecutionException, InterruptedException {
		MysqlExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		String reportStorage = validationConfig.getStorageLocation();
		try {
			// prepare release data for testing
			if (!executionConfig.isFirstTimeRelease()) {
				releaseVersionLoader.loadPreviousVersion(executionConfig);
			}
			// load dependency release
			if (executionConfig.isExtensionValidation()) {
				releaseVersionLoader.loadDependencyVersion(executionConfig);
				if (!releaseVersionLoader.isKnownVersion(executionConfig.getExtensionDependencyVersion())) {
					statusReport.addFailureMessage("Failed to load dependency release " + executionConfig.getExtensionDependencyVersion());
				}
			}
			// load prospective version
			releaseVersionLoader.loadProspectiveVersion(statusReport, executionConfig, validationConfig);
		} catch (Exception e) {
			String errorMsg = String.format("Failed to load data into MySql due to %s", ExceptionUtils.getRootCauseMessage(e));
			LOGGER.error(errorMsg, e);
			statusReport.addFailureMessage(errorMsg);
			statusReport.getReportSummary().put(TestType.SQL.name(), errorMsg);
			return statusReport;
		}
		if (executionConfig.isExtensionValidation()) {
			LOGGER.info("Run extension release validation with config {}", executionConfig);
			runExtensionReleaseValidation(statusReport, validationConfig, executionConfig);
		} else {
			LOGGER.info("Run international release validation with config {}", executionConfig);
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
		LOGGER.debug("Running release-type validations {}", releaseTypeAssertions.size());
		String reportStorage = validationConfig.getStorageLocation();
		List<TestRunItem> testItems = runAssertionTests(executionConfig, releaseTypeAssertions,reportStorage,false);
		//loading international snapshot
		try {
			releaseVersionLoader.combineCurrentExtensionWithDependencySnapshot(executionConfig, validationConfig);
		} catch (BusinessServiceException e) {
			String msg = String.format("Failed to prepare data for extension testing due to error %s", ExceptionUtils.getRootCauseMessage(e));
			statusReport.addFailureMessage(msg);
			LOGGER.error(msg, e);
			statusReport.getReportSummary().put(TestType.SQL.name(), msg);
		}
		testItems.addAll(runAssertionTests(executionConfig, noneReleaseTypeAssertions, reportStorage, true));
		constructTestReport(statusReport, executionConfig, timeStart, testItems, assertions);
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
			String reportStorage, boolean runResourceAssertions) throws ExecutionException, InterruptedException {
		List<TestRunItem> result = new ArrayList<>();
		if (runResourceAssertions) {
			final List<Assertion> resourceAssertions = assertionService.getAssertionsByKeyWords("resource", true);
			LOGGER.info("Found total resource assertions need to be run before test {}", resourceAssertions.size());
			reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
			result.addAll(executeAssertions(executionConfig, resourceAssertions, reportStorage));
		}
		reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
		LOGGER.info("Total assertions to run {}", assertions.size());
		if (batchSize == 0) {
			result.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			result.addAll(executeAssertionsConcurrently(executionConfig,assertions, batchSize, reportStorage));
		}
		return result;
	}

	private void runAssertionTests( ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, String reportStorage) throws ExecutionException, InterruptedException {
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
		LOGGER.info("Total assertions to run {}", assertions.size());
		if (batchSize == 0) {
			items.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			items.addAll(executeAssertionsConcurrently(executionConfig, assertions, batchSize, reportStorage));
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
					if (item.getFailureCount().longValue() == -1L) {
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
			statusReport.addFailureMessage(String.format("Failed to extract test results caused by %s", ExceptionUtils.getRootCauseMessage(exception)));
		}

		final long timeEnd = System.currentTimeMillis();
		report.addTimeTaken((timeEnd - timeStart) / 1000);
	}
}
