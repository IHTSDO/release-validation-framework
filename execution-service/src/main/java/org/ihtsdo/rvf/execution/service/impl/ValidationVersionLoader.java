package org.ihtsdo.rvf.execution.service.impl;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Service
public class ValidationVersionLoader {

    private static final String COMBINED = "_combined";
	private static final String RELATIONSHIP_SNAPSHOT_TABLE = "relationship_s";
	private static final String PREVIOUS = "previous_";
	private static final String EXTENSIONS = "extensions";
	private static final String SNAPSHOT_TABLE = "%_s";
	private static final String SEPARATOR = "/";

	private static final String INTERNATIONAL = "international";

	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final String SQL_FILE_EXTENSION = ".sql";

	private static final String FAILURE_MESSAGE = "failureMessage";
    private static final String MYSQL_COMMAND_PATTERN = "%s %s -u %s -p%s";
    private static final String MYSQL_COMMAND_NO_PASS_PATTERN = "%s %s -u %s";
    private static final String MYSQLDUMP = "mysqldump";
    private static final String MYSQL = "mysql";
    private static final String RVF = "rvf";

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

	@Resource(name = "snomedDataSource")
	private BasicDataSource snomedDataSource;

	@Value("${rvf.snomed.jdbc.username}")
	private String username;

	@Value("${rvf.snomed.jdbc.password}")
	private String password;

	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);


	public boolean loadPreviousVersion(ExecutionConfig executionConfig, Map<String, Object> responseMap, ValidationRunConfig validationConfig) throws Exception {

		List<String> rf2FilesLoaded = new ArrayList<>();
		boolean isSucessful = true;
		String reportStorage = validationConfig.getStorageLocation();
		if (!isPublishedVersionsLoaded(validationConfig)) {
			//load published versions from s3
			String priviousVersion = PREVIOUS + executionConfig.getExecutionId();
			executionConfig.setPreviousVersion(priviousVersion);
			isSucessful = prepareVersionsFromS3FilesForPreviousVersion(validationConfig, reportStorage,responseMap, rf2FilesLoaded, executionConfig);
		}  else {
			if (isExtension(validationConfig)) {
				executionConfig.setPreviousVersion(validationConfig.getPreviousExtVersion());
			} else {
				executionConfig.setPreviousVersion(validationConfig.getPrevIntReleaseVersion());
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
				executionConfig.setPreviousVersion(validationConfig.getPreviousExtVersion());
			} else {
				executionConfig.setPreviousVersion(validationConfig.getPrevIntReleaseVersion());
			}
		} else {
			//load prospective version alone now as used to combine with dependency for extension testing
			uploadProspectiveVersion(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
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
				if (!validationConfig.isFirstTimeRelease()) {
					releaseDataManager.copyTableData(validationConfig.getPreviousExtVersion(),validationConfig.getExtensionDependency(), prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				} else {
					releaseDataManager.copyTableData(validationConfig.getExtensionDependency(), prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				}

			} else {
				//copy snapshot from previous release
				if (!validationConfig.isFirstTimeRelease()) {
					releaseDataManager.copyTableData(validationConfig.getPrevIntReleaseVersion(), prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
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

	private boolean isPublishedVersionsLoaded(ValidationRunConfig validationConfig) {
		if (validationConfig.getPrevIntReleaseVersion() != null && validationConfig.getPrevIntReleaseVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		if (validationConfig.getPreviousExtVersion() != null && validationConfig.getPreviousExtVersion().endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
        return !(validationConfig.getExtensionDependency() != null && validationConfig.getExtensionDependency().endsWith(ZIP_FILE_EXTENSION));
    }

	private void loadPublishedVersionIntoDB( FileHelper s3PublishFileHelper, String publishedReleaseFilename, String storageLocation, String rvfVersion) throws Exception {
		String[] splits = publishedReleaseFilename.split("_");
		int index = splits.length-2;
		logger.debug( "release file short name:" + splits[index]);
		String publishedFileS3Path;
		if ("INT".equalsIgnoreCase(splits[index])) {
			//derivative products released by the international release but during RVF testing using the same logic as extension.
			publishedFileS3Path = INTERNATIONAL + SEPARATOR + publishedReleaseFilename;
		} else {
			publishedFileS3Path = EXTENSIONS + SEPARATOR + splits[index] + SEPARATOR + publishedReleaseFilename;
		}
		logger.debug("downloading published file from s3:" + publishedFileS3Path); //download previous ZIP file from S3
		InputStream publishedFileInput = s3PublishFileHelper.getFileStream(publishedFileS3Path);
		if (publishedFileInput != null) {
			File tempFile = File.createTempFile(publishedReleaseFilename, ZIP_FILE_EXTENSION);
			OutputStream out = new FileOutputStream(tempFile);
			IOUtils.copy(publishedFileInput,out);
			IOUtils.closeQuietly(publishedFileInput);
			IOUtils.closeQuietly(out);
			String createdSchemaName = releaseDataManager.loadSnomedData(rvfVersion, new ArrayList<String>(),tempFile);
			/**
             * Backup database
             */
			File tempDir = FileUtils.getTempDirectory();
			File backupMyISAMFile = new File(tempDir, rvfVersion + SQL_FILE_EXTENSION);
            backupDatabase(createdSchemaName, backupMyISAMFile);
			/**
			 * Zip file
			 */
			File backupMyISAMZipFile = new File(tempDir, rvfVersion + ZIP_FILE_EXTENSION);
			ZipFileUtils.zip(backupMyISAMFile.getAbsolutePath(), backupMyISAMZipFile.getAbsolutePath());

			/**
			 * Upload to S3
			 */
			final String previousMyISAMFullPath = storageLocation + File.separator + RVF + File.separator + backupMyISAMZipFile.getName();
			s3PublishFileHelper.putFile(backupMyISAMZipFile, previousMyISAMFullPath);
			FileUtils.deleteQuietly(backupMyISAMFile);
		} else {
			logger.error("Previous release not found in the published bucket:" + publishedFileS3Path);
		}
	}

	private void createDbIfNotExists(final String schemaName) throws SQLException, IOException {
		try (Connection connection = snomedDataSource.getConnection()) {
			connection.setAutoCommit(true);
			//clean and create database
			String createDbStr = "create database if not exists "+ schemaName + ";";
			try(Statement statement = connection.createStatement()) {
				statement.execute(createDbStr);
			}
		}
	}

	/**
	 * Check whether database exists or not
	 */
	private boolean databaseExists(String schemaName) throws SQLException {
		try (Connection connection = snomedDataSource.getConnection()) {
			connection.setAutoCommit(true);
			//check whether database exists or not
			String createDbStr = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '"+ schemaName + "';";
			try(Statement statement = connection.createStatement()) {
				ResultSet resultSet = statement.executeQuery(createDbStr);
				return resultSet.first();
			}
		}
	}

	private static boolean checkMySqlDumpCommand(){
		final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);
		String executeCommand = "mysqldump1";
		try {
			Process runtimeProcess = Runtime.getRuntime().exec(executeCommand);
			InputStream inputStream = runtimeProcess.getInputStream();
			if(inputStream != null){
				logger.info("Backup successful.");
			}
			InputStream  errorStream = runtimeProcess.getErrorStream();
			if(errorStream != null){
				byte[] buffer = new byte[errorStream.available()];
				errorStream.read(buffer);
				String str = new String(buffer);
				if(str.contains("error")){
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("Error when backup database: " + e);
			return false;
		}
		return true;
	}

	private void backupDatabase(String schemaName, File backupFile){
        String executeCommand;
        if(StringUtils.isEmpty(password)){
        	executeCommand = String.format(MYSQL_COMMAND_NO_PASS_PATTERN, MYSQLDUMP, schemaName, username);
		} else {
        	executeCommand = String.format(MYSQL_COMMAND_PATTERN, MYSQLDUMP, schemaName, username, password);
		}
		try {
			Process runtimeProcess = Runtime.getRuntime().exec(executeCommand);
			InputStream inputStream = runtimeProcess.getInputStream();
			if(inputStream != null){
				logger.info("Start to backup database.");
				PrintStream printStream = new PrintStream(backupFile);
				int ch;
				while ((ch = inputStream.read()) != -1) {
					printStream.write(ch);
				}
				logger.info("Backup successful.");
			}
			InputStream  errorStream = runtimeProcess.getErrorStream();
			if(errorStream != null){
				byte[] buffer = new byte[errorStream.available()];
				errorStream.read(buffer);
				String str = new String(buffer);
				logger.error("Backup error: " + str);
			}
		} catch (Exception e) {
			logger.error("Error when backup database: " + e);
		}
	}

	private void restoreDatabase(String schemaName, File backupFile){
		String executeCommand;
		if(StringUtils.isEmpty(password)){
			executeCommand = String.format(MYSQL_COMMAND_NO_PASS_PATTERN, MYSQL, schemaName, username);
		} else {
			executeCommand = String.format(MYSQL_COMMAND_PATTERN, MYSQL, schemaName, username, password);
		}
        try {
            InputStream backupStream  = new FileInputStream(backupFile);
			Process runtimeProcess = Runtime.getRuntime().exec(executeCommand);
            OutputStream outputStream = runtimeProcess.getOutputStream();
            if(outputStream != null){
                logger.info("Start to restore database.");
                byte[] buf = new byte[8192];
                int ch;
                while ((ch = backupStream.read(buf, 0, buf.length)) > 0) {
                    outputStream.write(buf, 0, ch);
                    outputStream.flush();
                }
				logger.info("Restore successful.");
            }
            InputStream errorStream = runtimeProcess.getErrorStream();
            if(errorStream != null) {
                byte[] buffer = new byte[errorStream.available()];
                errorStream.read(buffer);
                String str = new String(buffer);
                logger.error("Restore error: " + str);
            }

            IOUtils.closeQuietly(backupStream);
		} catch (IOException e) {
			logger.error("Error when restore database: " + e);
        }
    }

	private boolean prepareVersionsFromS3FilesForPreviousVersion(ValidationRunConfig validationConfig, String reportStorage, Map<String, Object> responseMap,List<String> rf2FilesLoaded, ExecutionConfig executionConfig) throws Exception {
		FileHelper s3PublishFileHelper = new FileHelper(validationConfig.getS3PublishBucketName(), s3Client);
		if (!validationConfig.isFirstTimeRelease()) {
			if (isExtension(validationConfig)) {
				if (validationConfig.getPreviousExtVersion() != null && validationConfig.getPreviousExtVersion().endsWith(ZIP_FILE_EXTENSION)) {
					if(!databaseExists(RVF + "_" + executionConfig.getPreviousVersion())){
						if(!downloadMyISAMOnS3AndRestore(s3PublishFileHelper, validationConfig.getStorageLocation(), executionConfig.getPreviousVersion())) {
							loadPublishedVersionIntoDB(s3PublishFileHelper, validationConfig.getPreviousExtVersion(), validationConfig.getStorageLocation(), executionConfig.getPreviousVersion());
						}
					}
				}
			} else {
				if (validationConfig.getPrevIntReleaseVersion() != null && validationConfig.getPrevIntReleaseVersion().endsWith(ZIP_FILE_EXTENSION)) {
					//check database exist or not
					if(!databaseExists(RVF + "_" + executionConfig.getPreviousVersion())){
						if(!downloadMyISAMOnS3AndRestore(s3PublishFileHelper, validationConfig.getStorageLocation(), executionConfig.getPreviousVersion())) {
							loadPublishedVersionIntoDB(s3PublishFileHelper, validationConfig.getPrevIntReleaseVersion(), validationConfig.getStorageLocation(), executionConfig.getPreviousVersion());
						}
					}
				}
			}
		}

		//this code to make sure that we have a database of previous published package
		if (!databaseExists(RVF + "_" + executionConfig.getPreviousVersion())) {
			String failureMsg = "Failed to load previous version:" + (isExtension(validationConfig) ? validationConfig.getPreviousExtVersion(): validationConfig.getPrevIntReleaseVersion())
					+ " into " + executionConfig.getPreviousVersion();
			responseMap.put(FAILURE_MESSAGE, failureMsg);
			reportService.writeResults(responseMap, State.FAILED, validationConfig.getStorageLocation());
			return false;
		}
		return true;
	}

	private boolean downloadMyISAMOnS3AndRestore(FileHelper s3PublishFileHelper, String storageLocation, String rvfVersion) throws Exception {
		String previousMyISAMS3Path = storageLocation + File.separator + RVF + File.separator + rvfVersion + ZIP_FILE_EXTENSION;
		logger.debug("Downloading previous published MyISAM file from s3:" + previousMyISAMS3Path);
		InputStream publishedFileInput = s3PublishFileHelper.getFileStream(previousMyISAMS3Path);
		if (publishedFileInput != null) {
			logger.debug("Download previous published MyISAM file from s3 successfully:" + previousMyISAMS3Path);
			File restoredZipFile = File.createTempFile(rvfVersion, ZIP_FILE_EXTENSION);
			OutputStream out = new FileOutputStream(restoredZipFile);
			IOUtils.copy(publishedFileInput, out);
			IOUtils.closeQuietly(publishedFileInput);
			IOUtils.closeQuietly(out);
			/**
			 * Unzip file downloaded from S3
			 */
			ZipFileUtils.extractZipFile(restoredZipFile, restoredZipFile.getParentFile().getAbsolutePath());
			/**
			 * Create database if not exist and restore
			 */
			String schemaName = RVF + "_" + rvfVersion;
			createDbIfNotExists(schemaName);
			restoreDatabase(schemaName, restoredZipFile);
			return true;
		}
		logger.debug("Can not download previous published MyISAM file from S3:" + previousMyISAMS3Path);
		return false;
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
	 *
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
