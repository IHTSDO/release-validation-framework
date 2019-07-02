package org.ihtsdo.rvf.execution.service;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.quality.validator.mrcm.Assertion;
import org.snomed.quality.validator.mrcm.ValidationRun;
import org.snomed.quality.validator.mrcm.ValidationService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MRCMValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MRCMValidationService.class);

	public void runMRCMAssertionTests(final ValidationStatusReport statusReport, ValidationRunConfig validationConfig, String effectiveDate, Long executionId) {
		File outputFolder = null;
		try {
			final long timeStart = System.currentTimeMillis();
			ValidationReport report = statusReport.getResultReport();
			ValidationService validationService = new ValidationService();
			ValidationRun validationRun = new ValidationRun(effectiveDate, true);
			try {
				outputFolder = extractZipFile(validationConfig, executionId);

			} catch (BusinessServiceException ex) {
				LOGGER.error("Error:" + ex);
			}
			if(outputFolder != null){
				validationService.loadMRCM(outputFolder, validationRun);
				validationService.validateRelease(outputFolder, validationRun);
			}

			TestRunItem testRunItem;
			final List<TestRunItem> passedAssertions = new ArrayList<>();
			for(Assertion assertion : validationRun.getCompletedAssertions()){
				testRunItem = createTestRunItem(assertion);
				passedAssertions.add(testRunItem);
			}

			final List<TestRunItem> skippedAssertions = new ArrayList<>();
			for(Assertion assertion : validationRun.getSkippedAssertions()){
				testRunItem = createTestRunItem(assertion);
				skippedAssertions.add(testRunItem);
			}

			final List<TestRunItem> failedAssertions = new ArrayList<>();

			for(Assertion assertion : validationRun.getFailedAssertions()){
				int failureCount = assertion.getCurrentViolatedConceptIds().size();
				if(failureCount == 0) continue;
				int maxFailuresCount = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 10;
				int firstNCount = failureCount >= maxFailuresCount? maxFailuresCount : failureCount;
				testRunItem = new TestRunItem();
				testRunItem.setTestCategory("");
				testRunItem.setTestType(TestType.MRCM);
				testRunItem.setAssertionUuid(assertion.getUuid());
				testRunItem.setAssertionText(assertion.getAssertionText());
				testRunItem.setExtractResultInMillis(0L);
				testRunItem.setFailureCount(Long.valueOf(failureCount));
				List<FailureDetail> failedDetails = new ArrayList(firstNCount);
				for(int i = 0; i < firstNCount; i++) {
					Long conceptId = assertion.getCurrentViolatedConceptIds().get(i);
					failedDetails.add(new FailureDetail(String.valueOf(conceptId), assertion.getAssertionText()));
				}
				testRunItem.setFirstNInstances(failedDetails);
				failedAssertions.add(testRunItem);
			}

			report.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
			report.addSkippedAssertions(skippedAssertions);
			report.addFailedAssertions(failedAssertions);
			report.addPassedAssertions(passedAssertions);
		} catch (Exception ex) {
			String message = "MRCM validation has stopped";
			LOGGER.error(message, ex);
			message = ex.getMessage() != null ? message + " due to error: " + ex.getMessage() : message;
			statusReport.addFailureMessage(message);
		}  finally {
			if(outputFolder != null) {
				FileUtils.deleteQuietly(outputFolder);
			}
		}

	}

	private TestRunItem createTestRunItem(Assertion mrcmAssertion) {
		TestRunItem testRunItem = new TestRunItem();
		testRunItem.setTestCategory("");
		testRunItem.setTestType(TestType.MRCM);
		testRunItem.setAssertionUuid(mrcmAssertion.getUuid());
		testRunItem.setAssertionText(mrcmAssertion.getAssertionText());
		testRunItem.setFailureCount(0L);
		testRunItem.setExtractResultInMillis(0L);
		return testRunItem;
	}

	private File extractZipFile(ValidationRunConfig validationConfig, Long executionId) throws BusinessServiceException {
		File outputFolder;
		try{
			outputFolder = new File(FileUtils.getTempDirectoryPath(), "rvf_loader_data_" + executionId);
			LOGGER.info("MRCM output folder location = " + outputFolder.getAbsolutePath());
			if (outputFolder.exists()) {
				LOGGER.info("MRCM output folder already exists and will be deleted before recreating.");
				outputFolder.delete();
			}
			outputFolder.mkdir();
			ZipFileUtils.extractFilesFromZipToOneFolder(validationConfig.getLocalProspectiveFile(), outputFolder.getAbsolutePath());
		} catch (final IOException ex){
			final String errorMsg = String.format("Error while loading file %s.", validationConfig.getLocalProspectiveFile());
			LOGGER.error(errorMsg, ex);
			throw new BusinessServiceException(errorMsg, ex);
		}
		return outputFolder;
	}

	
}
