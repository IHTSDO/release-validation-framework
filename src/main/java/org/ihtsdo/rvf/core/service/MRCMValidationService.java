package org.ihtsdo.rvf.core.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.sqs.service.dto.ConceptResult;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.ihtsdo.rvf.core.service.whitelist.WhitelistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.quality.validator.mrcm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.snomed.quality.validator.mrcm.SEPRefsetValidationService.SEPAssertionType;
@Service
public class MRCMValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MRCMValidationService.class);

	private static final String EXT_ZIP = ".zip";

	private static final String COMMA = ",";

	@Autowired
	private WhitelistService whitelistService;

	@Value("${rvf.assertion.whitelist.batchsize:1000}")
	private int whitelistBatchSize;

	public ValidationStatusReport runMRCMAssertionTests(final ValidationStatusReport statusReport, ValidationRunConfig validationConfig) {
		Set<String> extractedRF2FilesDirectory = new HashSet<>();
		try {
			boolean fullSnapshotRelease = !validationConfig.isRf2DeltaOnly() && StringUtils.isEmpty(validationConfig.getExtensionDependency());
			int maxFailureExports = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 100;
			String effectiveDate = StringUtils.isNotBlank(validationConfig.getEffectiveTime()) ? validationConfig.getEffectiveTime().replaceAll("-", "") : "";
			final long timeStart = System.currentTimeMillis();
			ValidationReport report = statusReport.getResultReport();
			ValidationService validationService = new ValidationService();

			Set<InputStream> snapshotsInputStream = new HashSet<>();

			InputStream testedReleaseFileStream = new FileInputStream(validationConfig.getLocalProspectiveFile());

			InputStream deltaInputStream = null;
			//If the validation is Delta validation, previous snapshot file must be loaded to snapshot files list.
			if (validationConfig.isRf2DeltaOnly()) {
				if (StringUtils.isBlank(validationConfig.getPreviousRelease()) || !validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
					throw new RVFExecutionException("MRCM validation cannot execute when Previous Release is empty or not a .zip file: " + validationConfig.getPreviousRelease());
				}
				InputStream previousStream = new FileInputStream(validationConfig.getLocalPreviousReleaseFile());
				snapshotsInputStream.add(previousStream);
				deltaInputStream = testedReleaseFileStream;
			} else {
				//If the validation is Snapshot validation, current file must be loaded to snapshot files list
				snapshotsInputStream.add(testedReleaseFileStream);
			}

			//Load the dependency package from S3 to snapshot files list before validating if the package is an MS extension and not an edition release
			//If the package is an MS edition, it is not necessary to load the dependency
			if (validationConfig.getExtensionDependency() != null && !validationConfig.isReleaseAsAnEdition() && !validationConfig.isStandAloneProduct()) {
				if (StringUtils.isBlank(validationConfig.getExtensionDependency()) || !validationConfig.getExtensionDependency().endsWith(EXT_ZIP)) {
					throw new RVFExecutionException("MRCM validation cannot execute when Extension Dependency is empty or not a .zip file: " + validationConfig.getExtensionDependency());
				}
				InputStream dependencyStream = new FileInputStream(validationConfig.getLocalDependencyReleaseFile());
				snapshotsInputStream.add(dependencyStream);
			}

			//Will filter the results based on component's module IDs if the package is an extension only
			Set<String> moduleIds = null;
			String moduleIdStr = validationConfig.getIncludedModules();
			if (StringUtils.isNotBlank(moduleIdStr)) {
				moduleIds = Sets.newHashSet(moduleIdStr.split(","));
			}

			//Unzip the release files
			for (InputStream inputStream : snapshotsInputStream) {
				File snapshotDirectory = new ReleaseImporter().unzipRelease(inputStream, ReleaseImporter.ImportType.SNAPSHOT);
				extractedRF2FilesDirectory.add(snapshotDirectory.getPath());
			}
			if (deltaInputStream != null) {
				File deltaDirectory = new ReleaseImporter().unzipRelease(deltaInputStream, ReleaseImporter.ImportType.DELTA);
				extractedRF2FilesDirectory.add(deltaDirectory.getPath());
			}

			ValidationRun validationRunner = new ValidationRun(null, null, false);
			validationRunner.setFullSnapshotRelease(fullSnapshotRelease);
			validationService.loadMRCM(extractedRF2FilesDirectory, validationRunner);

			ValidationRun validationRunnerInferredForm = getValidationRunGivenForm(effectiveDate, validationRunner, fullSnapshotRelease, moduleIds, ContentType.INFERRED);
			ValidationRun validationRunnerInStatedForm = getValidationRunGivenForm(effectiveDate, validationRunner, fullSnapshotRelease, moduleIds, ContentType.STATED);

			ExecutorService executorService = Executors.newCachedThreadPool();
			List<Future<Void>> tasks = new ArrayList<>();
			tasks.add(executorService.submit (() -> {
				validationService.validateRelease(extractedRF2FilesDirectory, validationRunnerInferredForm);
				return null;
			}));
			tasks.add(executorService.submit (() -> {
				validationService.validateRelease(extractedRF2FilesDirectory, validationRunnerInStatedForm);
				return null;
			}));
			for (Future<Void> task : tasks) {
				task.get(); // Use Future.get to receive any exceptions throwing from Thread
			}

			if (!whitelistService.isWhitelistDisabled()) {
				checkWhitelistItems(validationRunnerInferredForm);
				checkWhitelistItems(validationRunnerInStatedForm);
			}

			extractTestResults(maxFailureExports, report, validationRunnerInferredForm, ContentType.INFERRED);
			extractTestResults(maxFailureExports, report, validationRunnerInStatedForm, ContentType.STATED);
			report.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
			executorService.shutdown();
		} catch (Exception ex) {
			String message = "MRCM validation has stopped";
			LOGGER.error(message, ex);
			message = ex.getMessage() != null ? message + " due to error: " + ex.getMessage() : message;
			statusReport.addFailureMessage(message);
		} finally {
			if (!CollectionUtils.isEmpty(extractedRF2FilesDirectory)) {
				extractedRF2FilesDirectory.forEach(s -> FileUtils.deleteQuietly(new File(s)));
			}
		}
		return statusReport;
	}

	private void checkWhitelistItems(ValidationRun report) throws RestClientException {
		for (final Assertion assertion : report.getAssertionsWithWarning()) {
			checkWhitelistItemAgainstEachAssertion(assertion);
		}
		for (final Assertion assertion : report.getFailedAssertions()) {
			checkWhitelistItemAgainstEachAssertion(assertion);
		}
	}

	private void checkWhitelistItemAgainstEachAssertion(Assertion assertion) throws RestClientException {
		// checking whitelist
		List<ConceptResult> currentViolatedConcepts = assertion.getCurrentViolatedConcepts();
		if (!assertion.getCurrentViolatedConceptIds().isEmpty()) {
			List<Long> newViolatedConceptIds = new ArrayList<>();
			for (List<ConceptResult> batch : Iterables.partition(currentViolatedConcepts, whitelistBatchSize)) {
				// Convert to WhitelistItem
				List<WhitelistItem> whitelistItems = batch.stream().map(conceptResult -> new WhitelistItem(assertion.getUuid().toString(), conceptResult.getId(), conceptResult.getId(), getAdditionalFields(conceptResult))).toList();

				// Send to Authoring acceptance gateway
				LOGGER.info("Checking {} whitelist items in batch", whitelistItems.size());
				List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

				// Find the failures which are not in the whitelisted item
				newViolatedConceptIds.addAll(batch.stream().filter(conceptResult -> whitelistedItems.stream().noneMatch(whitelistedItem -> String.valueOf(conceptResult.getId()).equals(whitelistedItem.getConceptId()))).map(conceptResult -> Long.parseLong(conceptResult.getId())).toList());
			}
			List<ConceptResult> newViolatedConcepts = new ArrayList<>(currentViolatedConcepts.stream().filter(concept -> newViolatedConceptIds.contains(Long.valueOf(concept.getId()))).toList());
			assertion.setCurrentViolatedConceptIds(newViolatedConceptIds);
			assertion.setCurrentViolatedConcepts(newViolatedConcepts);
		}
	}

	private ValidationRun getValidationRunGivenForm(String effectiveDate, ValidationRun validationRunner, boolean fullSnapshotRelease, Set<String> moduleIds, ContentType contentType) {
		ValidationRun validationRun = new ValidationRun(effectiveDate, contentType, false);
		validationRun.setModuleIds(moduleIds);
		validationRun.setFullSnapshotRelease(fullSnapshotRelease);
		validationRun.setMRCMDomains(validationRunner.getMRCMDomains());
		validationRun.setAttributeRangesMap(validationRunner.getAttributeRangesMap());
		validationRun.setUngroupedAttributes(validationRunner.getUngroupedAttributes());
		validationRun.setConceptsUsedInMRCMTemplates(validationRunner.getConceptsUsedInMRCMTemplates());
		validationRun.setLateralizableRefsetMembers(validationRunner.getLateralizableRefsetMembers());
		validationRun.setAnatomyStructureAndEntireRefsets(validationRunner.getAnatomyStructureAndEntireRefsets());
		validationRun.setAnatomyStructureAndPartRefsets(validationRunner.getAnatomyStructureAndPartRefsets());
		return validationRun;
	}

	private void extractTestResults(int maxFailureExports, ValidationReport report, ValidationRun validationRun, ContentType contentType) {
		TestRunItem testRunItem;
		final List<TestRunItem> passedAssertions = new ArrayList<>();
		for (Assertion assertion : validationRun.getPassedAssertions()) {
			testRunItem = createTestRunItem(assertion, contentType);
			passedAssertions.add(testRunItem);
		}

		final List<TestRunItem> skippedAssertions = new ArrayList<>();
		for (Assertion assertion : validationRun.getSkippedAssertions()) {
			testRunItem = createTestRunItem(assertion, contentType);
			skippedAssertions.add(testRunItem);
		}

		final List<TestRunItem> warnedAssertions = new ArrayList<>();
		for (Assertion assertion : validationRun.getAssertionsWithWarning()) {
			testRunItem = createTestRunItemWithFailures(assertion, contentType, maxFailureExports);
			if (testRunItem != null) {
				warnedAssertions.add(testRunItem);
			}
		}

		final List<TestRunItem> failedAssertions = new ArrayList<>();
		for (Assertion assertion : validationRun.getFailedAssertions()) {
			testRunItem = createTestRunItemWithFailures(assertion, contentType, maxFailureExports);
			if (testRunItem != null) {
				failedAssertions.add(testRunItem);
			}
		}

		final List<TestRunItem> incompleteAssertions = new ArrayList<>();
		for (Assertion assertion : validationRun.getIncompleteAssertions()) {
			testRunItem = createTestRunItem(assertion, contentType);
			testRunItem.setFailureCount(-1L);
			testRunItem.setFailureMessage(assertion.getFailureMessage());
			incompleteAssertions.add(testRunItem);
		}

		report.addFailedAssertions(failedAssertions);
		report.addWarningAssertions(warnedAssertions);
		report.addIncompleteAssesrtions(incompleteAssertions);
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
		if (failureCount == 0) {
			if (mrcmAssertion.getCurrentViolatedReferenceSetMembers() != null) {
				failureCount = mrcmAssertion.getCurrentViolatedReferenceSetMembers().size();
				if (failureCount == 0) {
					return null;
				}
			} else {
				return null;
			}
		}
		int firstNCount = Math.min(failureCount, failureExportMax);

		TestRunItem testRunItem = createTestRunItem(mrcmAssertion, contentType);
		testRunItem.setFailureCount((long) failureCount);
		List<FailureDetail> failedDetails = new ArrayList<>(firstNCount);
		if (LateralizableRefsetValidationService.ASSERTION_ID_MEMBERS_NEED_TO_BE_REMOVED_FROM_LATERALIZABLE_REFSET.equals(mrcmAssertion.getUuid().toString())) {
			for (int i = 0; i < firstNCount; i++) {
				ConceptResult conceptResult = mrcmAssertion.getCurrentViolatedConcepts().get(i);
				String conceptId = conceptResult.getId();
				failedDetails.add(new FailureDetail(conceptId, String.format(mrcmAssertion.getDetails(), conceptId, "removed from"), conceptResult.getFsn()).setFullComponent(getAdditionalFields(conceptResult)).setComponentId(conceptId));
			}
		} else if (LateralizableRefsetValidationService.ASSERTION_ID_CONCEPTS_NEED_TO_BE_ADDED_TO_LATERALIZABLE_REFSET.equals(mrcmAssertion.getUuid().toString())) {
			for (int i = 0; i < firstNCount; i++) {
				ConceptResult conceptResult = mrcmAssertion.getCurrentViolatedConcepts().get(i);
				String conceptId = conceptResult.getId();
				failedDetails.add(new FailureDetail(conceptId, String.format(mrcmAssertion.getDetails(), conceptId, "added to"), conceptResult.getFsn()).setFullComponent(getAdditionalFields(conceptResult)).setComponentId(conceptId));
			}
		} else if (Arrays.stream(SEPAssertionType.values()).map(SEPAssertionType::getUuid).collect(Collectors.toSet()).contains(mrcmAssertion.getUuid().toString())) {
			for (int i = 0; i < firstNCount; i++) {
				ConceptResult conceptResult = mrcmAssertion.getCurrentViolatedConcepts().get(i);
				String conceptId = conceptResult.getId();
				failedDetails.add(new FailureDetail(conceptId, String.format(mrcmAssertion.getDetails(), conceptId), conceptResult.getFsn()).setFullComponent(getAdditionalFields(conceptResult)).setComponentId(conceptId));
			}
		} else {
			for (int i = 0; i < firstNCount; i++) {
				ConceptResult concept = mrcmAssertion.getCurrentViolatedConcepts().get(i);
				String conceptId = concept.getId();
				failedDetails.add(new FailureDetail(conceptId, mrcmAssertion.getDetails(), concept.getFsn()).setFullComponent(getAdditionalFields(concept)).setComponentId(conceptId));
			}
		}

		testRunItem.setFirstNInstances(failedDetails);
		return testRunItem;
	}

	private String getAdditionalFields(ConceptResult conceptResult) {
		if (conceptResult == null) {
			return null;
		}

		String moduleId = conceptResult.getModuleId();
		String definitionStatusId = conceptResult.getDefinitionStatusId();
		if (moduleId == null || definitionStatusId == null) {
			return null;
		}

		return (conceptResult.isActive() ? "1" : "0") + COMMA + moduleId + COMMA + definitionStatusId;
	}
}
