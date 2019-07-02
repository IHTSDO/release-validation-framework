package org.ihtsdo.rvf.execution.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.SeverityLevel;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MysqlValidationService {
	
	@Autowired
	private AssertionService assertionService;
	
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	@Value("${rvf.assertion.execution.BatchSize}")
	private int batchSize;
	
	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	private ValidationVersionLoader releaseVersionLoader;

	private static final Logger LOGGER = LoggerFactory.getLogger(MysqlValidationService.class);
	
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
	
	private ExecutorService executorService = Executors.newCachedThreadPool();

	public ValidationStatusReport runRF2MysqlValidations(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws BusinessServiceException{
		MysqlExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		String reportStorage = validationConfig.getStorageLocation();
		try {
			//prepare release data for testing
			if (!executionConfig.isFirstTimeRelease()) {
				releaseVersionLoader.loadPreviousVersion(executionConfig);
			}
			//load dependency release
			if (executionConfig.isExtensionValidation()) {
				releaseVersionLoader.loadDependncyVersion(executionConfig);
				if (!releaseVersionLoader.isKnownVersion(executionConfig.getExtensionDependencyVersion())) {
					statusReport.addFailureMessage("Failed to load dependency release " + executionConfig.getExtensionDependencyVersion());
				}
			}
			//load prospective version
			releaseVersionLoader.loadProspectiveVersion(statusReport, executionConfig, validationConfig);
		} catch (Exception e) {
			String msg = "Failed to prepare versions for mysql validation";
			msg = e.getMessage()!= null ? msg + " due to error: " + e.getMessage() : msg;
			LOGGER.error(msg, e);
			statusReport.addFailureMessage(msg);
			statusReport.getReportSummary().put(TestType.SQL.name(), msg);
			return statusReport;
		}
		if (executionConfig.isReleaseValidation() && executionConfig.isExtensionValidation()) {
			LOGGER.info("Run extension release validation with config " +  executionConfig);
			runExtensionReleaseValidation(statusReport, validationConfig, executionConfig);
		} else {
			LOGGER.info("Run international release validation with config " + executionConfig);
			runAssertionTests(statusReport, executionConfig, reportStorage);
		}
		return statusReport;
	}


	/** For extension release validation we need to test the release-type validations first using previous extension against current extension
	 * first then loading the international snapshot for the file-centric and component-centric validations.
	 * load previous published version
	 * @param statusReport
	 * @param validationConfig
	 * @param executionConfig
	 * @throws BusinessServiceException
	 */
	private void runExtensionReleaseValidation(ValidationStatusReport statusReport, ValidationRunConfig validationConfig, MysqlExecutionConfig executionConfig) throws BusinessServiceException {
		final long timeStart = System.currentTimeMillis();
		//run release-type validations
		List<Assertion> assertions = getAssertions(executionConfig.getGroupNames());
		LOGGER.debug("Total assertions found:" + assertions.size());
		List<Assertion> releaseTypeAssertions = new ArrayList<>();
		for (Assertion assertion : assertions) {
			if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				releaseTypeAssertions.add(assertion);
			}
		}
		LOGGER.debug("Running release-type validations:" + releaseTypeAssertions.size());
		String reportStorage = validationConfig.getStorageLocation();
		List<TestRunItem> testItems = runAssertionTests(executionConfig, releaseTypeAssertions,reportStorage,false);
		//loading international snapshot
		try {
			releaseVersionLoader.combineCurrenExtensionWithDependencySnapshot(executionConfig, validationConfig);
		} catch (BusinessServiceException e) {
			String msg = "Failed to prepare data for extension testing due to error:" + e.getMessage();
			statusReport.addFailureMessage(msg);
			LOGGER.error(msg, e);
			statusReport.getReportSummary().put(TestType.SQL.name(), msg);
		}
		//remove already run release-type validations 
		assertions.removeAll(releaseTypeAssertions);
		testItems.addAll(runAssertionTests(executionConfig, assertions, reportStorage, true));
		constructTestReport(statusReport.getResultReport(), executionConfig, timeStart, testItems);
	}
	
	private List<Assertion> getAssertions(List<String> groupNames) {
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(groupNames);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			assertions.addAll(group.getAssertions());
		}
		return new ArrayList<Assertion>(assertions);
	}

	private List<TestRunItem> runAssertionTests(MysqlExecutionConfig executionConfig, List<Assertion> assertions,
			String reportStorage, boolean runResourceAssertions) {
		List<TestRunItem> result = new ArrayList<>();
		if (runResourceAssertions) {
			final List<Assertion> resourceAssertions = assertionService.getAssertionsByKeyWords("resource", true);
			LOGGER.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
			reportService.writeProgress("Start executing assertions...", reportStorage);
			result.addAll(executeAssertions(executionConfig, resourceAssertions, reportStorage));
		}
		reportService.writeProgress("Start executing assertions...", reportStorage);
		LOGGER.info("Total assertions to run: " + assertions.size());
		if (batchSize == 0) {
			result.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			result.addAll(executeAssertionsConcurrently(executionConfig,assertions, batchSize, reportStorage));
		}
		return result;
	}

	private void runAssertionTests( ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, String reportStorage) {
		long timeStart = System.currentTimeMillis();
		List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(executionConfig.getGroupNames());
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		List<Assertion> resourceAssertions = assertionService.getAssertionsByKeyWords("resource", true);
		LOGGER.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
		reportService.writeProgress("Start executing assertions...", reportStorage);
		List<TestRunItem> items = executeAssertions(executionConfig, resourceAssertions, reportStorage);
		Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			assertions.addAll(group.getAssertions());
		}
		LOGGER.info("Total assertions to run: " + assertions.size());
		if (batchSize == 0) {
			items.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			items.addAll(executeAssertionsConcurrently(executionConfig, assertions, batchSize, reportStorage));
		}
		constructTestReport(statusReport.getResultReport(), executionConfig, timeStart, items);
		
	}

	private List<TestRunItem> executeAssertionsConcurrently(MysqlExecutionConfig executionConfig, Collection<Assertion> assertions,
			int batchSize, String reportStorage) {
		List<Future<Collection<TestRunItem>>> tasks = new ArrayList<>();
		List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList<Assertion>();
			}
			batch.add(assertion);
			if (counter % batchSize == 0 || counter == assertions.size()) {
				final List<Assertion> work = batch;
				LOGGER.info(String.format("Started executing assertion [%1s] of [%2s]", counter, assertions.size()));
				final Future<Collection<TestRunItem>> future = executorService.submit(new Callable<Collection<TestRunItem>>() {
					@Override
					public Collection<TestRunItem> call() throws Exception {
						return assertionExecutionService.executeAssertions(work, executionConfig);
					}
				});
				LOGGER.info(String.format("Finished executing assertion [%1s] of [%2s]", counter, assertions.size()));
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are started.", counter,
						assertions.size()), reportStorage);
				tasks.add(future);
				batch = null;
			}
			counter++;
		}
		
		// Wait for all concurrent tasks to finish
		for (final Future<Collection<TestRunItem>> task : tasks) {
			try {
				results.addAll(task.get());
			} catch (ExecutionException | InterruptedException e) {
				LOGGER.error("Thread interrupted while waiting for future result for run item:" + task , e);
			}
		}
		return results;
	}

	private List<TestRunItem> executeAssertions(MysqlExecutionConfig executionConfig, Collection<Assertion> assertions, String reportStorage) {
		
		List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		for (Assertion assertion: assertions) {
			LOGGER.info(String.format("Started executing assertion [%1s] of [%2s] with uuid : [%3s]",
					counter, assertions.size(), assertion.getUuid()));
			results.addAll(assertionExecutionService.executeAssertion(assertion, executionConfig));
			LOGGER.info(String.format("Finished executing assertion [%1s] of [%2s] with uuid : [%3s]",
					counter, assertions.size(), assertion.getUuid()));
			counter++;
			if (counter % 10 == 0) {
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", 
						counter, assertions.size()), reportStorage);
			}
		}
		reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", 
				counter, assertions.size()), reportStorage);
		return results;
	}
	
	private void constructTestReport(ValidationReport report, MysqlExecutionConfig executionConfig,
			long timeStart, List<TestRunItem> items) {
		final long timeEnd = System.currentTimeMillis();
		report.addTimeTaken((timeEnd - timeStart) / 1000);
		//failed tests
		final List<TestRunItem> failedItems = new ArrayList<>();
		final List<TestRunItem> warningItems = new ArrayList<>();
		for (final TestRunItem item : items) {
			if (item.getFailureCount() != 0 && !SeverityLevel.WARN.toString().equalsIgnoreCase(item.getSeverity())) {
				failedItems.add(item);
			}
			if(SeverityLevel.WARN.toString().equalsIgnoreCase(item.getSeverity())){
				warningItems.add(item);
			}
			item.setTestType(TestType.SQL);
		}

		report.addFailedAssertions(failedItems);
		report.addWarningAssertions(warningItems);

		items.removeAll(failedItems);
		items.removeAll(warningItems);
		report.addPassedAssertions(items);
	}
}
