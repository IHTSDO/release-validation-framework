package org.ihtsdo.rvf.execution.service;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.quality.validator.mrcm.Assertion;
import org.snomed.quality.validator.mrcm.ContentType;
import org.snomed.quality.validator.mrcm.ValidationRun;
import org.snomed.quality.validator.mrcm.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MRCMValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MRCMValidationService.class);

	private static final String EXT_ZIP = ".zip";

	public enum CharacteristicType { inferred, stated }

	public ValidationStatusReport runMRCMAssertionTests(final ValidationStatusReport statusReport, ValidationRunConfig validationConfig) {
		Set<String> extractedRF2FilesDirectory = new HashSet<>();
		try {
			boolean fullSnapshotRelease = !validationConfig.isRf2DeltaOnly() && StringUtils.isEmpty(validationConfig.getExtensionDependency());
			int maxFailureExports = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 100;
			String effectiveDate = StringUtils.isNotBlank(validationConfig.getEffectiveTime()) ? validationConfig.getEffectiveTime().replaceAll("-","") : "";
			final long timeStart = System.currentTimeMillis();
			ValidationReport report = statusReport.getResultReport();
			ValidationService validationService = new ValidationService();

			Set<InputStream> snapshotsInputStream = new HashSet<>();

			InputStream testedReleaseFileStream = new FileInputStream(validationConfig.getLocalProspectiveFile());

			InputStream deltaInputStream = null;
			//If the validation is Delta validation, previous snapshot file must be loaded to snapshot files list.
			if (validationConfig.isRf2DeltaOnly()) {
				if(StringUtils.isBlank(validationConfig.getPreviousRelease()) || !validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
					throw new RVFExecutionException("MRCM validation cannot execute when Previous Release is empty or not a .zip file: " + validationConfig.getPreviousRelease());
				}
				InputStream previousStream = new FileInputStream(validationConfig.getLocalPreviousReleaseFile());
				snapshotsInputStream.add(previousStream);
				deltaInputStream = testedReleaseFileStream;
			} else {
				//If the validation is Snapshot validation, current file must be loaded to snapshot files list
				snapshotsInputStream.add(testedReleaseFileStream);
			}

			//Load the dependency package from S3 to snapshot files list before validating if the package is a MS extension and not an edition release
			//If the package is an MS edition, it is not necessary to load the dependency
			Set<String> moduleIds = null;
			if (validationConfig.getExtensionDependency() != null && !validationConfig.isReleaseAsAnEdition()) {
				if(StringUtils.isBlank(validationConfig.getExtensionDependency()) || !validationConfig.getExtensionDependency().endsWith(EXT_ZIP)) {
					throw new RVFExecutionException("MRCM validation cannot execute when Extension Dependency is empty or not a .zip file: " + validationConfig.getExtensionDependency());
				}
				InputStream dependencyStream = new FileInputStream(validationConfig.getLocalDependencyReleaseFile());
				snapshotsInputStream.add(dependencyStream);

				//Will filter the results based on component's module IDs if the package is an extension only
				String moduleIdStr = validationConfig.getIncludedModules();
				if(StringUtils.isNotBlank(moduleIdStr)) {
					moduleIds = Sets.newHashSet(moduleIdStr.split(","));
				}
			}

			//Unzip the release files
			for (InputStream inputStream : snapshotsInputStream) {
				File snapshotDirectory = new ReleaseImporter().unzipRelease(inputStream, ReleaseImporter.ImportType.SNAPSHOT);
				extractedRF2FilesDirectory.add(snapshotDirectory.getPath());
			}
			if(deltaInputStream != null) {
				File deltaDirectory = new ReleaseImporter().unzipRelease(deltaInputStream, ReleaseImporter.ImportType.DELTA);
				extractedRF2FilesDirectory.add(deltaDirectory.getPath());
			}

			ValidationRun validationRunner = new ValidationRun(null, null, false);
			validationRunner.setFullSnapshotRelease(fullSnapshotRelease);
			validationService.loadMRCM(extractedRF2FilesDirectory, validationRunner);

			ValidationRun validationRunnerInferredForm = getValidationRunGivenForm(effectiveDate, validationRunner, fullSnapshotRelease, moduleIds, ContentType.INFERRED);
			ValidationRun validationRunnerInStatedForm = getValidationRunGivenForm(effectiveDate, validationRunner, fullSnapshotRelease, moduleIds, ContentType.STATED);

			Set<Callable<Void>> callables = new HashSet<>();
			callables.add(() -> {
				validationService.validateRelease(extractedRF2FilesDirectory, validationRunnerInferredForm);
				return null;
			});
			callables.add(() -> {
				validationService.validateRelease(extractedRF2FilesDirectory, validationRunnerInStatedForm);
				return null;
			});
			ExecutorService executorService = Executors.newCachedThreadPool();
			executorService.invokeAll(callables);

			extractTestResults(maxFailureExports, report, validationRunnerInferredForm, ContentType.INFERRED);
			extractTestResults(maxFailureExports, report, validationRunnerInStatedForm, ContentType.STATED);
			report.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
			executorService.shutdown();
		} catch (Exception ex) {
			String message = "MRCM validation has stopped";
			LOGGER.error(message, ex);
			message = ex.getMessage() != null ? message + " due to error: " + ex.getMessage() : message;
			statusReport.addFailureMessage(message);
		}  finally {
			if(!CollectionUtils.isEmpty(extractedRF2FilesDirectory)) {
				extractedRF2FilesDirectory.forEach(s -> FileUtils.deleteQuietly(new File(s)));
			}
		}
		return  statusReport;
	}

	private ValidationRun getValidationRunGivenForm(String effectiveDate, ValidationRun validationRunner, boolean fullSnapshotRelease, Set<String> moduleIds, ContentType contentType) {
		ValidationRun validationRun = new ValidationRun(effectiveDate, contentType, false);
		validationRun.setModuleIds(moduleIds);
		validationRun.setFullSnapshotRelease(fullSnapshotRelease);
		validationRun.setMRCMDomains(validationRunner.getMRCMDomains());
		validationRun.setAttributeRangesMap(validationRunner.getAttributeRangesMap());
		validationRun.setUngroupedAttributes(validationRunner.getUngroupedAttributes());
		validationRun.setConceptsUsedInMRCMTemplates(validationRunner.getConceptsUsedInMRCMTemplates());
		return validationRun;
	}

	private void extractTestResults(int maxFailureExports, ValidationReport report, ValidationRun validationRun, ContentType contentType) {
		TestRunItem testRunItem;
		final List<TestRunItem> passedAssertions = new ArrayList<>();
		for(Assertion assertion : validationRun.getCompletedAssertions()){
			testRunItem = createTestRunItem(assertion, contentType);
			passedAssertions.add(testRunItem);
		}

		final List<TestRunItem> skippedAssertions = new ArrayList<>();
		for(Assertion assertion : validationRun.getSkippedAssertions()){
			testRunItem = createTestRunItem(assertion, contentType);
			skippedAssertions.add(testRunItem);
		}

		final List<TestRunItem> warnedAssertions = new ArrayList<>();
		for(Assertion assertion : validationRun.getAssertionsWithWarning()){
			testRunItem = createTestRunItemWithFailures(assertion, contentType, maxFailureExports);
			if(testRunItem != null) {
				warnedAssertions.add(testRunItem);
			}
		}

		final List<TestRunItem> failedAssertions = new ArrayList<>();
		for(Assertion assertion : validationRun.getFailedAssertions()){
			testRunItem = createTestRunItemWithFailures(assertion, contentType, maxFailureExports);
			if(testRunItem != null) {
				failedAssertions.add(testRunItem);
			}
		}

		report.addFailedAssertions(failedAssertions);
		report.addWarningAssertions(warnedAssertions);
		report.addSkippedAssertions(skippedAssertions);
		report.addPassedAssertions(passedAssertions);
	}

	private TestRunItem createTestRunItem(Assertion mrcmAssertion, ContentType contentType) {
		TestRunItem testRunItem = new TestRunItem();
		testRunItem.setTestType(TestType.MRCM);
		testRunItem.setAssertionUuid(mrcmAssertion.getUuid());
		testRunItem.setAssertionText((ContentType.STATED.equals(contentType) ? "STATED FORM" : "INFERRED FORM") + ": " + mrcmAssertion.getAssertionText());
		testRunItem.setFailureCount(0L);
		testRunItem.setExtractResultInMillis(0L);
		return testRunItem;
	}

	private TestRunItem createTestRunItemWithFailures(Assertion mrcmAssertion, ContentType contentType, int failureExportMax) {
		int failureCount = mrcmAssertion.getCurrentViolatedConceptIds().size();
		if(failureCount == 0) return null;
		int firstNCount = Math.min(failureCount, failureExportMax);
		TestRunItem testRunItem = createTestRunItem(mrcmAssertion, contentType);
		testRunItem.setFailureCount((long) failureCount);
		List<FailureDetail> failedDetails = new ArrayList(firstNCount);
		for(int i = 0; i < firstNCount; i++) {
			ConceptResult concept = mrcmAssertion.getCurrentViolatedConcepts().get(i);
			failedDetails.add(new FailureDetail(concept.getId(), mrcmAssertion.getDetails(), concept.getFsn()));
		}
		testRunItem.setFirstNInstances(failedDetails);
		return testRunItem;
	}
}
