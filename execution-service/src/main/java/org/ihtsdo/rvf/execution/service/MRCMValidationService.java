package org.ihtsdo.rvf.execution.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.quality.validator.mrcm.Assertion;
import org.snomed.quality.validator.mrcm.ValidationRun;
import org.snomed.quality.validator.mrcm.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class MRCMValidationService {

	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;

	@Autowired
	private ResourceLoader cloudResourceLoader;

	private ResourceManager releaseSourceManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(MRCMValidationService.class);

	private static final String MRCM_PROSPECTIVE_FILE = "mrcm_prospective_file_";

	private static final String MRCM_DEPENDENCY_FILE = "mrcm_dependency_file_";

	@PostConstruct
	public void init() {
		releaseSourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
	}

	public void runMRCMAssertionTests(final ValidationStatusReport statusReport, ValidationRunConfig validationConfig, String effectiveDate, Long executionId) {
		File outputFolder = null;
		try {
			final long timeStart = System.currentTimeMillis();
			ValidationReport report = statusReport.getResultReport();
			ValidationService validationService = new ValidationService();
			ValidationRun validationRun = new ValidationRun(
					StringUtils.isNotBlank(effectiveDate) ? effectiveDate.replaceAll("-","") : effectiveDate
					, false);
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

			int maxFailureExports = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 10;
			final List<TestRunItem> warnedAssertions = new ArrayList<>();
			for(Assertion assertion : validationRun.getFailedAssertions()){
				testRunItem = createTestRunItemWithFailures(assertion, maxFailureExports);
				if(testRunItem != null) {
					warnedAssertions.add(testRunItem);
				}
			}

			final List<TestRunItem> failedAssertions = new ArrayList<>();
			for(Assertion assertion : validationRun.getFailedAssertions()){
				testRunItem = createTestRunItemWithFailures(assertion, maxFailureExports);
				if(testRunItem != null) {
					failedAssertions.add(testRunItem);
				}
			}

			report.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
			report.addFailedAssertions(failedAssertions);
			report.addWarningAssertions(warnedAssertions);
			report.addSkippedAssertions(skippedAssertions);
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
		testRunItem.setTestType(TestType.MRCM);
		testRunItem.setAssertionUuid(mrcmAssertion.getUuid());
		testRunItem.setAssertionText(mrcmAssertion.getAssertionText());
		testRunItem.setFailureCount(0L);
		testRunItem.setExtractResultInMillis(0L);
		return testRunItem;
	}

	private TestRunItem createTestRunItemWithFailures(Assertion mrcmAssertion, int failureExportMax) {
		int failureCount = mrcmAssertion.getCurrentViolatedConceptIds().size();
		if(failureCount == 0) return null;
		int firstNCount = failureCount >= failureExportMax ? failureExportMax : failureCount;
		TestRunItem testRunItem = createTestRunItem(mrcmAssertion);
		testRunItem.setFailureCount(Long.valueOf(failureCount));
		List<FailureDetail> failedDetails = new ArrayList(firstNCount);
		for(int i = 0; i < firstNCount; i++) {
			Long conceptId = mrcmAssertion.getCurrentViolatedConceptIds().get(i);
			failedDetails.add(new FailureDetail(String.valueOf(conceptId), mrcmAssertion.getAssertionText()));
		}
		testRunItem.setFirstNInstances(failedDetails);
		return testRunItem;
	}

	private File extractZipFile(ValidationRunConfig validationConfig, Long executionId) throws BusinessServiceException, RVFExecutionException {
		File outputFolder;
		try{
			outputFolder = new File(FileUtils.getTempDirectoryPath(), MRCM_PROSPECTIVE_FILE + executionId);
			LOGGER.info("Unzipped release folder location = " + outputFolder.getAbsolutePath());
			if (outputFolder.exists()) {
				LOGGER.info("Unzipped release folder already exists and will be deleted before recreating.");
				outputFolder.delete();
			}
			outputFolder.mkdir();

			ZipFileUtils.extractFilesFromZipToOneFolder(validationConfig.getLocalProspectiveFile(), outputFolder.getAbsolutePath());
			if (validationConfig.getExtensionDependency() != null && !validationConfig.isReleaseAsAnEdition()) {
				if(StringUtils.isBlank(validationConfig.getExtensionDependency()) || !validationConfig.getExtensionDependency().endsWith(".zip")) {
					throw new RVFExecutionException("MRCM validation cannot execute when Extension Dependency is empty or not a .zip file: " + validationConfig.getExtensionDependency());
				}
				InputStream dependencyStream = releaseSourceManager.readResourceStreamOrNullIfNotExists(validationConfig.getExtensionDependency());
				File dependencyFile = File.createTempFile(MRCM_DEPENDENCY_FILE, ".zip");
				OutputStream out = new FileOutputStream(dependencyFile);
				IOUtils.copy(dependencyStream, out);
				IOUtils.closeQuietly(dependencyStream);
				IOUtils.closeQuietly(out);
				ZipFileUtils.extractFilesFromZipToOneFolder(dependencyFile, outputFolder.getAbsolutePath());
			}
		} catch (final IOException ex){
			final String errorMsg = String.format("Error while loading file %s.", validationConfig.getLocalProspectiveFile());
			LOGGER.error(errorMsg, ex);
			throw new BusinessServiceException(errorMsg, ex);
		}
		return outputFolder;
	}

	
}
