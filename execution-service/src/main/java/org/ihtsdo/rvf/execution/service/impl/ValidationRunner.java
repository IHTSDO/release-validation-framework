package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidationRunner {
	
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
	
	@Autowired
	private ResourceDataLoader resourceLoader;
	
	@Autowired
	private RvfDbScheduledEventGenerator scheduleEventGenerator;
	
	@Autowired
	private ValidationReportService reportService;
	
	
	private ExecutorService executorService = Executors.newCachedThreadPool();
	
	
	public void run(ValidationRunConfig validationConfig) {
		reportService.init(validationConfig);
		final Map<String , Object> responseMap = new LinkedHashMap<>();
		try {
			responseMap.put("Validation config", validationConfig);
			runValidation(responseMap, validationConfig);
		} catch (final Exception e) {
			final StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			final String failureMsg = "System Failure: " + e.getMessage() + " : " + errors.toString();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			logger.error("Exception thrown, writing as result",e);
			try {
				reportService.writeResults(responseMap, State.FAILED);
			} catch (final Exception e2) {
				//Can't even record the error to disk!  Lets hope Telemetry is working
				logger.error("Failed to record failure (which was: " + failureMsg + ") due to " + e2.getMessage());
			}
		} finally {
			FileUtils.deleteQuietly(validationConfig.getProspectiveFile());
		}
	}

	private void runValidation(final Map<String , Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		
		final Calendar startTime = Calendar.getInstance();
		logger.info(String.format("Started execution with runId [%1s] : ", validationConfig.getRunId()));
		// load the filename
		final String structureTestStartMsg = "Start structure testing for release file:" + validationConfig.getTestFileName();
		logger.info(structureTestStartMsg);
		reportService.writeProgress(structureTestStartMsg);
		reportService.writeState(State.RUNNING);
		boolean isFailed = structuralTestRunner.verifyZipFileStructure(responseMap, validationConfig.getProspectiveFile(), validationConfig.getRunId(), validationConfig.getManifestFileFullPath(), validationConfig.isWriteSucceses(), validationConfig.getUrl());
		if (isFailed) {
			reportService.writeResults(responseMap, State.FAILED);
			return;
		} else {
			isFailed = checkKnownVersion(validationConfig.getPrevIntReleaseVersion(),
										 validationConfig.getPreviousExtVersion(),
										 validationConfig.getExtensionDependencyVersion(),
										 responseMap);
			if (isFailed) {
				reportService.writeResults(responseMap, State.FAILED);
				return;
			}
			
			String prospectiveVersion = validationConfig.getRunId().toString();
			String prevReleaseVersion = validationConfig.getPrevIntReleaseVersion();
			final boolean isExtension = isExtension(validationConfig); 
			String combinedVersionName = null;
			if (isExtension && !validationConfig.isFirstTimeRelease()) {
				//SnomedCT_Release-es_INT_20140430.zip
				//SnomedCT_SpanishRelease_INT_20141031.zip
				if (validationConfig.getPrevIntReleaseVersion() != null) {
					combinedVersionName = validationConfig.getPreviousExtVersion() + "_" + validationConfig.getPreviousExtVersion() + "_" + validationConfig.getRunId();
					prevReleaseVersion = combinedVersionName;
					final String startCombiningMsg = String.format("Combining previous releases:[%s],[%s] into: [%s]", validationConfig.getPrevIntReleaseVersion() , validationConfig.getPreviousExtVersion(), combinedVersionName);
					logger.info(startCombiningMsg);
					reportService.writeProgress(startCombiningMsg);
					final boolean isSuccess = releaseDataManager.combineKnownVersions(combinedVersionName, validationConfig.getPrevIntReleaseVersion(), validationConfig.getPreviousExtVersion());
					if (!isSuccess) {
						responseMap.put(FAILURE_MESSAGE, "Failed to combine known versions:" 
								+ validationConfig.getPrevIntReleaseVersion() + " and " + validationConfig.getPreviousExtVersion() + " into " + combinedVersionName);
						reportService.writeResults(responseMap, State.FAILED);
						String schemaName = releaseDataManager.getSchemaForRelease(combinedVersionName);
						if (schemaName != null) {
							scheduleEventGenerator.createDropReleaseSchemaEvent(schemaName);
							releaseDataManager.dropVersion(combinedVersionName);
						}
						return;
					}
				}
			} 
			reportService.writeProgress("Loading prospective file into DB.");
			List<String> rf2FilesLoaded = new ArrayList<>();
			if (isExtension) {
				uploadProspectiveVersion(prospectiveVersion, validationConfig.getExtensionDependencyVersion(), validationConfig.getProspectiveFile(), rf2FilesLoaded);
			} else {
				uploadProspectiveVersion(prospectiveVersion, null, validationConfig.getProspectiveFile(), rf2FilesLoaded);
			}
			responseMap.put("Total RF2 Files loaded", rf2FilesLoaded.size());
			Collections.sort(rf2FilesLoaded);
			responseMap.put("RF2 Files", rf2FilesLoaded);
			
			final String prospectiveSchema = releaseDataManager.getSchemaForRelease(prospectiveVersion);
			if (prospectiveSchema != null) {
				reportService.writeProgress("Loading resource data for prospective schema:" + prospectiveSchema);
				resourceLoader.loadResourceData(prospectiveSchema);
				logger.info("completed loading resource data for schema:" + prospectiveSchema);
			}
			final ExecutionConfig executionConfig = new ExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
			executionConfig.setProspectiveVersion(prospectiveVersion);
			executionConfig.setPreviousVersion(prevReleaseVersion);
			executionConfig.setGroupNames(validationConfig.getGroupsList());
			//default to 10
			executionConfig.setFailureExportMax(10);
			if (validationConfig.getFailureExportMax() != null) {
				executionConfig.setFailureExportMax(validationConfig.getFailureExportMax());
			}
			runAssertionTests(executionConfig,responseMap);
			final Calendar endTime = Calendar.getInstance();
			final long timeTaken = (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 60000;
			logger.info(String.format("Finished execution with runId : [%1s] in [%2s] minutes ", validationConfig.getRunId(), timeTaken));
			responseMap.put("Start time", startTime.getTime());
			responseMap.put("End time", endTime.getTime());
			reportService.writeResults(responseMap, State.COMPLETE);
			//house keeping prospective version and combined previous extension 
			scheduleEventGenerator.createDropReleaseSchemaEvent(prospectiveSchema);
			releaseDataManager.dropVersion(prospectiveVersion);
			if (combinedVersionName != null) {
				scheduleEventGenerator.createDropReleaseSchemaEvent(releaseDataManager.getSchemaForRelease(combinedVersionName));
				releaseDataManager.dropVersion(combinedVersionName);
			}
			// house keeping qa_result for the given run id
			scheduleEventGenerator.createQaResultDeleteEvent(validationConfig.getRunId());
			
		}
	}

	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependencyVersion() != null 
				&& !runConfig.getExtensionDependencyVersion().trim().isEmpty()) ? true : false;
	}
	
	/*private void combineKnownVersions(final String combinedVersion, final String firstKnown, final String secondKnown) {
		logger.info("Start combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
		final File firstZipFile = releaseDataManager.getZipFileForKnownRelease(firstKnown);
		final File secondZipFile = releaseDataManager.getZipFileForKnownRelease(secondKnown);
		releaseDataManager.loadSnomedData(combinedVersion, firstZipFile , secondZipFile);
		logger.info("Complete combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
	}*/

	private void runAssertionTests(final ExecutionConfig executionConfig, final Map<String, Object> responseMap) throws IOException {
		final long timeStart = System.currentTimeMillis();
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(executionConfig.getGroupNames());
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		final List<Assertion> resourceAssertions = assertionService.getResourceAssertions();
		logger.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
		reportService.writeProgress("Start executing assertions...");
		 final List<TestRunItem> items = executeAssertions(executionConfig, resourceAssertions);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			for (final Assertion assertion : assertionService.getAssertionsForGroup(group)) {
				assertions.add(assertion);
			}
		}
		logger.info("Total assertions to run: " + assertions.size());
		items.addAll(executeAssertionsConcurrently(executionConfig,assertions));
//		items.addAll(executeAssertions(executionConfig,assertions));
		//failed tests
		final List<TestRunItem> failedItems = new ArrayList<>();
		for (final TestRunItem item : items) {
			if (item.getFailureCount() != 0) {
				failedItems.add(item);
			}
		}
		final long timeEnd = System.currentTimeMillis();
		final ValidationReport report = new ValidationReport(TestType.SQL);
		report.setExecutionId(executionConfig.getExecutionId());
		report.setTotalTestsRun(items.size());
		report.setTimeTakenInSeconds((timeEnd - timeStart) / 1000);
		report.setTotalFailures(failedItems.size());
		report.setFailedAssertions(failedItems);
		items.removeAll(failedItems);
		report.setPassedAssertions(items);
		responseMap.put(report.getTestType().name() + " test result", report);
		
	}

	private boolean checkKnownVersion(final String prevIntReleaseVersion, final String previousExtVersion, 
			final String extensionBaseLine, final Map<String, Object> responseMap) {
		logger.debug("Checking known versions...");
		if (previousExtVersion != null) {
			if (extensionBaseLine == null) {
				responseMap.put(FAILURE_MESSAGE, "PreviousExtensionVersion is :" 
						+ prevIntReleaseVersion + " but extension release base line has not been specified.");
				return true;
			}
		}
		if (prevIntReleaseVersion == null && previousExtVersion == null && extensionBaseLine == null) {
			responseMap.put(FAILURE_MESSAGE, "None of the known release version is specified");
			return true;
		}
		boolean isFailed = false;
		if (prevIntReleaseVersion != null && !prevIntReleaseVersion.isEmpty()) {
			if (!isKnownVersion(prevIntReleaseVersion, responseMap)) {
				isFailed = true;
			}
		}
		if (previousExtVersion != null && !previousExtVersion.isEmpty()) {
			if (!isKnownVersion(previousExtVersion, responseMap)) {
				isFailed = true;
			}
		}
		if (extensionBaseLine != null && !extensionBaseLine.isEmpty()) {
			if (!isKnownVersion(extensionBaseLine, responseMap)) {
				isFailed = true;
			}
		}
		return isFailed;
	}

	private void uploadProspectiveVersion(final String prospectiveVersion, final String knownVersion, final File tempFile, 
			List<String> rf2FilesLoaded) throws ConfigurationException, BusinessServiceException {
		
		if (knownVersion != null && !knownVersion.trim().isEmpty()) {
			logger.info(String.format("Baseline verison: [%1s] will be combined with prospective release file: [%2s]", knownVersion, tempFile.getName()));
			//load them together here as opposed to clone the existing DB so that to make sure it is clean.
			String versionDate = knownVersion;
			if (knownVersion.length() > 8) {
				versionDate = knownVersion.substring(knownVersion.length() - 8);
			}
			final File preLoadedZipFile = releaseDataManager.getZipFileForKnownRelease(versionDate);
			if (preLoadedZipFile != null) {
				logger.info("Start loading release version {} with release file {} and baseline {}", 
						prospectiveVersion, tempFile.getName(), preLoadedZipFile.getName());
				releaseDataManager.loadSnomedData(prospectiveVersion,rf2FilesLoaded, tempFile, preLoadedZipFile);
			} else {
				throw new ConfigurationException("Can't find the cached release zip file for known version: " + versionDate);
			}
		} else {
			logger.info("Start loading release version {} with release file {}", prospectiveVersion, tempFile.getName());
			releaseDataManager.loadSnomedData(prospectiveVersion,rf2FilesLoaded, tempFile);
		}
		logger.info("Completed loading release version {}", prospectiveVersion);
	}

	private boolean isKnownVersion(final String vertionToCheck, final Map<String, Object> responseMap) {
		if (!releaseDataManager.isKnownRelease(vertionToCheck)) {
			// the previous published release must already be present in database, otherwise we throw an error!
			responseMap.put("type", "post");
			final String errorMsg = "Please load published release data in RVF first for version: " + vertionToCheck;
			responseMap.put(FAILURE_MESSAGE, errorMsg);
			logger.info(errorMsg);
			return false;
		}
		return true;
	} 
	


	private List<TestRunItem> executeAssertionsConcurrently(final ExecutionConfig executionConfig, final Collection<Assertion> assertions) {
		
		final List<Future<Collection<TestRunItem>>> tasks = new ArrayList<>();
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList<Assertion>();
			}
			batch.add(assertion);
			if (counter % 10 == 0 || counter == assertions.size()) {
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
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are started.", counter, assertions.size()));
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


	private List<TestRunItem> executeAssertions(final ExecutionConfig executionConfig, final Collection<Assertion> assertions) {
		
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		for (final Assertion assertion: assertions) {
			logger.info(String.format("Started executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
			results.addAll(assertionExecutionService.executeAssertion(assertion, executionConfig));
			logger.info(String.format("Finished executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
			counter++;
			if (counter % 10 == 0) {
				//reporting every 10 assertions
				reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()));
			}
		}
		reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()));
		return results;
	}
}
