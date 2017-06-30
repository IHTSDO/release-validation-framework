package org.ihtsdo.rvf.execution.service.impl;

import com.google.common.collect.Sets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentFactory;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.service.DroolsConceptService;
import org.ihtsdo.drools.validator.rf2.service.DroolsDescriptionService;
import org.ihtsdo.drools.validator.rf2.service.DroolsRelationshipService;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
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
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import org.h2.tools.*;
import java.util.concurrent.atomic.AtomicInteger;

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

	private String droolRulesModuleName;
	
	public ValidationRunner( int batchSize) {
		this.batchSize = batchSize;
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
		
		boolean isFailed = structuralTestRunner.verifyZipFileStructure(responseMap, validationConfig.getLocalProspectiveFile(), validationConfig.getRunId(), 
				validationConfig.getLocalManifestFile(), validationConfig.isWriteSucceses(), validationConfig.getUrl(), validationConfig.getStorageLocation());
		reportService.putFileIntoS3(reportStorage, new File(structuralTestRunner.getStructureTestReportFullPath()));
		if (isFailed) {
			reportService.writeResults(responseMap, State.FAILED, reportStorage);
			return;
		} 

		//load previous published version
		ExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		//check dependency version is loaded
		if (executionConfig.isExtensionValidation()) {
			if (!releaseVersionLoader.isKnownVersion(executionConfig.getExtensionDependencyVersion(), responseMap)) {
				reportService.writeResults(responseMap, State.FAILED, reportStorage);
				return;
			}
		}
		if (executionConfig.isReleaseValidation() && !executionConfig.isFirstTimeRelease()) {
			boolean isLoaded = releaseVersionLoader.loadPreviousVersion(executionConfig, validationConfig, responseMap);
			if (!isLoaded) {
				reportService.writeResults(responseMap, State.FAILED, reportStorage);
				return;
			}
		}
		//check version are loaded
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
			runExtensionReleaseValidation(responseMap, validationConfig,reportStorage, executionConfig);
		} else {
			runAssertionTests(executionConfig,responseMap, reportStorage);
		}

		//Run Drool Validator
		runDroolValidator(responseMap, validationConfig, executionConfig);

		//Run MRCM Validator
		runMRCMAssertionTests(responseMap, validationConfig, executionConfig);

		final Calendar endTime = Calendar.getInstance();
		final long timeTaken = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 60000;
		logger.info(String.format("Finished execution with runId : [%1s] in [%2s] minutes ", validationConfig.getRunId(), timeTaken));
		responseMap.put("startTime", startTime.getTime());
		responseMap.put("endTime", endTime.getTime());
		reportService.writeResults(responseMap, State.COMPLETE, reportStorage);
		releaseDataManager.dropVersion(executionConfig.getProspectiveVersion());
	}

	private void runDroolValidator(Map<String, Object> responseMap, ValidationRunConfig validationConfig, ExecutionConfig executionConfig) {
		long timeStart = (new Date()).getTime();
		//String releaseFilePath = "D:\\Projects\\SNOMED\\release-validation-framework\\xSnomedCT_MRCMReferenceSets_Beta_20170210.zip";
		String directoryOfRuleSetsPath = droolRulesModuleName;

		HashSet<String> ruleSetNamesToRun = Sets.newHashSet(validationConfig.getGroupsList().iterator().next());
		List<InvalidContent> invalidContents = null;
		try {
			invalidContents = validateRF2(new FileInputStream(validationConfig.getLocalProspectiveFile()), directoryOfRuleSetsPath, ruleSetNamesToRun);
		} catch (ReleaseImportException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		RuleExecutor ruleExecutor = new RuleExecutor(directoryOfRuleSetsPath);
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
		Iterator it = invalidContentMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			AssertionDroolRule assertionDroolRule = new AssertionDroolRule();
			List<InvalidContent> invalidContentList = (List<InvalidContent>) pair.getValue();
			assertionDroolRule.setGroupRule((String)pair.getKey());
			assertionDroolRule.setTotalFails(invalidContentList.size());
			assertionDroolRule.setContentItems(invalidContentList);
			assertionDroolRules.add(assertionDroolRule);
			it.remove(); // avoids a ConcurrentModificationException
		}


		final DroolsRulesValidationReport report = new DroolsRulesValidationReport(TestType.DROOL_RULES);
		report.setAssertionsInvalidContent(assertionDroolRules);
		report.setExecutionId(executionConfig.getExecutionId());
		report.setTotalTestsRun(ruleExecutor.getTotalRulesLoaded());
		report.setTimeTakenInSeconds((System.currentTimeMillis() - timeStart) / 1000);
		report.setTotalFailures(invalidContents.size());
		report.setRuleSetExecuted(validationConfig.getGroupsList().iterator().next());
		responseMap.put(report.getTestType().toString() + "TestResult", report);

	}

	private List<InvalidContent> validateRF2(InputStream fileInputStream, String directoryOfRuleSetsPath, HashSet<String> ruleSetNamesToRun) throws ReleaseImportException {
		long start = (new Date()).getTime();
		Assert.isTrue((new File(directoryOfRuleSetsPath)).isDirectory(), "The rules directory is not accessible.");
		Assert.isTrue(ruleSetNamesToRun != null && !ruleSetNamesToRun.isEmpty(), "The name of at least one rule set must be specified.");
		ReleaseImporter importer = new ReleaseImporter();
		SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();
		this.logger.info("Loading components from RF2");
		LoadingProfile loadingProfile = LoadingProfile.complete;

		importer.loadSnapshotReleaseFiles(fileInputStream, loadingProfile,  new SnomedDroolsComponentFactory(repository));
		this.logger.info("Components loaded");
		DroolsConceptService conceptService = new DroolsConceptService(repository);
		DroolsDescriptionService descriptionService = new DroolsDescriptionService(repository);
		DroolsRelationshipService relationshipService = new DroolsRelationshipService(repository);
		RuleExecutor ruleExecutor = new RuleExecutor(directoryOfRuleSetsPath);

		Collection<DroolsConcept> concepts = repository.getConcepts();
		this.logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, concepts, conceptService, descriptionService, relationshipService, true, false);
		this.logger.info("Tests complete. Total run time {} seconds", Long.valueOf(((new Date()).getTime() - start) / 1000L));
		this.logger.info("invalidContent count {}", Integer.valueOf(invalidContents.size()));
		return invalidContents;
	}
	private File extractZipFile(ValidationRunConfig validationConfig, Long executionId) throws BusinessServiceException {
		File outputFolder;
		try{
			outputFolder = new File(FileUtils.getTempDirectoryPath(), "rvf_loader_data_" + executionId);
			logger.info("MRCM output folder location = " + outputFolder.getAbsolutePath());
			if (outputFolder.exists()) {
				logger.info("MRCM output folder already exists and will be deleted before recreating.");
				outputFolder.delete();
			}
			outputFolder.mkdir();
			ZipFileUtils.extractFilesFromZipToOneFolder(validationConfig.getLocalProspectiveFile(), outputFolder.getAbsolutePath());
		} catch (final IOException ex){
			final String errorMsg = String.format("Error while loading file %s.", validationConfig.getLocalProspectiveFile());
			logger.error(errorMsg, ex);
			throw new BusinessServiceException(errorMsg, ex);
		}
		return outputFolder;
	}
	private void runMRCMAssertionTests(final Map<String, Object> responseMap, ValidationRunConfig validationConfig, ExecutionConfig executionConfig) throws IOException, ReleaseImportException, ServiceException {
		final long timeStart = System.currentTimeMillis();
		ValidationService validationService = new ValidationService();
		ValidationRun validationRun = new ValidationRun();
		File outputFolder = null;
		try {
			outputFolder = extractZipFile(validationConfig, executionConfig.getExecutionId());

		} catch (BusinessServiceException ex) {
			logger.error("Error:" + ex);
		}
		if(outputFolder != null){
			validationService.loadMRCM(outputFolder, validationRun);
			validationService.validateRelease(outputFolder, validationRun);
			FileUtils.deleteQuietly(outputFolder);
		}

		TestRunItem testRunItem;
		final List<TestRunItem> passedAssertions = new ArrayList<>();
		for(org.snomed.quality.validator.mrcm.Assertion assertion : validationRun.getCompletedAssertions()){
			testRunItem = new TestRunItem();
			testRunItem.setTestCategory(MMRCM_TYPE_VALIDATION);
			testRunItem.setAssertionUuid(assertion.getUuid());
			testRunItem.setAssertionText(assertion.getAssertionText());
			testRunItem.setFailureCount(0L);
			testRunItem.setExtractResultInMillis(0L);
			passedAssertions.add(testRunItem);
		}

		final List<TestRunItem> skippedAssertions = new ArrayList<>();
		for(org.snomed.quality.validator.mrcm.Assertion assertion : validationRun.getSkippedAssertions()){
			testRunItem = new TestRunItem();
			testRunItem.setTestCategory(MMRCM_TYPE_VALIDATION);
			testRunItem.setAssertionUuid(assertion.getUuid());
			testRunItem.setAssertionText(assertion.getAssertionText());
			testRunItem.setFailureCount(0L);
			testRunItem.setExtractResultInMillis(0L);
			skippedAssertions.add(testRunItem);
		}

		final List<TestRunItem> failedAssertions = new ArrayList<>();
		for(org.snomed.quality.validator.mrcm.Assertion assertion : validationRun.getFailedAssertions()){
			testRunItem = new TestRunItem();
			testRunItem.setTestCategory(MMRCM_TYPE_VALIDATION);
			testRunItem.setAssertionUuid(assertion.getUuid());
			testRunItem.setAssertionText(assertion.getAssertionText());
			testRunItem.setExtractResultInMillis(0L);
			int failureCount = assertion.getConceptIdsWithInvalidAttributeValue().size();
			testRunItem.setFailureCount(Long.valueOf(failureCount));
			List<FailureDetail> failedDetails = new ArrayList(failureCount);
			for (Long conceptId : assertion.getConceptIdsWithInvalidAttributeValue()){
				failedDetails.add(new FailureDetail(String.valueOf(conceptId), assertion.getAssertionText(), null));
			}
			testRunItem.setFirstNInstances(failedDetails);
			failedAssertions.add(testRunItem);
		}

		Collections.sort(passedAssertions);
		Collections.sort(skippedAssertions);
		Collections.sort(failedAssertions);
		final MrcmValidationReport report = new MrcmValidationReport(TestType.MRCM);
		report.setExecutionId(executionConfig.getExecutionId());
		report.setTotalTestsRun(passedAssertions.size() + skippedAssertions.size() + failedAssertions.size());
		report.setTimeTakenInSeconds((System.currentTimeMillis() - timeStart) / 1000);
		report.setTotalSkips(skippedAssertions.size());
		report.setTotalFailures(failedAssertions.size());
		report.setAssertionsSkipped(skippedAssertions);
		report.setAssertionsFailed(failedAssertions);
		report.setAssertionsPassed(passedAssertions);
		responseMap.put(report.getTestType().toString() + "TestResult", report);
	}

	private void runExtensionReleaseValidation(final Map<String, Object> responseMap, ValidationRunConfig validationConfig, String reportStorage,
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
		constructTestReport(executionConfig, responseMap, timeStart, testItems);
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

	private void runAssertionTests(final ExecutionConfig executionConfig, final Map<String, Object> responseMap, String reportStorage) throws IOException {
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
		constructTestReport(executionConfig, responseMap, timeStart, items);
		
	}

	private void constructTestReport(final ExecutionConfig executionConfig, final Map<String, Object> responseMap, final long timeStart, final List<TestRunItem> items) {
		final long timeEnd = System.currentTimeMillis();
		
		//summary
		final ValidationReport summary = new ValidationReport(TestType.SQL);
		final ValidationReport failedReport = new ValidationReport(TestType.SQL);
		final ValidationReport warningReport = new ValidationReport(TestType.SQL);
		
		summary.setExecutionId(executionConfig.getExecutionId());
		summary.setTotalTestsRun(items.size());
		summary.setTimeTakenInSeconds((timeEnd - timeStart) / 1000);
		
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
		}
		
		if (!failedItems.isEmpty()) {
			Collections.sort(failedItems);
			failedReport.setExecutionId(executionConfig.getExecutionId());
			failedReport.setTotalFailures(failedItems.size());
			failedReport.setFailedAssertions(failedItems);
		}
		
		if(!warningItems.isEmpty()) {
			Collections.sort(warningItems);
			warningReport.setExecutionId(executionConfig.getExecutionId());
			warningReport.setTotalWarnings(warningItems.size());
			warningReport.setAssertionsWarning(warningItems);			
		}
		
		items.removeAll(failedItems);
		items.removeAll(warningItems);
		Collections.sort(items);
		summary.setPassedAssertions(items);
		summary.setTotalFailures(failedItems.size());
		summary.setTotalWarnings(warningItems.size());
		
		responseMap.put("SQL Test result summary", summary);
		if(failedReport.getTotalFailures() != null && failedReport.getTotalFailures() > 0) {
			responseMap.put(failedReport.getTestType().toString() + "FailedTestResult", failedReport);
		}
		if(warningReport.getTotalWarnings() != null && warningReport.getTotalWarnings() > 0){
			responseMap.put(warningReport.getTestType().toString() + "WarningTestResult", warningReport);
		}
	}

	private List<TestRunItem> executeAssertionsConcurrently(final ExecutionConfig executionConfig, final Collection<Assertion> assertions, int batchSize, String reportStorage) {
		
		final List<Future<Collection<TestRunItem>>> tasks = new ArrayList<>();
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList<Assertion>();
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

	public void setDroolRulesModuleName(String droolRulesModuleName) {
		this.droolRulesModuleName = droolRulesModuleName;
	}
}
