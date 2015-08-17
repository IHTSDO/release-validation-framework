package org.ihtsdo.rvf.execution.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
@Scope(value = "prototype")
public class ValidationRunner implements Runnable {
	
	private static final String UTF_8 = "UTF-8";

	public enum State { READY, RUNNING, FAILED, COMPLETE } 
	
	public static final String FAILURE_MESSAGE = "failureMessage";

	private final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
	
	private ValidationRunConfig validationConfig;
	
	private FileHelper s3Helper;
	
	private boolean initialized = false;
	
	@Resource
	private S3Client s3Client;
	
	@Autowired
	private StructuralTestRunner structuralTestRunner;
	
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	@Autowired
	private AssertionService assertionService;
	
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	@Autowired
	private final String bucketName;
	
	@Autowired
	private ResourceDataLoader resourceLoader;
	
	@Autowired
	private RvfDbScheduledEventGenerator scheduleEventGenerator;
	
	private String stateFilePath;
	private String resultsFilePath;
	private String progressFilePath;
	
	public ValidationRunner(final String bucketName) {
		this.bucketName = bucketName;
	}

	@Override
	public void run() {
		final Map<String , Object> responseMap = new LinkedHashMap<>();
		try {
			responseMap.put("Validation config", validationConfig);
			runValidation(responseMap);
		} catch (final Exception e) {
			final StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			final String failureMsg = "System Failure: " + e.getMessage() + " : " + errors.toString();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			logger.error("Exception thrown, writing as result",e);
			try {
				writeResults(responseMap, State.FAILED);
			} catch (final Exception e2) {
				//Can't even record the error to disk!  Lets hope Telemetry is working
				logger.error("Failed to record failure (which was: " + failureMsg + ") due to " + e2.getMessage());
			}
		}
	}

	private void runValidation(final Map<String , Object> responseMap) throws Exception {
		
		final Calendar startTime = Calendar.getInstance();
		logger.info(String.format("Started execution with runId [%1s] : ", validationConfig.getRunId()));
		writeState(State.RUNNING);
		
		// load the filename
		final String structureTestStartMsg = "Start structure testing for release file:" + validationConfig.getFile().getOriginalFilename();
		logger.info(structureTestStartMsg);
		writeProgress(structureTestStartMsg);
		boolean isFailed = structuralTestRunner.verifyZipFileStructure(responseMap, validationConfig.getProspectiveFile(), validationConfig.getRunId(), validationConfig.getManifestFile(), validationConfig.isWriteSucceses(), validationConfig.getUrl());
		if (isFailed) {
			writeResults(responseMap, State.FAILED);
			return;
		} else {
			isFailed = checkKnownVersion(validationConfig.getPrevIntReleaseVersion(),
										 validationConfig.getPreviousExtVersion(),
										 validationConfig.getExtensionDependencyVersion(),
										 responseMap);
			if (isFailed) {
				writeResults(responseMap, State.FAILED);
				return;
			}
			
			final String[] tokens = Files.getNameWithoutExtension(validationConfig.getFile().getOriginalFilename()).split("_");
			String prospectiveVersion = validationConfig.getRunId().toString();
			String prevReleaseVersion = validationConfig.getPrevIntReleaseVersion();
			final boolean isExtension = isExtension(validationConfig); 
			if (isExtension && !validationConfig.isFirstTimeRelease()) {
				//SnomedCT_Release-es_INT_20140430.zip
				//SnomedCT_SpanishRelease_INT_20141031.zip
				final String extensionName = tokens[1].replace("Release", "").replace("-", "").concat("edition_");
				prevReleaseVersion = validationConfig.getPreviousExtVersion();
				if (validationConfig.getPrevIntReleaseVersion() != null) {
					//previous extension release is being specified as already being merged, but we might have already done it anyway
					prevReleaseVersion = extensionName.toLowerCase() + validationConfig.getPreviousExtVersion();
					if (!releaseDataManager.isKnownRelease(prevReleaseVersion)) {
						final String startCombiningMsg = String.format("Combining previous releases:[%s],[%s] into: [%s]", validationConfig.getPrevIntReleaseVersion() , validationConfig.getPreviousExtVersion(), prevReleaseVersion);
						logger.info(startCombiningMsg);
						writeProgress(startCombiningMsg);
						final boolean isSuccess = releaseDataManager.combineKnownVersions(prevReleaseVersion, validationConfig.getPrevIntReleaseVersion(), validationConfig.getPreviousExtVersion());
						if (!isSuccess) {
							responseMap.put(FAILURE_MESSAGE, "Failed to combine known versions:" 
									+ validationConfig.getPrevIntReleaseVersion() + " and " + validationConfig.getPreviousExtVersion() + " into " + prevReleaseVersion);
							writeResults(responseMap, State.FAILED);
							return;
						}
					} else {
						logger.info("Skipping merge of {} with {} as already detected in database as {}",validationConfig.getPrevIntReleaseVersion(), validationConfig.getPreviousExtVersion(), prevReleaseVersion);
					}
				}
			} 
			writeProgress("Loading prospective file into DB.");
			List<String> rf2FilesLoaded = new ArrayList<>();
			if (isExtension) {
				uploadProspectiveVersion(prospectiveVersion, validationConfig.getExtensionDependencyVersion(), validationConfig.getProspectiveFile(), rf2FilesLoaded);
			} else {
				uploadProspectiveVersion(prospectiveVersion, null, validationConfig.getProspectiveFile(), rf2FilesLoaded);
			}
			responseMap.put("Total RF2 Files loaded", rf2FilesLoaded.size());
			responseMap.put("RF2 Files", rf2FilesLoaded);
			
			final String prospectiveSchema = releaseDataManager.getSchemaForRelease(prospectiveVersion);
			if (prospectiveSchema != null) {
				writeProgress("Loading resource data for prospective schema:" + prospectiveSchema);
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
			writeResults(responseMap, State.COMPLETE);
			//house keeping prospective version and combined previous extension 
			scheduleEventGenerator.createDropReleaseSchemaEvent(prospectiveSchema);
			if (isExtension && !validationConfig.isFirstTimeRelease()) {
				scheduleEventGenerator.createDropReleaseSchemaEvent(releaseDataManager.getSchemaForRelease(prevReleaseVersion));
			}
			// house keeping qa_result for the given run id
			scheduleEventGenerator.createQaResultDeleteEvent(validationConfig.getRunId());
		}
	}

	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependencyVersion() != null 
				&& !runConfig.getExtensionDependencyVersion().trim().isEmpty()) ? true : false;
	}

	public boolean init(final ValidationRunConfig config, final Map<String, String> responseMap) {
		setConfig(config);
		logger.info("Run config:" + config);
		//check assertion groups
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(config.getGroupsList());
		if (groups.size() != config.getGroupsList().size()) {
			final List<String> found = new ArrayList<>();
			for (final AssertionGroup group : groups) {
				found.add(group.getName());
			}
			final String groupNotFoundMsg = String.format("Assertion groups requested: %s but found in RVF: %s", config.getGroupsList(), found);
			responseMap.put(FAILURE_MESSAGE, groupNotFoundMsg);
			logger.warn("Invalid assertion groups requested." + groupNotFoundMsg);
			return false;
			
		}
		//Setting this before we actually start running to ensure we have access to storageLocation
		try {
			if (saveUploadedFiles(config, responseMap)) {
				writeState(State.READY);
				initialized = true;
			}
		} catch (final Exception e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to write Ready State to Storage Location due to " + e.getMessage());
		}
		return initialized;
	}
	
	/*
	 * The issue here is that spring cleans up Multipart files when Dispatcher is complete, so 
	 * we need to save off the file before we allow the parent thread to finish.
	 */
	private boolean saveUploadedFiles(final ValidationRunConfig config, final Map<String, String> responseMap) throws IOException {
		final String filename = config.getFile().getOriginalFilename();
		final File tempFile = File.createTempFile(filename, ".zip");
		tempFile.deleteOnExit();
		if (!filename.endsWith(".zip")) {
			responseMap.put(FAILURE_MESSAGE, "Post condition test package has to be zipped up");
			return false;
		}
		// must be a zip, save it off
		config.getFile().transferTo(tempFile);	
		config.setProspectiveFile(tempFile);
		config.setTestFileName(filename);
		return true;
	}
	
	private void writeResults(final Map<String , Object> responseMap, final State state) throws IOException, NoSuchAlgorithmException, DecoderException {
		final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
		final File temp = File.createTempFile("resultJson", ".tmp"); 
		try {
			try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),Charset.forName(UTF_8)))) {
				prettyGson.toJson(responseMap, bw);
				//Now copy to our S3 Location
			} 
			s3Helper.putFile(temp, resultsFilePath);
		}
		finally {
			temp.delete();
		}
		writeState(state);
	}
	
	private void writeState(final State state) throws IOException, NoSuchAlgorithmException, DecoderException {
		logger.info("RVF run {} setting state as {}", validationConfig.getRunId(), state.toString());
		writeToS3(state.name(), stateFilePath);
	}
	
	private void writeProgress(final String progress) {
		try {
			writeToS3(progress, progressFilePath);
		} catch (NoSuchAlgorithmException | IOException | DecoderException e) {
			logger.error("Failed to write progress to S3: " + progressFilePath);
		}
		
	}
	private void writeToS3(final String writeMe, final String targetPath) throws IOException, NoSuchAlgorithmException, DecoderException {
		//First write the data to a local temp file
		final File temp = File.createTempFile("tempfile", ".tmp"); 
		try {
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),Charset.forName(UTF_8)))) {
				bw.write(writeMe);
			}
			//Now copy to our S3 Location
			s3Helper.putFile(temp, targetPath);
		} finally {
			//And clean up
			temp.delete();
		}
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
		writeProgress("Start executing assertions...");
		 final List<TestRunItem> items = executeAssertions(executionConfig, resourceAssertions);
		final Set<Assertion> assertions = new HashSet<>();
		for (final AssertionGroup group : groups) {
			for (final Assertion assertion : assertionService.getAssertionsForGroup(group)) {
				assertions.add(assertion);
			}
		}
		logger.info("Total assertions to run: " + assertions.size());
		items.addAll(executeAssertions(executionConfig,assertions));
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
				writeProgress(String.format("[%1s] of [%2s] assertions are completed.", counter, assertions.size()));
			}
		}
		return results;
	}

	public void setConfig(final ValidationRunConfig config) {
		this.validationConfig = config;
		s3Helper = new FileHelper(bucketName, s3Client);
		stateFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "state.txt";
		resultsFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "results.json";
		progressFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "progress.txt";
	}
	
	public State getCurrentState() {
		State currentState = null;
		try {
			final InputStream is = s3Helper.getFileStream(stateFilePath);
			if (is == null) {
				logger.warn("Failed to find state file {}, in bucket {}", stateFilePath, bucketName);
			}
			final String stateStr = IOUtils.toString(is, UTF_8);
			currentState = State.valueOf(stateStr);
		} catch (final Exception e) {
			logger.warn("Failed to determine validation run state in file {} due to {}", stateFilePath, e.toString());
		}
		return currentState;
	}

	public void recoverResult(final Map<String, Object> responseMap) throws IOException {
		final InputStream is = s3Helper.getFileStream(resultsFilePath);
		Object jsonResults = null;
		if (is == null) {
			logger.warn("Failed to find results file {}, in bucket {}", stateFilePath, bucketName);
		} else {
			final Gson gson = new Gson();
			jsonResults = gson.fromJson(new InputStreamReader(is,Charset.forName(UTF_8)), Map.class);
		}
		if (jsonResults == null) {
			jsonResults = new String("Failed to recover results in " + resultsFilePath);
		}
		responseMap.put("RVF Validation Result", jsonResults);
	}

	public String recoverProgress() {
		final InputStream is = s3Helper.getFileStream(progressFilePath);
		String progressMsg = new String("Failed to read from " + progressFilePath);
		if (is == null) {
			logger.warn("Failed to find progress file {}, in bucket {}", progressFilePath, bucketName);
		} else {
			try {
				progressMsg = IOUtils.toString(is, UTF_8);
			} catch (final IOException e) {
				logger.warn("Failed to read data from progress file {}, in bucket {}", progressFilePath, bucketName);
			}
		}
		return progressMsg;
	}

}
