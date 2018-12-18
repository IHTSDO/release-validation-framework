package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.execution.service.util.RvfReleaseDbSchemaNameGenerator;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ValidationVersionLoader {

	private static final String COMBINED = "_combined";
	private static final String RELATIONSHIP_SNAPSHOT_TABLE = "relationship_s";
	private static final String PREVIOUS = "previous_";
	private static final String DEPENDENCY = "dependency_";
	private static final String SNAPSHOT_TABLE = "%_s";
	private static final String SEPARATOR = "/";

	private static final String RVF_PREFIX = "rvf_";

	private static final String INTERNATIONAL = "international";
	private static final String INT = "int";

	private static final String ZIP_FILE_EXTENSION = ".zip";

	public static final String FAILURE_MESSAGE = "failureMessage";
	private static final String ISAM_BACKUP = "isam_backup";

	private static final String UTF_8 = "UTF-8";
	private static final String DELTA_TABLE = "%_d";
	private static final String FULL_TABLE = "%_f";

	@Autowired
	private ReleaseDataManager releaseDataManager;

	@Autowired
	private ValidationReportService reportService;

	@Resource
	private S3Client s3Client;

	@Autowired
	private ResourceDataLoader resourceLoader;

	@Resource(name = "dataSource")
	private BasicDataSource snomedDataSource;

	@Value("${rvf.jdbc.data.myisam.folder}")
	private String mysqlMyISamDataFolder;

	@Autowired
	private RvfReleaseDbSchemaNameGenerator generator;

	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);
	
	public String loadRelease(ExecutionConfig executionConfig, Map<String, Object> responseMap) throws BusinessServiceException, IOException {
		String previousVersion = executionConfig.getPreviousVersion();
		if (previousVersion != null && previousVersion.endsWith(ZIP_FILE_EXTENSION)) {
			String schemaName = generator.generate(previousVersion);
			if (!releaseDataManager.isKnownRelease(schemaName)) {
				if (!releaseDataManager.restoreReleaseFromBinaryArchive(schemaName + ZIP_FILE_EXTENSION)) {
					releaseDataManager.uploadRelease(previousVersion, schemaName);
					String archiveFilename = releaseDataManager.generateBinaryArchive(schemaName);
					logger.info("Release mysql binary archive is generated:" + archiveFilename);
				} 
			} 
			return schemaName;
		}
		return previousVersion;
	}
	
	public boolean loadDependncyVersion(ExecutionConfig executionConfig, Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws BusinessServiceException {
		boolean isSucessful = true;
		if (validationConfig.getExtensionDependency() != null && validationConfig.getExtensionDependency().endsWith(ZIP_FILE_EXTENSION)) {
			//load dependency release
			FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
			String extensionDependencyVersion = getVersion(validationConfig.getExtensionDependency(), false);
			executionConfig.setExtensionDependencyVersion(extensionDependencyVersion);
			try {
				if(!databaseExists(RVF_PREFIX + executionConfig.getExtensionDependencyVersion())){
					loadPublishedVersionIntoDatabase(s3PublishFileHelper, validationConfig.getExtensionDependency(), executionConfig.getExtensionDependencyVersion());
				}
			} catch (Exception e) {
				throw new BusinessServiceException("Failed to load dependency release from S3.", e);
			}
			String schemaName = releaseDataManager.getSchemaForRelease(executionConfig.getExtensionDependencyVersion());
			if (schemaName == null) {
				String failureMsg = "Failed to load dependency version:" + schemaName;
				responseMap.put(FAILURE_MESSAGE, failureMsg);
				reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
				isSucessful = false;
			}
		}
		return isSucessful;
	}
	
	public boolean loadPreviousVersion(ExecutionConfig executionConfig, Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		boolean isSucessful = true;
		if (!isPeviousVersionLoaded(validationConfig)) {
			String previousVersion = null;
			//load previous published versions from s3
			if (isExtension(validationConfig)) {
				previousVersion = getVersion(validationConfig.getPreviousExtVersion(), validationConfig.isReleaseAsAnEdition());
			} else {
				executionConfig.setPreviousVersion(validationConfig.getPrevIntReleaseVersion());
				previousVersion = loadRelease(executionConfig, responseMap);
			}
			executionConfig.setPreviousVersion(previousVersion);
		} else {
			if (isExtension(validationConfig)) {
				executionConfig.setPreviousVersion(getVersion(validationConfig.getPreviousExtVersion(), validationConfig.isReleaseAsAnEdition()));
			} else {
				executionConfig.setPreviousVersion(getVersion(validationConfig.getPrevIntReleaseVersion(),false));
			}
		}
		return isSucessful;
	}
		
	public boolean loadProspectiveVersion(ExecutionConfig executionConfig, Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws Exception {
		String prospectiveVersion = executionConfig.getExecutionId().toString();
		executionConfig.setProspectiveVersion(prospectiveVersion);
		List<String> rf2FilesLoaded = new ArrayList<>();
		String reportStorage = validationConfig.getStorageLocation();
		if (validationConfig.isRf2DeltaOnly()) {
			List<String> excludeTables = Arrays.asList(RELATIONSHIP_SNAPSHOT_TABLE);
			rf2FilesLoaded.addAll(loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(prospectiveVersion, validationConfig,excludeTables));
			if (isExtension(validationConfig)) {
				executionConfig.setPreviousVersion(getVersion(validationConfig.getPreviousExtVersion(), validationConfig.isReleaseAsAnEdition()));
			} else {
				executionConfig.setPreviousVersion(getVersion(validationConfig.getPrevIntReleaseVersion(), false));
			}
		} else {
			//load prospective version alone now as used to combine with dependency for extension testing
			uploadReleaseFileIntoDB(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
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

	public ExecutionConfig createExecutionConfig(ValidationRunConfig validationConfig) {
		ExecutionConfig executionConfig = new ExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setExtensionValidation( isExtension(validationConfig));
		executionConfig.setExtensionDependencyVersion(validationConfig.getExtensionDependency());
		executionConfig.setFirstTimeRelease(validationConfig.isFirstTimeRelease());
		executionConfig.setEffectiveTime(validationConfig.getEffectiveTime());
		if(validationConfig.getExtensionDependency() != null) {
			executionConfig.setDependencyEffectiveTime(extractEffetiveTimeFromDepedencyVersion(validationConfig.getExtensionDependency()));
		}
		//default to 10
		executionConfig.setFailureExportMax(10);
		if (validationConfig.getFailureExportMax() != null) {
			executionConfig.setFailureExportMax(validationConfig.getFailureExportMax());
		}
		executionConfig.setReleaseValidation(!validationConfig.isRf2DeltaOnly());
		return executionConfig;
	}

	public List<String> loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(String prospectiveVersion, ValidationRunConfig validationConfig, List<String> excludeTableNames) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		if (validationConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, validationConfig.getLocalProspectiveFile());
			if (isExtension(validationConfig)) {
				String previousVersion = getVersion(validationConfig.getPreviousExtVersion(), validationConfig.isReleaseAsAnEdition());
				String extensionDependencyVersion = getVersion(validationConfig.getExtensionDependency(), false);
				if (!validationConfig.isFirstTimeRelease()) {
					releaseDataManager.copyTableData(previousVersion,extensionDependencyVersion, prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				} else {
					releaseDataManager.copyTableData(extensionDependencyVersion, prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				}
				
			} else {
				//copy snapshot from previous release
				if (!validationConfig.isFirstTimeRelease()) {
					String previousVersion = getVersion(validationConfig.getPrevIntReleaseVersion(), false);
					releaseDataManager.copyTableData(previousVersion, prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				}
			}
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
			File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_" + validationConfig.getTestFileName(), ZIP_FILE_EXTENSION);
			OutputStream out = new FileOutputStream(prospectiveFile);
			IOUtils.copy(input, out);
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
			logger.debug("local prospective file" + prospectiveFile.getAbsolutePath());
			validationConfig.setLocalProspectiveFile(prospectiveFile);
			if (validationConfig.getManifestFileFullPath() != null) {
				InputStream manifestInput = s3Helper.getFileStream(validationConfig.getManifestFileFullPath());
				File manifestFile = File.createTempFile("manifest_" + validationConfig.getRunId(), ".xml");
				Writer output = new FileWriter(manifestFile);
				IOUtils.copy(manifestInput, output, UTF_8);
				IOUtils.closeQuietly(manifestInput);
				IOUtils.closeQuietly(output);
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

	public void downloadDependencyVersion(ValidationRunConfig validationConfig) throws IOException, BusinessServiceException {
		String extensionDependencyVersion = validationConfig.getExtensionDependency();
		if (StringUtils.isNotBlank(extensionDependencyVersion) && extensionDependencyVersion.endsWith(ZIP_FILE_EXTENSION)) {
			String publishedFileS3Path = getPublishedFilePath(validationConfig.getExtensionDependency());
			FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
			InputStream publishedFileInput = s3PublishFileHelper.getFileStream(publishedFileS3Path);
			if (publishedFileInput != null) {
				File dependencyFile = File.createTempFile(DEPENDENCY +validationConfig.getRunId(), ZIP_FILE_EXTENSION);
				OutputStream out = new FileOutputStream(dependencyFile);
				IOUtils.copy(publishedFileInput,out);
				IOUtils.closeQuietly(publishedFileInput);
				IOUtils.closeQuietly(out);
				logger.debug("local dependency file" + dependencyFile.getAbsolutePath());
				validationConfig.setLocalDependencyFile(dependencyFile);
			} else {
				String msg = "Failed to load dependency " + extensionDependencyVersion + ": not an S3 path or dependency release not found in the published bucket:" + publishedFileS3Path;
				logger.error(msg);
				throw new BusinessServiceException(msg);
			}
		}
	}

	public void downloadPreviousVersion(ValidationRunConfig validationConfig) throws IOException, BusinessServiceException {
		String previousVersion = StringUtils.isNotBlank(validationConfig.getPreviousExtVersion()) ? validationConfig.getPreviousExtVersion() : validationConfig.getPrevIntReleaseVersion();
		if (StringUtils.isNotBlank(previousVersion) && previousVersion.endsWith(ZIP_FILE_EXTENSION)) {
			String publishedFileS3Path = getPublishedFilePath(previousVersion);
			FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
			InputStream publishedFileInput = s3PublishFileHelper.getFileStream(publishedFileS3Path);
			if (publishedFileInput != null) {
				File previousDependency = File.createTempFile(PREVIOUS +validationConfig.getRunId(), ZIP_FILE_EXTENSION);
				OutputStream out = new FileOutputStream(previousDependency);
				IOUtils.copy(publishedFileInput,out);
				IOUtils.closeQuietly(publishedFileInput);
				IOUtils.closeQuietly(out);
				logger.debug("local previous file" + previousDependency.getAbsolutePath());
				validationConfig.setLocalPreviousFile(previousDependency);
			} else {
				String msg = "Failed to load previous version " + previousVersion + ": not an S3 path or dependency release not found in the published bucket:" + publishedFileS3Path;
				logger.error(msg);
				throw new BusinessServiceException(msg);
			}
		}
	}

	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependency() != null
				&& !runConfig.getExtensionDependency().trim().isEmpty());
	}

	public boolean isKnownVersion(final String vertionToCheck, final Map<String, Object> responseMap) {
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
	
	private boolean isPeviousVersionLoaded(ValidationRunConfig validationConfig) {
		if (validationConfig.getPrevIntReleaseVersion() != null && validationConfig.getPrevIntReleaseVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		if (validationConfig.getPreviousExtVersion() != null && validationConfig.getPreviousExtVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		return true;
	}
	
	private void loadPublishedVersionIntoDB( FileHelper s3PublishFileHelper, String publishedReleaseFilename, String rvfVersion) throws Exception {
		String publishedFileS3Path = getPublishedFilePath(publishedReleaseFilename);
		InputStream publishedFileInput = s3PublishFileHelper.getFileStream(publishedFileS3Path);
		if (publishedFileInput != null) {
			File tempFile = File.createTempFile(publishedReleaseFilename, ZIP_FILE_EXTENSION);
			OutputStream out = new FileOutputStream(tempFile);
			IOUtils.copy(publishedFileInput,out);
			IOUtils.closeQuietly(publishedFileInput);
			IOUtils.closeQuietly(out);
			releaseDataManager.loadSnomedData(rvfVersion, new ArrayList<String>(),tempFile);
		} else {
			String msg = "Previous release not found in the published bucket:" + publishedFileS3Path;
			logger.error(msg);
			throw new BusinessServiceException(msg);
		}
	}

	private void loadPublishedVersionIntoDatabase( FileHelper s3PublishFileHelper, String publishedReleaseFilename, String rvfVersion) throws Exception {
		//Try to restore schema from S3 if there is MyISAM backup, otherwise load data from published package
		if(!downloadMyISAMOnS3AndRestore(s3PublishFileHelper, rvfVersion)) {
			String publishedFileS3Path = getPublishedFilePath(publishedReleaseFilename);
			logger.debug("downloading published file from s3: " + publishedFileS3Path); //download previous ZIP file from S3
			InputStream publishedFileInput = s3PublishFileHelper.getFileStream(publishedFileS3Path);
			if (publishedFileInput != null) {
				File tempFile = File.createTempFile(publishedReleaseFilename, ZIP_FILE_EXTENSION);
				OutputStream out = new FileOutputStream(tempFile);
				IOUtils.copy(publishedFileInput,out);
				IOUtils.closeQuietly(publishedFileInput);
				IOUtils.closeQuietly(out);
				String createdSchemaName = releaseDataManager.loadSnomedData(rvfVersion, new ArrayList<String>(),tempFile);
				//Backup database to S3 for faster loading
				backupDatabaseToS3(rvfVersion, createdSchemaName, s3PublishFileHelper);
			} else {
				String msg = "Previous release not found in the published bucket:" + publishedFileS3Path;
				logger.error(msg);
				//throw new BusinessServiceException(msg);
			}
		}
	}

	private void backupDatabaseToS3(String rvfVersion, String createdSchemaName, FileHelper s3PublishFileHelper) throws Exception {
		File mysqlISAMFolder = new File(mysqlMyISamDataFolder);
		//Have to check the MYSQL data directory permission, if RVF cannot read or write to this directory, skip backup step
		if(mysqlISAMFolder.isDirectory() && mysqlISAMFolder.canWrite()) {
			File tempDir = FileUtils.getTempDirectory();
			File backupMyISAMZipFile = new File(tempDir, createdSchemaName + ZIP_FILE_EXTENSION);

			//copy all files with extension: FRM, MYD, MYI
			File myISAMFolder = new File(mysqlMyISamDataFolder + SEPARATOR + createdSchemaName);
			File[] files = myISAMFolder.listFiles();
			if(files == null) {
				logger.warn("RVF cannot access {} with user {} to backup database {} to S3. Skip this step", mysqlISAMFolder, System.getProperty("user.name"), createdSchemaName);
				return;
			}

			ZipFileUtils.zip(myISAMFolder.getAbsolutePath(), backupMyISAMZipFile.getAbsolutePath());
			//Upload to S3
			final String previousMyISAMFullPath = ISAM_BACKUP + File.separator + backupMyISAMZipFile.getName();
			s3PublishFileHelper.putFile(backupMyISAMZipFile, previousMyISAMFullPath);
			FileUtils.deleteQuietly(backupMyISAMZipFile);
		} else {
			logger.warn("RVF cannot access {} with user {} to backup database {} to S3. Skip this step", mysqlISAMFolder, System.getProperty("user.name"), createdSchemaName);
		}


	}

	/**
	 * Check whether database exists or not
	 */
	private boolean databaseExists(String schemaName) throws SQLException {
		logger.info("Checking existence of database {}", schemaName);
		try (Connection connection = snomedDataSource.getConnection()) {
			connection.setAutoCommit(true);
			//check whether database exists or not
			String createDbStr = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '"+ schemaName + "';";
			try(Statement statement = connection.createStatement()) {
				ResultSet resultSet = statement.executeQuery(createDbStr);
				boolean result = resultSet.first();
				logger.info("Checking condition: Database {} has already existed: {}", schemaName, result);
				return result;
			}
		}
	}

	private boolean prepareVersionsFromS3FilesForPreviousVersion(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap,List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception {
		FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
		if (!validationConfig.isFirstTimeRelease()) {
			if (isExtension(validationConfig)) {
				if (validationConfig.getPreviousExtVersion() != null && validationConfig.getPreviousExtVersion().endsWith(ZIP_FILE_EXTENSION)) {
					if(!databaseExists(RVF_PREFIX + executionConfig.getPreviousVersion())){
						loadPublishedVersionIntoDatabase(s3PublishFileHelper, validationConfig.getPreviousExtVersion(),  executionConfig.getPreviousVersion());
					}
				}
			} else {
				if (validationConfig.getPrevIntReleaseVersion() != null && validationConfig.getPrevIntReleaseVersion().endsWith(ZIP_FILE_EXTENSION)) {
					//check database exist or not
					if(!databaseExists(RVF_PREFIX + executionConfig.getPreviousVersion())){
						loadPublishedVersionIntoDatabase(s3PublishFileHelper, validationConfig.getPrevIntReleaseVersion(), executionConfig.getPreviousVersion());
					}
				}
			}
		}

		/**
		 * this code to make sure that we have a database of previous published package
		 */
		if (!databaseExists(RVF_PREFIX + executionConfig.getPreviousVersion())) {
			String failureMsg = "Failed to load previous version:" + (isExtension(validationConfig) ? validationConfig.getPreviousExtVersion(): validationConfig.getPrevIntReleaseVersion())
					+ " into " + executionConfig.getPreviousVersion();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
			return false;
		}
		return true;
	}

	private String extractEffetiveTimeFromDepedencyVersion(String dependencyVersion) {
		String effectiveTime = null;
		try {
			Pattern pattern = null;
			String text;
			if(dependencyVersion.endsWith(ZIP_FILE_EXTENSION)) {
				pattern = Pattern.compile("\\d{8}(?=(T\\d+|.zip))");
				String[] splits = dependencyVersion.split("/");
				text = splits[splits.length-1];
			} else {
				pattern = Pattern.compile("(?<=_)(\\d{8})");
				text = dependencyVersion;
			}
			Matcher matcher = pattern.matcher(text);
			if(matcher.find()) {
				effectiveTime = matcher.group();
			}
		} catch (Exception e) {
			logger.error("Encounter error when extracting effective time from {}", dependencyVersion);
		}
		return  effectiveTime;
	}

	private String getVersion(String versionString, boolean isEdition) throws BusinessServiceException {
		if (!versionString.endsWith(ZIP_FILE_EXTENSION)) {
			return versionString;
		}
		if (versionString.contains(" <= ")) {
			return versionString.substring(0,versionString.indexOf(" <= ")).trim();

		} else {
			String publishedS3Path = getPublishedFilePath(versionString);
			String[] splits = publishedS3Path.split("/");
			String releaseCenter = splits[0];
			String fileName = splits[splits.length-1];
			Pattern pattern = Pattern.compile("\\d{8}(?=(T\\d+|.zip))");
			Matcher matcher = pattern.matcher(fileName);
			String versionDate = null;
			if(matcher.find()) {
				versionDate = matcher.group();
			}
			if(versionDate==null) {
				throw new BusinessServiceException("Could not extract version date from " + versionString);
			}
			if(releaseCenter.equalsIgnoreCase(INTERNATIONAL)) {
				releaseCenter = INT;
			} else {
				//If MS product, check whether it is an edition
				if(isEdition) {
					releaseCenter = releaseCenter + "_edition";
				}
			}
			//Add s3 prefix to know that this version is loaded from S3 instead of being pre-loaded into RVF DB
			return "s3_" + releaseCenter + "_" + versionDate;
		}

	}

	private String getPublishedFilePath(String publishedReleasePath) {
		String preparedPath = null;
		if(publishedReleasePath.contains(" <= ")) {
			preparedPath = publishedReleasePath.substring(publishedReleasePath.indexOf(" <= ")+4).trim();
		} else {
			preparedPath = publishedReleasePath;
		}
		//default to the international folder;
		String publishedFileS3Path = null;
		if (preparedPath != null && preparedPath.contains("/")) {
			//published release file with s3 prefix
			if(preparedPath.startsWith("s3:")) {
				publishedFileS3Path = preparedPath.replace("//", "");
				publishedFileS3Path = publishedFileS3Path.substring(publishedFileS3Path.indexOf("/") + 1);
			} else {
				publishedFileS3Path = preparedPath;
				if(publishedFileS3Path.startsWith("/")) {
					publishedFileS3Path = publishedFileS3Path.substring(1);
				}
			}
		} else {
			publishedFileS3Path = INTERNATIONAL + SEPARATOR + preparedPath;
		}
		return publishedFileS3Path;
	}


	private boolean downloadMyISAMOnS3AndRestore(FileHelper s3PublishFileHelper, String rvfVersion) throws Exception {
		File myISAMDirectory = new File(mysqlMyISamDataFolder);
		String schemaName = RVF_PREFIX + rvfVersion;
		if(myISAMDirectory.isDirectory() && myISAMDirectory.canWrite()) {
			String previousMyISAMS3Path =  ISAM_BACKUP + File.separator + schemaName + ZIP_FILE_EXTENSION;
			logger.debug("Downloading previous published MyISAM file from s3:" + previousMyISAMS3Path);
			InputStream publishedFileInput = s3PublishFileHelper.getFileStream(previousMyISAMS3Path);
			if (publishedFileInput != null) {
				logger.debug("Download previous published MyISAM file from s3 successfully:" + previousMyISAMS3Path);
				File restoredZipFile = File.createTempFile(schemaName, ZIP_FILE_EXTENSION);
				OutputStream out = new FileOutputStream(restoredZipFile);
				IOUtils.copy(publishedFileInput, out);
				IOUtils.closeQuietly(publishedFileInput);
				IOUtils.closeQuietly(out);
				File tmp = new File(restoredZipFile.getParentFile().getAbsolutePath() + File.separator + schemaName + new Date().getTime());
				tmp.mkdir();
				ZipFileUtils.unzip(restoredZipFile.getPath(), tmp.getPath());
				File myISamDataFolder = new File(mysqlMyISamDataFolder + SEPARATOR + schemaName);
				FileUtils.copyDirectory(tmp, myISamDataFolder);
				FileUtils.deleteQuietly(restoredZipFile);
				FileUtils.deleteQuietly(tmp);
				//ISAM binary files' permissions must be update to allow MySQL to read
				updateFilesPermissions(myISamDataFolder);
				releaseDataManager.setSchemaForRelease(rvfVersion, schemaName);
				return true;
			}
			logger.debug("Can not find published MyISAM file from S3:" + rvfVersion);
			return false;
		}
		logger.warn("RVF cannot access {} with user to restore version {} to S3. Skip this step", mysqlMyISamDataFolder, System.getProperty("user.name"), rvfVersion);
		return false;
	}

	private void updateFilesPermissions(File schemaDirectory) {
		File[] files = schemaDirectory.listFiles();
		for (File file : files) {
			file.setExecutable(true,false);
			file.setReadable(true,false);
			file.setWritable(true,false);
		}
		schemaDirectory.setExecutable(true,false);
		schemaDirectory.setReadable(true,false);
		schemaDirectory.setWritable(true,false);

	}

	private void uploadReleaseFileIntoDB(final String prospectiveVersion, final String knownVersion, final File tempFile,
										 final List<String> rf2FilesLoaded) throws ConfigurationException, BusinessServiceException {

		if (knownVersion != null && !knownVersion.trim().isEmpty()) {
			logger.info(String.format("Baseline verison: [%1s] will be combined with prospective release file: [%2s]", knownVersion, tempFile.getName()));
			//load them together here as opposed to clone the existing DB so that to make sure it is clean.
			String versionDate = knownVersion;
			if (knownVersion.length() > 8) {
				versionDate = knownVersion.substring(knownVersion.length() - 8);
			}
			final List<File> filesFound = releaseDataManager.getZipFileForKnownRelease(versionDate);
			if (filesFound != null && !filesFound.isEmpty()) {
				File preLoadedZipFile = filesFound.get(0);
				if (filesFound.size() > 1) {
					logger.info("Found more than release files with date:" + versionDate);
					String[] splits = knownVersion.split("_");
					logger.info("Release center short name:" + splits[0]);
					for (File zipFile : filesFound ) {
						if (zipFile.getName().contains(splits[0].toUpperCase())) {
							preLoadedZipFile = zipFile;
							break;
						}
					}
				}
				logger.info("Start loading release version {} with release file {} and baseline {}", 
						prospectiveVersion, tempFile.getName(), preLoadedZipFile.getName());
				releaseDataManager.loadSnomedData(prospectiveVersion,rf2FilesLoaded, tempFile, preLoadedZipFile);
			} else {
				throw new ConfigurationException("Can't find the cached release zip file for known version: " + versionDate);
			}
		} else {
			logger.info("Start loading release version {} with release file {}", prospectiveVersion, tempFile.getName());
			releaseDataManager.loadSnomedData(prospectiveVersion, rf2FilesLoaded, tempFile);
		}
		logger.info("Completed loading release version {}", prospectiveVersion);
	}

	

	/**Current extension is already loaded into the prospective version
	 * @param executionConfig
	 * @param responseMap
	 * @param validationConfig
	 * @return
	 * @throws BusinessServiceException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void combineCurrenExtensionWithDependencySnapshot(ExecutionConfig executionConfig, Map<String, Object> responseMap,ValidationRunConfig validationConfig) throws BusinessServiceException {
		String extensionVersion = executionConfig.getProspectiveVersion();
		String combinedVersion = executionConfig.getProspectiveVersion() + COMBINED;
		executionConfig.setProspectiveVersion(combinedVersion);
		logger.debug("Combined version:" + combinedVersion);
		String combinedSchema = releaseDataManager.createSchema(combinedVersion);
		if (!isKnownVersion(executionConfig.getExtensionDependencyVersion(), responseMap)) {
			throw new BusinessServiceException("Extension dependency version is not found in DB:" + executionConfig.getExtensionDependencyVersion());
		}
		if (isExtension(validationConfig)) {
			try {
				releaseDataManager.copyTableData(extensionVersion, combinedVersion, DELTA_TABLE, null);
				releaseDataManager.copyTableData(extensionVersion, combinedVersion,FULL_TABLE, null);
				releaseDataManager.copyTableData(executionConfig.getExtensionDependencyVersion(),extensionVersion, combinedVersion,SNAPSHOT_TABLE, null);
				resourceLoader.loadResourceData(combinedSchema);
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				if (errorMsg == null) {
					errorMsg = "Failed to combine current extension with the dependency version:" + executionConfig.getExtensionDependencyVersion();
				}
				responseMap.put(FAILURE_MESSAGE, errorMsg);
				throw new BusinessServiceException(errorMsg, e);
			}
		} 
	}
}
