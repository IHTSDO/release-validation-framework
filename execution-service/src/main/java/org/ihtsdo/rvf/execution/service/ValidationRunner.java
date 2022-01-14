package org.ihtsdo.rvf.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.DecoderException;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.ValidationReportService.State;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Scope("prototype")
public class ValidationRunner {

	public static final List<String> EMPTY_TEST_ASSERTION_GROUPS = Collections.singletonList("empty-test");

	@Autowired
	private StructuralTestRunner structuralTestRunner;
	
	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	private ValidationVersionLoader releaseVersionLoader;
	
	@Autowired 
	private DroolsRulesValidationService droolsValidationService;
	
	@Autowired
	private MysqlValidationService mysqlValidationService;

	@Autowired
	private MRCMValidationService mrcmValidationService;

	@Autowired
	private TraceabilityComparisonService traceabilityComparisonService;

	@Autowired
	private MessagingHelper messagingHelper;

	private static final String MSG_VALIDATIONS_RUN = "Validations executed. Failures count: ";
	private static final String MSG_VALIDATIONS_DISABLED = "Validations are disabled.";

	private final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);

	public void run(ValidationRunConfig validationConfig) {
		try {
			runValidations(validationConfig);
		} catch (final Exception t) {
			StringWriter errors = new StringWriter();
			t.printStackTrace(new PrintWriter(errors));
			String failureMsg = "System Failure: " + t.getMessage() + " : " + errors.toString();
			ValidationStatusReport statusReport = new ValidationStatusReport(validationConfig);
			statusReport.addFailureMessage(failureMsg);
			logger.error("Exception thrown, writing as result",t);
			try {
				reportService.writeResults(statusReport, State.FAILED, validationConfig.getStorageLocation());
				updateRvfState(validationConfig, State.FAILED);
			} catch (final Exception e) {
				throw new IllegalStateException("Failed to record failure (which was: " + failureMsg + ")", e);
			}
		}
	}
	
	private void runValidations(ValidationRunConfig validationConfig) throws Exception {
		// Prepare to run validations
		Calendar startTime = Calendar.getInstance();
		MysqlExecutionConfig executionConfig = releaseVersionLoader.createExecutionConfig(validationConfig);
		releaseVersionLoader.downloadProspectiveFiles(validationConfig);
		releaseVersionLoader.downloadPreviousReleaseAndDependencyFiles(validationConfig);
		if (validationConfig.getLocalProspectiveFile() == null) {
			reportService.writeState(State.FAILED, validationConfig.getStorageLocation());
			String errorMsg = "Prospective file can't be null " + validationConfig.getLocalProspectiveFile();
			reportService.writeProgress(errorMsg, validationConfig.getStorageLocation());
			logger.error(errorMsg);
		}
		ValidationReport report = new ValidationReport();
		report.setExecutionId(executionConfig.getExecutionId());
		report.setReportUrl(validationConfig.getUrl());
		ValidationStatusReport statusReport = new ValidationStatusReport(validationConfig);
		statusReport.setResultReport(report);

		if (!EMPTY_TEST_ASSERTION_GROUPS.equals(validationConfig.getGroupsList())) {
			// Actually run validations
			doRunValidations(validationConfig, statusReport);
		}

		// Update reports and status after validations run
		report.sortAssertionLists();
		final Calendar endTime = Calendar.getInstance();
		final long timeTaken = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 60000;
		logger.info("Finished execution with runId : {} in {} minutes ", validationConfig.getRunId(), timeTaken);
		statusReport.setStartTime(startTime.getTime());
		statusReport.setEndTime(endTime.getTime());
		report.setTimeTakenInSeconds(timeTaken*60);
		State state = statusReport.getFailureMessages().isEmpty() ? State.COMPLETE : State.FAILED;
		updateRvfState(validationConfig, state);
		updateExecutionSummary(statusReport, validationConfig);

		// Ignore token and user name to be persisted to S3
		statusReport.getValidationConfig().setAuthenticationToken(null);
		statusReport.getValidationConfig().setUsername(null);
		reportService.writeResults(statusReport, state, validationConfig.getStorageLocation());
	}

	private void doRunValidations(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws Exception {
		runRF2StructureTests(validationConfig, statusReport);

		Map<String, Future<ValidationStatusReport>> taskMap = new HashMap<>();
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		StringBuilder statusMessages = new StringBuilder();
		statusMessages.append("RVF assertions validation started");
		reportService.writeProgress(statusMessages.toString(), validationConfig.getStorageLocation());

		ValidationStatusReport mysqlValidationStatusReport = new ValidationStatusReport(validationConfig);
		mysqlValidationStatusReport.setResultReport(new ValidationReport());
		taskMap.put("SQL Assertions", executorService.submit(() -> mysqlValidationService.runRF2MysqlValidations(validationConfig, mysqlValidationStatusReport)));

		if (validationConfig.isEnableDrools()) {
			statusMessages.append("\nDrools rules validation started");
			reportService.writeProgress(statusMessages.toString(), validationConfig.getStorageLocation());
			ValidationStatusReport droolsValidationStatusReport = new ValidationStatusReport(validationConfig);
			droolsValidationStatusReport.setResultReport(new ValidationReport());
			taskMap.put("Drools Assertions", executorService.submit(() -> droolsValidationService.runDroolsAssertions(validationConfig, droolsValidationStatusReport)));
		}

		if (validationConfig.isEnableMRCMValidation()) {
			statusMessages.append("\nMRCM validation started");
			reportService.writeProgress(statusMessages.toString(), validationConfig.getStorageLocation());
			ValidationStatusReport mrcmValidationStatusReport = new ValidationStatusReport(validationConfig);
			mrcmValidationStatusReport.setResultReport(new ValidationReport());
			taskMap.put("MRCM Validation", executorService.submit(() -> mrcmValidationService.runMRCMAssertionTests(mrcmValidationStatusReport, validationConfig)));
		}

		if (validationConfig.isEnableTraceabilityValidation()) {
			statusMessages.append("\nTraceability comparison validation started");
			reportService.writeProgress(statusMessages.toString(), validationConfig.getStorageLocation());
			ValidationStatusReport traceabilityComparisonReport = new ValidationStatusReport(validationConfig);
			traceabilityComparisonReport.setResultReport(new ValidationReport());
			taskMap.put("Traceability Comparison", executorService.submit(() -> {
				traceabilityComparisonService.runTraceabilityComparison(traceabilityComparisonReport, validationConfig);
				return traceabilityComparisonReport;
			}));
		}

		for (Map.Entry<String, Future<ValidationStatusReport>> entry : taskMap.entrySet()) {
			mergeValidationStatusReports(statusReport, entry.getValue().get());
		}
		executorService.shutdown();
	}

	private void updateRvfState(final ValidationRunConfig config, final State state) throws JsonProcessingException, JMSException {
		final String responseQueue = config.getResponseQueue();
		if (responseQueue != null) {
			logger.info("Updating RVF state to {}}: {}", state, responseQueue);
			messagingHelper.send(responseQueue,
					Map.of("runId", config.getRunId(),
							"state", state.name(),
							"username", config.getUsername() != null ? config.getUsername() : "",
							"authenticationToken", config.getAuthenticationToken() != null ? config.getAuthenticationToken() : ""));
		}
	}

	private void mergeValidationStatusReports(ValidationStatusReport mainValidationReport, ValidationStatusReport validationTaskReport) {
		ValidationReport mainResult = mainValidationReport.getResultReport();
		ValidationReport taskResult = validationTaskReport.getResultReport();

		mainResult.getAssertionsFailed().addAll(taskResult.getAssertionsFailed());
		mainResult.getAssertionsWarning().addAll(taskResult.getAssertionsWarning());
		mainResult.getAssertionsSkipped().addAll(taskResult.getAssertionsSkipped());
		mainResult.getAssertionsPassed().addAll(taskResult.getAssertionsPassed());
		
		mainResult.setTotalTestsRun(mainResult.getTotalTestsRun() + taskResult.getTotalTestsRun());
		mainResult.setTotalFailures(mainResult.getTotalFailures() + taskResult.getTotalFailures());
		mainResult.setTotalWarnings(mainResult.getTotalWarnings() + taskResult.getTotalWarnings());
		mainResult.setTotalTestsIncomplete(mainResult.getTotalTestsIncomplete() + taskResult.getTotalTestsIncomplete());
		mainResult.setTotalSkips(mainResult.getTotalSkips() + taskResult.getTotalSkips());

		mainValidationReport.getFailureMessages().addAll(validationTaskReport.getFailureMessages());
		mainValidationReport.getRf2FilesLoaded().addAll(validationTaskReport.getRf2FilesLoaded());
		mainValidationReport.setTotalRF2FilesLoaded(mainValidationReport.getTotalRF2FilesLoaded());

		mainValidationReport.getReportSummary().putAll(validationTaskReport.getReportSummary());
		
	}
	
	private void runRF2StructureTests(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws NoSuchAlgorithmException, IOException, DecoderException, BusinessServiceException {
		logger.info("Started execution with runId {}", validationConfig.getRunId());
		// load the filename
		String structureTestStartMsg = "Start structure testing for release file:" + validationConfig.getTestFileName();
		logger.info(structureTestStartMsg);
		String reportStorage = validationConfig.getStorageLocation();
		reportService.writeProgress(structureTestStartMsg, reportStorage);
		reportService.writeState(State.RUNNING, reportStorage);

		boolean isFailed = structuralTestRunner.verifyZipFileStructure(statusReport.getResultReport(), 
																		validationConfig.getLocalProspectiveFile(),
																		validationConfig.getRunId(),
																		validationConfig.getLocalManifestFile(),
																		validationConfig.isWriteSucceses(),
																		validationConfig.getUrl(),
																		validationConfig.getStorageLocation(),
																		validationConfig.getFailureExportMax());
		
		reportService.putFileIntoS3(reportStorage, new File(structuralTestRunner.getStructureTestReportFullPath()));
		if (isFailed) {
			reportService.writeResults(statusReport, State.FAILED, reportStorage);
		}
	}

	private void updateExecutionSummary(ValidationStatusReport statusReport, ValidationRunConfig validationRunConfig) {
		List<TestRunItem> failures = statusReport.getResultReport().getAssertionsFailed();
		Map<TestType, Integer> testTypeFailuresCount = new EnumMap<>(TestType.class);
		testTypeFailuresCount.put(TestType.ARCHIVE_STRUCTURAL, 0);
		testTypeFailuresCount.put(TestType.SQL, 0);
		testTypeFailuresCount.put(TestType.DROOL_RULES, validationRunConfig.isEnableDrools() ? 0 : -1);
		testTypeFailuresCount.put(TestType.MRCM, validationRunConfig.isEnableMRCMValidation() ? 0 : -1);
		testTypeFailuresCount.put(TestType.TRACEABILITY, validationRunConfig.isEnableTraceabilityValidation() ? 0 : -1);
		for (TestRunItem failure : failures) {
			TestType testType = failure.getTestType();
			testTypeFailuresCount.put(testType, testTypeFailuresCount.get(testType)+1);
		}
		for (Map.Entry<TestType, Integer> entry : testTypeFailuresCount.entrySet()) {
			if(statusReport.getReportSummary().get(entry.getKey().name()) == null) {
				Integer failuresCount = entry.getValue();
				statusReport.getReportSummary().put(entry.getKey().name(), failuresCount >= 0 ? MSG_VALIDATIONS_RUN + failuresCount : MSG_VALIDATIONS_DISABLED);
			}
		}
	}
}
