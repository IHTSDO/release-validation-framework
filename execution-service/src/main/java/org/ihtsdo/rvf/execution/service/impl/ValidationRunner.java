package org.ihtsdo.rvf.execution.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

@Service
@Scope(value = "prototype")
public class ValidationRunner implements Runnable{
	
	public enum State { READY, RUNNING, FAILED, COMPLETE } 
	
	public static final String FAILURE_MESSAGE = "failureMessage";

	private final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
	
	private ValidationRunConfig config;
	
	private FileHelper s3Helper;
	
	boolean initialized = false;
	
	@Resource
	S3Client s3Client;
	
	@Autowired
	StructuralTestRunner structuralTestRunner;
	
	@Autowired
	ReleaseDataManager releaseDataManager;
	
	@Autowired
	AssertionService assertionService;
	
	@Autowired
	AssertionExecutionService assertionExecutionService;
	
	@Autowired
	String bucketName;
	
	private String stateFilePath;
	private String resultsFilePath;
	
	public ValidationRunner (String bucketName) {
		this.bucketName = bucketName;
	}

	@Override
	public void run() {
		final Map<String , Object> responseMap = new HashMap<>();
		try {
			runValidation(responseMap);
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String failureMsg = "System Failure: " + e.getMessage() + " : " + errors.toString();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			logger.error("Exception thrown, writing as result",e);
			try {
				writeResults(responseMap, State.FAILED);
			} catch (Exception e2) {
				//Can't even record the error to disk!  Lets hope Telemetry is working
				logger.error("Failed to record failure (which was: " + failureMsg + ") due to " + e2.getMessage() );
			}
		}
	}

	private void runValidation (Map<String , Object> responseMap) throws Exception {
		
		final Calendar startTime = Calendar.getInstance();
		logger.info(String.format("Started execution with runId [%1s] : ", config.getRunId()));
		writeState(State.RUNNING);
		// load the filename
		logger.info("Loading file: {}", config.getFile().getOriginalFilename());

		boolean isFailed = structuralTestRunner.verifyZipFileStructure(responseMap, config.getProspectiveFile(), config.getRunId(), config.getManifestFile(), config.isWriteSucceses(), config.getUrl());
		if (isFailed) {
			writeResults(responseMap, State.FAILED);
			return;
		} else {
			isFailed = checkKnownVersion(config.getPrevIntReleaseVersion(),
										 config.getPreviousExtVersion(),
										 config.getExtensionBaseLine(),
										 responseMap);
			if (isFailed) {
				writeResults(responseMap, State.FAILED);
				return;
			}
			// TODO We could move all the following code to another thread using TaskExecutor and return the URL for 
			//assertion results something like: https://rvf.ihtsdotools.org/api/v1/assertionresults/{run_id}
			//We need to create AssertionResultService which queries the qa_result table for a given run_id

			final String[] tokens = Files.getNameWithoutExtension(config.getFile().getOriginalFilename()).split("_");
			final String releaseDate = tokens[tokens.length-1];
			String prospectiveVersion = releaseDate ;
			String prevReleaseVersion = config.getPrevIntReleaseVersion();
			final boolean isExtension = config.getPreviousExtVersion() != null ? true : false;
			if (isExtension) {
				//SnomedCT_Release-es_INT_20140430.zip
				//SnomedCT_SpanishRelease_INT_20141031.zip
				final String extensionName = tokens[1].replace("Release", "").replace("-", "").concat("edition_");
				prospectiveVersion = extensionName.toLowerCase() + releaseDate;
				prevReleaseVersion = config.getPreviousExtVersion();
				if (config.getPrevIntReleaseVersion() != null) {
					//previous extension release is being specified as already being merged, but we might have already done it anyway
					prevReleaseVersion = extensionName.toLowerCase() + config.getPreviousExtVersion();
					if (!releaseDataManager.isKnownRelease(prevReleaseVersion)){
						final boolean isSuccess = releaseDataManager.combineKnownVersions(prevReleaseVersion, config.getPrevIntReleaseVersion(), config.getPreviousExtVersion());
						if (!isSuccess) {
							responseMap.put(FAILURE_MESSAGE, "Failed to combine known versions:" 
									+ config.getPrevIntReleaseVersion() + " and"+ config.getPreviousExtVersion() + "into" + prevReleaseVersion);
							writeResults(responseMap, State.FAILED);
							return;
						}
					} else {
						logger.info("Skipping merge of {} with {} as already detected in database as {}",config.getPrevIntReleaseVersion(), config.getPreviousExtVersion(), prevReleaseVersion);
					}
				}
			} 
			uploadProspectiveVersion(prospectiveVersion, config.getExtensionBaseLine(), config.getProspectiveFile());
			runAssertionTests(prospectiveVersion, prevReleaseVersion,config.getRunId(),config.getGroupsList(),responseMap);
			final long timeTaken = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())/60000;
			logger.info(String.format("Finished execution with runId : [%1s] in [%2s] minutes ", config.getRunId(), timeTaken));
			writeResults(responseMap, State.COMPLETE);
		}
		
	}


	public boolean init(ValidationRunConfig config, Map<String, String> responseMap) {
		setConfig(config);
		//Setting this before we actually start running to ensure we have access to storageLocation
		try {
			if (saveUploadedFiles(config, responseMap)) {
				writeState(State.READY);
				initialized = true;
			}
		} catch (Exception e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to write Ready State to Storage Location due to " + e.getMessage());
		}
		return initialized;
	}
	
	/*
	 * The issue here is that spring cleans up Multipart files when Dispatcher is complete, so 
	 * we need to save off the file before we allow the parent thread to finish.
	 */
	private boolean saveUploadedFiles(ValidationRunConfig config, Map<String, String> responseMap) throws IOException {
		final String filename = config.getFile().getOriginalFilename();
		final File tempFile = File.createTempFile(filename, ".zip");
		tempFile.deleteOnExit();
		if (!filename.endsWith(".zip")) {
			responseMap.put(FAILURE_MESSAGE, "Post condition test package has to be zipped up");
			return false;
		}
		// must be a zip, save it off
		ZipFileUtils.copyUploadToDisk(config.getFile(), tempFile);	
		config.setProspectiveFile(tempFile);
		return true;
	}
	
	private void writeResults (Map<String , Object> responseMap, State state) throws IOException, NoSuchAlgorithmException, JSONException, DecoderException {

		JSONObject json = new JSONObject(responseMap);
		writeToS3(json.toString(1),resultsFilePath);
		writeState(state);
	}
	
	private void writeState (State state) throws IOException, NoSuchAlgorithmException, DecoderException {
		logger.info("RVF run {} setting state as {}", config.getRunId(), state.toString() );
		writeToS3(state.name(), stateFilePath);
	}
	
	private void writeToS3(String writeMe, String targetPath) throws IOException, NoSuchAlgorithmException, DecoderException{
		//First write the data to a local temp file
		File temp = File.createTempFile("tempfile", ".tmp"); 
		BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
		bw.write(writeMe);
		bw.close();
		
		//Now copy to our S3 Location
		s3Helper.putFile(temp, targetPath);
		
		//And clean up
		temp.delete();
	}
	
	/*private void combineKnownVersions(final String combinedVersion, final String firstKnown, final String secondKnown) {
		logger.info("Start combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
		final File firstZipFile = releaseDataManager.getZipFileForKnownRelease(firstKnown);
		final File secondZipFile = releaseDataManager.getZipFileForKnownRelease(secondKnown);
		releaseDataManager.loadSnomedData(combinedVersion, firstZipFile , secondZipFile);
		logger.info("Complete combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
	}*/

	private void runAssertionTests(final String prospectiveReleaseVersion, final String previousReleaseVersion, final long runId, final List<String> groupNames,
			final Map<String, Object> responseMap) throws IOException {
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(groupNames);
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		final List<Assertion> resourceAssertions = assertionService.getResourceAssertions();
		logger.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
		final Map<Assertion, Collection<TestRunItem>> assertionResultMap = new HashMap<>();
		int failedAssertionCount = 0;
		failedAssertionCount += executeAssertions(prospectiveReleaseVersion, previousReleaseVersion, runId, resourceAssertions, assertionResultMap);
		final HashSet<Assertion> assertions = new HashSet<>();
		for(final AssertionGroup group : groups)
		{
			for(final Assertion assertion : assertionService.getAssertionsForGroup(group)){
				assertions.add(assertion);
			}
		}
		failedAssertionCount += executeAssertions(prospectiveReleaseVersion,
				previousReleaseVersion, runId, assertions, assertionResultMap);

		responseMap.put("type", "post");

		final List<TestRunItem> items = new ArrayList<>();
		for( final Collection<TestRunItem> testItesms : assertionResultMap.values()) {
			items.addAll(testItesms);
		}
		responseMap.put("assertions", items);
		responseMap.put("assertionsRun", assertionResultMap.keySet().size());
		responseMap.put("assertionsFailed", failedAssertionCount);
	}

	private boolean checkKnownVersion(final String prevIntReleaseVersion, final String previousExtVersion, 
			final String extensionBaseLine, final Map<String, Object> responseMap) {
		logger.debug("Checking known versions...");
		if (previousExtVersion != null ) {
			if(extensionBaseLine == null ) {
				responseMap.put(FAILURE_MESSAGE, "PreviousExtensionVersion is :" 
						+ prevIntReleaseVersion +" but extension release base line has not been specified.");
				return true;
			}
		}
		if (prevIntReleaseVersion == null && previousExtVersion == null && extensionBaseLine == null) {
			responseMap.put(FAILURE_MESSAGE, "None of the known release version is specified");
			return true;
		}
		boolean isFailed = false;
		if (prevIntReleaseVersion != null) {
			if (!isKnownVersion(prevIntReleaseVersion, responseMap))
			{
				isFailed = true;
			}
		}
		if(previousExtVersion != null) {
			if(!isKnownVersion(previousExtVersion, responseMap))
			{
				isFailed = true;
			}
		}
		if( extensionBaseLine != null) {
			if(!isKnownVersion(extensionBaseLine, responseMap)) {
				isFailed = true;
			}
		}
		return isFailed;
	}

	private void uploadProspectiveVersion(final String prospectiveVersion, final String knownVersion, final File tempFile) throws ConfigurationException {
		
		if (knownVersion != null) {
			//load them together here as opposed to clone the existing DB so that to make sure it is clean.
			String versionDate = knownVersion;
			if (knownVersion.length() > 8) {
				versionDate = knownVersion.substring(knownVersion.length() -8);
			}
			final File preLoadedZipFile = releaseDataManager.getZipFileForKnownRelease(versionDate);
			if (preLoadedZipFile != null) {
				logger.info("Start loading release version {} with release file {} and baseline {}", 
						prospectiveVersion, tempFile.getName(), preLoadedZipFile.getName());
				releaseDataManager.loadSnomedData(prospectiveVersion, tempFile, preLoadedZipFile);
			} else {
				throw new ConfigurationException ("Can't find the cached release zip file for known version: " + versionDate);
			}
		} else {
			logger.info("Start loading release version {} with release file {}", prospectiveVersion, tempFile.getName());
			releaseDataManager.loadSnomedData(prospectiveVersion, tempFile);
		}
		logger.info("Completed loading release version {}", prospectiveVersion);
	}

	private boolean isKnownVersion(final String vertionToCheck, final Map<String, Object> responseMap) {
		if(!releaseDataManager.isKnownRelease(vertionToCheck)){
			// the previous published release must already be present in database, otherwise we throw an error!
			responseMap.put("type", "post");
			final String errorMsg = "Please load published release data in RVF first for version: " + vertionToCheck;
			responseMap.put(FAILURE_MESSAGE, errorMsg);
			logger.info(errorMsg);
			return false;
		}
		return true;
	} 
	


	private int executeAssertions(final String prospectiveReleaseVersion,
			final String previousReleaseVersion, final Long runId,
			final Collection<Assertion> assertions,
			final Map<Assertion, Collection<TestRunItem>> map) {
		int failedAssertionCount = 0;
		
		int counter = 1;
		for (final Assertion assertion: assertions) {
			try
			{
				logger.info(String.format("Started executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
				final List<TestRunItem> items = new ArrayList<>(assertionExecutionService.executeAssertion(assertion, runId,
						prospectiveReleaseVersion, previousReleaseVersion));
				// get only first since we have 1:1 correspondence between Assertion and Test
				if(items.size() == 1){
					final TestRunItem runItem = items.get(0);
					if(runItem.isFailure()){
						failedAssertionCount++;
					}
				}
				map.put(assertion, items);
				logger.info(String.format("Finished executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
				counter++;
			}
			catch (final MissingEntityException e) {
				failedAssertionCount++;
			}
		}
		return failedAssertionCount;
	}

	public void setConfig(ValidationRunConfig config) {
		this.config = config;
		s3Helper = new FileHelper(bucketName, s3Client);
		stateFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "state.txt";
		resultsFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "results.json";
	}
	
	public State getCurrentState() {
		State currentState = null;
		try {
			InputStream is = s3Helper.getFileStream(stateFilePath);
			if (is == null) {
				logger.warn("Failed to find state file {}, in bucket {}", stateFilePath, bucketName);
			}
			String stateStr = IOUtils.toString(is, "UTF-8");
			currentState = State.valueOf(stateStr);
		} catch (Exception e) {
			logger.warn("Failed to determine validation run state in file {} due to {}", stateFilePath, e.toString());
		}
		return currentState;
	}

	public void recoverResult(Map<String, String> responseMap) throws IOException {
		InputStream is = s3Helper.getFileStream(resultsFilePath);
		String jsonResults = "Failed to recover results in " + resultsFilePath;
		if (is == null) {
			logger.warn("Failed to find results file {}, in bucket {}", stateFilePath, bucketName);
		} else {
			String jsonResultsEscaped = IOUtils.toString(is, "UTF-8");
			jsonResults = StringEscapeUtils.unescapeJava(jsonResultsEscaped);
		}
		responseMap.put("RVFResult", jsonResults);
	}

}
