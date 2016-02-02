package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidationVersionLoader {

	private static final String SNAPSHOT_TABLE = "%_s";
	private static final String SEPARATOR = "/";

	private static final String INTERNATIONAL = "international";

	private static final String ZIP_FILE_EXTENSION = ".zip";

	public static final String FAILURE_MESSAGE = "failureMessage";

	@Autowired
	private ReleaseDataManager releaseDataManager;
	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	private RvfDbScheduledEventGenerator scheduleEventGenerator;
	
	@Resource
	private S3Client s3Client;

	@Autowired
	private ResourceDataLoader resourceLoader;
	
	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);
	
	
	public ExecutionConfig loadPreviousVersion(Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		String prevReleaseVersion = resolvePreviousVersion(validationConfig.getPrevIntReleaseVersion());
		final boolean isExtension = isExtension(validationConfig);
		if (isExtension) {
			prevReleaseVersion = "previous_" + validationConfig.getRunId();
		}
		final ExecutionConfig executionConfig = new ExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setPreviousVersion(prevReleaseVersion);
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setExtensionValidation(isExtension);
		//default to 10
		executionConfig.setFailureExportMax(10);
		if (validationConfig.getFailureExportMax() != null) {
			executionConfig.setFailureExportMax(validationConfig.getFailureExportMax());
		}
		List<String> rf2FilesLoaded = new ArrayList<>();
		boolean isSucessful = false;
		String reportStorage = validationConfig.getStorageLocation();
		if (!isPublishedVersionsLoaded(validationConfig)) {
			//load published versions from s3 
			isSucessful = prepareVersionsFromS3FilesForPreviousVersion(validationConfig, reportStorage,responseMap, rf2FilesLoaded, executionConfig);

		} else {
			isSucessful = combineKnownReleases(validationConfig, reportStorage,responseMap, rf2FilesLoaded, executionConfig);
		}
		return isSucessful ? executionConfig : null;
		}
		
	public boolean loadProspectiveVersion(ExecutionConfig executionConfig, Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		String prospectiveVersion = executionConfig.getExecutionId().toString();
		executionConfig.setProspectiveVersion(prospectiveVersion);
		List<String> rf2FilesLoaded = new ArrayList<>();
		boolean isSucessful = false;
		String reportStorage = validationConfig.getStorageLocation();
		if (!isPublishedVersionsLoaded(validationConfig)) {
			//load published versions from s3 and load prospective file
			isSucessful = prepareVersionsFromS3FilesForProspectvie(validationConfig, reportStorage,responseMap, rf2FilesLoaded, executionConfig);
		} else {
			isSucessful = combineCurrentReleases(validationConfig, reportStorage,responseMap, rf2FilesLoaded, executionConfig);
		}
		if (!isSucessful) {
			return false;
		}
		responseMap.put("totalRF2FilesLoaded", rf2FilesLoaded.size());
		Collections.sort(rf2FilesLoaded);
		responseMap.put("rf2Files", rf2FilesLoaded);
		final String prospectiveSchema = releaseDataManager.getSchemaForRelease(prospectiveVersion);
		if (prospectiveSchema != null) {
			reportService.writeProgress("Loading resource data for prospective schema:" + prospectiveSchema, reportStorage);
			resourceLoader.loadResourceData(prospectiveSchema);
			logger.info("completed loading resource data for schema:" + prospectiveSchema);
		}
		return true;
	}

	public List<String> loadProspectiveDeltaWithPreviousSnapshotIntoDB(String prospectiveVersion, ValidationRunConfig validationConfig) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		if (validationConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, validationConfig.getLocalProspectiveFile());
			//copy snapshot from previous release
			releaseDataManager.copyTableData(validationConfig.getPrevIntReleaseVersion(), prospectiveVersion,SNAPSHOT_TABLE);
			releaseDataManager.updateSnapshotTableWithDataFromDelta(prospectiveVersion);
		}
		return filesLoaded;
	}

	public void downloadProspectiveVersion(ValidationRunConfig validationConfig) throws Exception {
		if (validationConfig.isProspectiveFilesInS3()) {
			//streaming file from S3 to local
			long s3StreamingStart = System.currentTimeMillis();
			FileHelper s3Helper = new FileHelper(validationConfig.getS3ExecutionBucketName(), s3Client);
			InputStream input = s3Helper.getFileStream(validationConfig.getProspectiveFileFullPath());
			File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_" + validationConfig.getTestFileName(), null);
			IOUtils.copy(input, new FileOutputStream(prospectiveFile));
			validationConfig.setLocalProspectiveFile(prospectiveFile);
			if (validationConfig.getManifestFileFullPath() != null) {
				InputStream manifestInput = s3Helper.getFileStream(validationConfig.getManifestFileFullPath());
				File manifestFile = File.createTempFile("manifest.xml_" + validationConfig.getRunId(), null);
				IOUtils.copy(manifestInput, new FileOutputStream(manifestFile));
				validationConfig.setLocalManifestFile(manifestFile);
			}
			logger.info("Time taken {} seconds to download files {} from s3", (System.currentTimeMillis()-s3StreamingStart)/1000 , validationConfig.getProspectiveFileFullPath());
		} else {
			validationConfig.setLocalProspectiveFile(new File(validationConfig.getProspectiveFileFullPath()));
			if (validationConfig.getManifestFileFullPath() != null) {
				validationConfig.setLocalManifestFile(new File(validationConfig.getManifestFileFullPath()));
			}
		}
		
	}
	
	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependencyVersion() != null 
				&& !runConfig.getExtensionDependencyVersion().trim().isEmpty()) ? true : false;
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
	
	private boolean isPublishedVersionsLoaded(ValidationRunConfig validationConfig) {
		if (validationConfig.getPrevIntReleaseVersion() != null && validationConfig.getPrevIntReleaseVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		if (validationConfig.getPreviousExtVersion() != null && validationConfig.getPreviousExtVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		if (validationConfig.getExtensionDependencyVersion() != null && validationConfig.getExtensionDependencyVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		return true;
	}
	
	
	
	private boolean prepareVersionsFromS3FilesForPreviousVersion(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap,List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception {
		FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
		if (validationConfig.getPrevIntReleaseVersion() != null) {
			String previousPublished = INTERNATIONAL + SEPARATOR + validationConfig.getPrevIntReleaseVersion();
			logger.debug("download published version from s3:" + previousPublished );
			InputStream previousIntInput = s3PublishFileHelper.getFileStream(previousPublished);
			File previousVersionTemp = File.createTempFile(validationConfig.getPrevIntReleaseVersion(), null);
			IOUtils.copy(previousIntInput, new FileOutputStream(previousVersionTemp));
			List<String> prevRf2FilesLoaded = new ArrayList<>();
			if (isExtension(validationConfig) && !validationConfig.isFirstTimeRelease()) {
				String combinedVersionName = executionConfig.getPreviousVersion();
				final String startCombiningMsg = String.format("Combining previous releases:[%s],[%s] into [%s]", validationConfig.getPrevIntReleaseVersion() , validationConfig.getPreviousExtVersion(), combinedVersionName);
				logger.info(startCombiningMsg);
				reportService.writeProgress(startCombiningMsg, reportStorage);
				String previousExtZipFile = INTERNATIONAL + SEPARATOR + validationConfig.getPreviousExtVersion();
				logger.debug("downloading published extension from s3:" + previousExtZipFile);
				InputStream previousExtInput = s3PublishFileHelper.getFileStream(previousExtZipFile);
				File previousExtTemp = File.createTempFile(validationConfig.getPreviousExtVersion(), null);
				IOUtils.copy(previousExtInput, new FileOutputStream(previousExtTemp));

				releaseDataManager.loadSnomedData(combinedVersionName, prevRf2FilesLoaded, previousVersionTemp,previousExtTemp);
				String schemaName = releaseDataManager.getSchemaForRelease(executionConfig.getPreviousVersion());
				if (schemaName == null) {
					responseMap.put(FAILURE_MESSAGE, "Failed to load two versions:" 
							+ validationConfig.getPrevIntReleaseVersion() + " and " + validationConfig.getPreviousExtVersion() + " into " + combinedVersionName);
					reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
					return false;
				}
			} else {
				releaseDataManager.loadSnomedData(executionConfig.getPreviousVersion(), prevRf2FilesLoaded, previousVersionTemp);
			}
		} 
		return true;
	}
	
	
	
	private boolean prepareVersionsFromS3FilesForProspectvie(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap,List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception {
		FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
		String prospectiveVersion = validationConfig.getRunId().toString();
		if (isExtension(validationConfig)) {
			String extensionDependency = INTERNATIONAL + SEPARATOR + validationConfig.getExtensionDependencyVersion();
			logger.debug("download published  extension dependency version from s3:" +  extensionDependency);
			InputStream extensionDependencyInput = s3PublishFileHelper.getFileStream(extensionDependency);
			File extensionDependencyTemp = File.createTempFile(validationConfig.getExtensionDependencyVersion(), null);
			IOUtils.copy(extensionDependencyInput, new FileOutputStream(extensionDependencyTemp));
			releaseDataManager.loadSnomedData(prospectiveVersion, rf2FilesLoaded, validationConfig.getLocalProspectiveFile(),extensionDependencyTemp);
		} else {
			uploadProspectiveVersion(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
		}
		return true;
	}
	
	private void uploadProspectiveVersion(final String prospectiveVersion, final String knownVersion, final File tempFile, 
			final List<String> rf2FilesLoaded) throws ConfigurationException, BusinessServiceException {
		
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


	private boolean combineKnownReleases(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap, List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception{
		boolean isFailed = checkKnownVersion(validationConfig,responseMap);
		if (isFailed) {
			reportService.writeResults(responseMap, State.FAILED, reportStorage);
			return false;
		}
		if (isExtension(validationConfig) && !validationConfig.isFirstTimeRelease()) {
			//SnomedCT_Release-es_INT_20140430.zip
			//SnomedCT_SpanishRelease_INT_20141031.zip
			if (validationConfig.getPrevIntReleaseVersion() != null) {
				String combinedVersionName = executionConfig.getPreviousVersion();
				final String startCombiningMsg = String.format("Combining previous releases:[%s],[%s] into: [%s]", validationConfig.getPrevIntReleaseVersion() , validationConfig.getPreviousExtVersion(), combinedVersionName);
				logger.info(startCombiningMsg);
				reportService.writeProgress(startCombiningMsg, reportStorage);
				final boolean isSuccess = releaseDataManager.combineKnownVersions(combinedVersionName, validationConfig.getPrevIntReleaseVersion(), validationConfig.getPreviousExtVersion());
				if (!isSuccess) {
					String message = "Failed to combine known versions:" 
							+ validationConfig.getPrevIntReleaseVersion() + " and " + validationConfig.getPreviousExtVersion() + " into " + combinedVersionName;
					responseMap.put(FAILURE_MESSAGE, message);
					reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
					String schemaName = releaseDataManager.getSchemaForRelease(combinedVersionName);
					if (schemaName != null) {
						scheduleEventGenerator.createDropReleaseSchemaEvent(schemaName);
						releaseDataManager.dropVersion(combinedVersionName);
					}
					return false;
				}
			}
		} 
		return true;
	}
	
	
	private boolean combineCurrentReleases(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap, List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception{
		String prospectiveVersion = validationConfig.getRunId().toString();
		if (isExtension(validationConfig)) {
			uploadProspectiveVersion(prospectiveVersion, validationConfig.getExtensionDependencyVersion(), validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
		} else if (validationConfig.isRf2DeltaOnly()) {
			rf2FilesLoaded.addAll(loadProspectiveDeltaWithPreviousSnapshotIntoDB(prospectiveVersion, validationConfig));
		} else {		  			
			uploadProspectiveVersion(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
		}
		return true;
	}
	
	
	private boolean checkKnownVersion(ValidationRunConfig validationConfig, Map<String, Object> responseMap) {
		logger.debug("Checking known versions...");
		String previousExtVersion =validationConfig.getPreviousExtVersion();
		String extensionBaseLine = validationConfig.getExtensionDependencyVersion();
		String prevIntReleaseVersion = validationConfig.getPrevIntReleaseVersion();
		if (previousExtVersion != null) {
			if (extensionBaseLine == null) {
				responseMap.put(FAILURE_MESSAGE, "PreviousExtensionVersion is :" 
						+ prevIntReleaseVersion + " but extension release base line has not been specified.");
				return true;
			}
		}
		if (!validationConfig.isFirstTimeRelease() && prevIntReleaseVersion == null && previousExtVersion == null && extensionBaseLine == null) {
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
	
	private String resolvePreviousVersion(String releasePkgName) {
		String version = releasePkgName;
		if (releasePkgName.endsWith(ZIP_FILE_EXTENSION)) {
			version = releasePkgName.replace(ZIP_FILE_EXTENSION, "");
			String [] splits = version.split("_");
			if (splits.length >=4) {
				version = splits[2] + "_" + splits[3];
			}
		}
		return version;
	}
	
	/*private void combineKnownVersions(final String combinedVersion, final String firstKnown, final String secondKnown) {
	logger.info("Start combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
	final File firstZipFile = releaseDataManager.getZipFileForKnownRelease(firstKnown);
	final File secondZipFile = releaseDataManager.getZipFileForKnownRelease(secondKnown);
	releaseDataManager.loadSnomedData(combinedVersion, firstZipFile , secondZipFile);
	logger.info("Complete combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
}*/

}
