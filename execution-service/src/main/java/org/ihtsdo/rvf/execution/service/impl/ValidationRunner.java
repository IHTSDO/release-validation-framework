package org.ihtsdo.rvf.execution.service.impl;

import com.google.common.collect.Sets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.jira.JiraService;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.quality.validator.mrcm.ValidationRun;
import org.snomed.quality.validator.mrcm.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class ValidationRunner {
	
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";

	private static final String MMRCM_TYPE_VALIDATION = "mrcm-validation";

	private static final String VALIDATION_CONFIG = "validationConfig";

	public static final String FAILURE_MESSAGE = "failureMessage";

	private final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
	
	@Autowired
	private StructuralTestRunner structuralTestRunner;
	
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	@Autowired
	private AssertionService assertionService;
	
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	private int batchSize = 0;

	private ExecutorService executorService = Executors.newCachedThreadPool();

	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	ValidationVersionLoader releaseVersionLoader;

	private String droolsRuleDirectoryPath;
	
	public ValidationRunner(int batchSize, String droolsRuleDirectoryPath) {
		this.batchSize = batchSize;
		this.droolsRuleDirectoryPath = droolsRuleDirectoryPath;
	}
	
	public void run(ValidationRunConfig validationConfig) {
		final Map<String , Object> responseMap = new LinkedHashMap<>();
		try {
			responseMap.put(VALIDATION_CONFIG, validationConfig);
			runValidation(responseMap, validationConfig);
		} catch (final Throwable t) {
			final StringWriter errors = new StringWriter();
			t.printStackTrace(new PrintWriter(errors));
			final String failureMsg = "System Failure: " + t.getMessage() + " : " + errors.toString();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			logger.error("Exception thrown, writing as result",t);
			try {
				reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
			} catch (final Exception e) {
				//Can't even record the error to disk!  Lets hope Telemetry is working
				logger.error("Failed to record failure (which was: " + failureMsg + ") due to " + e.getMessage());
			}
		} finally {
			FileUtils.deleteQuietly(validationConfig.getLocalProspectiveFile());
			FileUtils.deleteQuietly(validationConfig.getLocalManifestFile());
		}
	}
	
	
	private void runValidation(final Map<String , Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		final Calendar startTime = Calendar.getInstance();
		//download prospective version
		releaseVersionLoader.downloadProspectiveVersion(validationConfig);
		logger.info(String.format("Started execution with runId [%1s] : ", validationConfig.getRunId()));
		// load the filename
		final String structureTestStartMsg = "Start structure testing for release file:" + validationConfig.getTestFileName();
		logger.info(structureTestStartMsg);
		String reportStorage = validationConfig.getStorageLocation();
		reportService.writeProgress(structureTestStartMsg, reportStorage);
		reportService.writeState(State.RUNNING, reportStorage);
	
		if (validationConfig.getLocalProspectiveFile() == null) {
			reportService.writeResults(responseMap, State.FAILED, reportStorage);
			String errorMsg ="Prospective file can't be null" + validationConfig.getLocalProspectiveFile();
			logger.error(errorMsg);
			responseMap.put(FAILURE_MESSAGE, errorMsg);
			throw new BusinessServiceException(errorMsg);
		}

		ValidationReport report = new ValidationReport();
		report.setExecutionId(validationConfig.getRunId());

		boolean isFailed = structuralTestRunner.verifyZipFileStructure(report, validationConfig.getLocalProspectiveFile(), validationConfig.getRunId(),
				validationConfig.getLocalManifestFile(), validationConfig.isWriteSucceses(), validationConfig.getUrl(), validationConfig.getStorageLocation(),
				validationConfig.getFailureExportMax());
		reportService.putFileIntoS3(reportStorage, new File(structuralTestRunner.getStructureTestReportFullPath()));
		if (isFailed) {
			reportService.writeResults(responseMap, State.FAILED, reportStorage);
			return;
		}

		//load previous published version
		ExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		//check dependency version is loaded
		boolean isLoaded = false;
		if (executionConfig.isExtensionValidation()) {
			isLoaded = releaseVersionLoader.loadDependncyVersion(executionConfig, responseMap, validationConfig);
			if (!releaseVersionLoader.isKnownVersion(executionConfig.getExtensionDependencyVersion(), responseMap)) {
				reportService.writeResults(responseMap, State.FAILED, reportStorage);
				return;
			}
		}
		//check previous version is loaded
		if (executionConfig.isReleaseValidation() && !executionConfig.isFirstTimeRelease()) {
		   isLoaded = releaseVersionLoader.loadPreviousVersion(executionConfig, responseMap, validationConfig);
			if (!isLoaded) {
				reportService.writeResults(responseMap, State.FAILED, reportStorage);
				return;
			}
		}

		//load prospective version
		boolean isSuccessful = releaseVersionLoader.loadProspectiveVersion(executionConfig, responseMap, validationConfig);
		if (!isSuccessful) {
			reportService.writeResults(responseMap, State.FAILED, reportStorage);
			return;
		}
		// for extension release validation we need to test the release-type validations first using previous extension against current extension
		// first then loading the international snapshot for the file-centric and component-centric validations.

		if (executionConfig.isReleaseValidation() && executionConfig.isExtensionValidation()) {
			logger.info("Run extension release validation with runId:" +  executionConfig.getExecutionId());
			runExtensionReleaseValidation(report, responseMap, validationConfig,reportStorage, executionConfig);
		} else {
			runAssertionTests(report, executionConfig, reportStorage);
		}

		if(validationConfig.isEnableDrools()) {
			// Run Drools Validator
			runDroolsAssertions(responseMap, validationConfig, executionConfig);
		}



		//Run MRCM Validator
//		runMRCMAssertionTests(report, validationConfig, executionConfig);

		report.sortAssertionLists();
		responseMap.put("TestResult", report);
		final Calendar endTime = Calendar.getInstance();
		final long timeTaken = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 60000;
		logger.info(String.format("Finished execution with runId : [%1s] in [%2s] minutes ", validationConfig.getRunId(), timeTaken));
		responseMap.put("startTime", startTime.getTime());
		responseMap.put("endTime", endTime.getTime());
		reportService.writeResults(responseMap, State.COMPLETE, reportStorage);
		releaseDataManager.dropVersion(executionConfig.getProspectiveVersion());
		releaseDataManager.clearQAResult(executionConfig.getExecutionId());
	}

	private void runDroolsAssertions(Map<String, Object> responseMap, ValidationRunConfig validationConfig, ExecutionConfig executionConfig) throws RVFExecutionException {

			long timeStart = new Date().getTime();
			//Filter only Drools rules set from all the assertion groups
			Set<String> droolsRulesSets = getDroolsRulesSetFromAssertionGroups(Sets.newHashSet(validationConfig.getGroupsList()));

			//Skip running Drools rules set altogether if there is no Drools rules set in the assertion groups
			if(droolsRulesSets.isEmpty()) return;
			int totalTestsRun = 0;
			try {
				List<InvalidContent> invalidContents;
			try (InputStream snapshotStream = new FileInputStream(validationConfig.getLocalProspectiveFile())) {
				DroolsRF2Validator droolsRF2Validator = new DroolsRF2Validator(droolsRuleDirectoryPath);
				invalidContents = droolsRF2Validator.validateSnapshot(snapshotStream, droolsRulesSets,"");
				for (String assertionGroup : droolsRulesSets) {
					totalTestsRun += droolsRF2Validator.getRuleExecutor().getAssertionGroupRuleCount(assertionGroup);
				}
			} catch (ReleaseImportException | IOException e) {
				throw new RVFExecutionException("Failed to load RF2 snapshot for Drools validation.", e);
			}
			HashMap<String, List<InvalidContent>> invalidContentMap = new HashMap<>();
			for(InvalidContent invalidContent : invalidContents){
				if(!invalidContentMap.containsKey(invalidContent.getMessage())){
					List<InvalidContent> invalidContentArrayList = new ArrayList<>();
					invalidContentArrayList.add(invalidContent);
					invalidContentMap.put(invalidContent.getMessage(), invalidContentArrayList);
				}else {
					invalidContentMap.get(invalidContent.getMessage()).add(invalidContent);
				}
			}
			invalidContents.clear();

			List<AssertionDroolRule> assertionDroolRules = new ArrayList<>();
			int failureExportMax = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 10;
			for (String ruleName : invalidContentMap.keySet()) {
				AssertionDroolRule assertionDroolRule = new AssertionDroolRule();
				assertionDroolRule.setGroupRule(ruleName);
				List<InvalidContent> invalidContentList = invalidContentMap.get(ruleName);
				assertionDroolRule.setTotalFails(invalidContentList.size());
				assertionDroolRule.setContentItems(invalidContentList.stream().limit(failureExportMax).collect(Collectors.toList()));
				assertionDroolRules.add(assertionDroolRule);
			}

			final DroolsRulesValidationReport report = new DroolsRulesValidationReport(TestType.DROOL_RULES);
			report.setAssertionsInvalidContent(assertionDroolRules);
			report.setExecutionId(executionConfig.getExecutionId());
			report.setTotalTestsRun(totalTestsRun);
			report.setTimeTakenInSeconds((System.currentTimeMillis() - timeStart) / 1000);
			report.setTotalFailures(invalidContentMap.size());
			report.setRuleSetExecuted(String.join(",", droolsRulesSets));
			report.setCompleted(true);
			responseMap.put(report.getTestType().toString() + "TestResult", report);
		} catch (Exception ex){
			final DroolsRulesValidationReport report = new DroolsRulesValidationReport(TestType.DROOL_RULES);
			report.setRuleSetExecuted(String.join(",", droolsRulesSets));
			report.setTimeTakenInSeconds((System.currentTimeMillis() - timeStart) / 1000);
			report.setExecutionId(executionConfig.getExecutionId());
			report.setMessage(ExceptionUtils.getStackTrace(ex));
			report.setCompleted(false);
			responseMap.put(report.getTestType().toString() + "TestResult", report);
		}

	}

	private Set<String> getDroolsRulesSetFromAssertionGroups(Set<String> assertionGroups) throws RVFExecutionException {
		File droolsRuleDir = new File(droolsRuleDirectoryPath);
		if(!droolsRuleDir.isDirectory()) throw new RVFExecutionException("Drools rules directory path " + droolsRuleDirectoryPath + " is not a directory or inaccessible");
		Set<String> droolsRulesModules = new HashSet<>();
		File[] droolsRulesSubfiles = droolsRuleDir.listFiles();
		for (File droolsRulesSubfile : droolsRulesSubfiles) {
			if(droolsRulesSubfile.isDirectory()) droolsRulesModules.add(droolsRulesSubfile.getName());
		}
		//Only keep the assertion groups with matching Drools Rule modules in the Drools Directory
		droolsRulesModules.retainAll(assertionGroups);
		return droolsRulesModules;
	}

	private void runExtensionReleaseValidation(final ValidationReport report, final Map<String, Object> responseMap, ValidationRunConfig validationConfig, String reportStorage,
											   ExecutionConfig executionConfig) throws IOException,
			NoSuchAlgorithmException, DecoderException, BusinessServiceException, SQLException {
		final long timeStart = System.currentTimeMillis();
		//run release-type validations
		List<Assertion> assertions = getAssertions(executionConfig.getGroupNames());
		logger.debug("Total assertions found:" + assertions.size());
		List<Assertion> releaseTypeAssertions = new ArrayList<>();
		for (Assertion assertion : assertions) {
			if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				releaseTypeAssertions.add(assertion);
			}
		}
		logger.debug("Running release-type validations:" + releaseTypeAssertions.size());
		List<TestRunItem> testItems = runAssertionTests(executionConfig, releaseTypeAssertions,reportStorage,false);
		String prospectiveExtensionVersion = executionConfig.getProspectiveVersion();
		//loading international snapshot
		releaseVersionLoader.combineCurrenExtensionWithDependencySnapshot(executionConfig, responseMap, validationConfig);
		releaseDataManager.dropVersion(prospectiveExtensionVersion);
		//run remaining component-centric and file-centric validaitons
		assertions.removeAll(releaseTypeAssertions);
		testItems.addAll(runAssertionTests(executionConfig, assertions, reportStorage, true));
		constructTestReport(report, executionConfig, timeStart, testItems);
	}

	private List<Assertion> getAssertions(List<String> groupNames) {
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(groupNames);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			assertions.addAll(assertionService.getAssertionsForGroup(group));
		}
		return new ArrayList<Assertion>(assertions);
	}

	private List<TestRunItem> runAssertionTests(ExecutionConfig executionConfig,List<Assertion> assertions, String reportStorage, boolean runResourceAssertions) {
		List<TestRunItem> result = new ArrayList<>();
		if (runResourceAssertions) {
			final List<Assertion> resourceAssertions = assertionService.getResourceAssertions();
			logger.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
			reportService.writeProgress("Start executing assertions...", reportStorage);
			result.addAll(executeAssertions(executionConfig, resourceAssertions, reportStorage));
		}
		reportService.writeProgress("Start executing assertions...", reportStorage);
		logger.info("Total assertions to run: " + assertions.size());
		if (batchSize == 0) {
			result.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			result.addAll(executeAssertionsConcurrently(executionConfig,assertions, batchSize, reportStorage));
		}
		return result;
	}

	private void runAssertionTests(final ValidationReport report, final ExecutionConfig executionConfig, String reportStorage) throws IOException {
		final long timeStart = System.currentTimeMillis();
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(executionConfig.getGroupNames());
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		final List<Assertion> resourceAssertions = assertionService.getResourceAssertions();
		logger.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
		reportService.writeProgress("Start executing assertions...", reportStorage);
		 final List<TestRunItem> items = executeAssertions(executionConfig, resourceAssertions, reportStorage);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			for (final Assertion assertion : assertionService.getAssertionsForGroup(group)) {
				assertions.add(assertion);
			}
		}
		logger.info("Total assertions to run: " + assertions.size());
		if (batchSize == 0) {
			items.addAll(executeAssertions(executionConfig, assertions, reportStorage));
		} else {
			items.addAll(executeAssertionsConcurrently(executionConfig,assertions, batchSize, reportStorage));
		}
		constructTestReport(report, executionConfig, timeStart, items);
		
	}

	private void constructTestReport(final ValidationReport report, final ExecutionConfig executionConfig, final long timeStart, final List<TestRunItem> items) {
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

	private List<TestRunItem> executeAssertionsConcurrently(final ExecutionConfig executionConfig, final Collection<Assertion> assertions, int batchSize, String reportStorage) {
		
		final List<Future<Collection<TestRunItem>>> tasks = new ArrayList<>();
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList();
			}
			batch.add(assertion);
			if (counter % batchSize == 0 || counter == assertions.size()) {
				final List<Assertion> work = batch;
				logger.info(String.format("Started executing assertion [%1s] of [%2s]", counter, assertions.size()));
				final Future<Collection<TestRunItem>> future = executorService.submit(new Callable<Collection<TestRunItem>>() {
					@Override
					public Collection<TestRunItem> call() throws Exception {
						return assertionExecutionService.executeAssertions(work, executionConfig);
					}
				});
				logger.info(String.format("Finished executing assertion [%1s] of [%2s]", counter, assertions.size()));
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are started.", counter, assertions.size()), reportStorage);
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
				logger.error("Thread interrupted while waiting for future result for run item:" + task , e);
			}
		}
		return results;
	}

	private List<TestRunItem> executeAssertions(final ExecutionConfig executionConfig, final Collection<Assertion> assertions, String reportStorage) {
		
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		for (final Assertion assertion: assertions) {
			logger.info(String.format("Started executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
			results.addAll(assertionExecutionService.executeAssertion(assertion, executionConfig));
			logger.info(String.format("Finished executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
			counter++;
			if (counter % 10 == 0) {
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()), reportStorage);
			}
		}
		reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()), reportStorage);
		return results;
	}

}
