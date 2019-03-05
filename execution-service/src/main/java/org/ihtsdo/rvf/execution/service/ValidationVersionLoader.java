package org.ihtsdo.rvf.execution.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import static org.ihtsdo.rvf.execution.service.ReleaseDataManager.*;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.util.RvfReleaseDbSchemaNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class ValidationVersionLoader {

	private static final String COMBINED = "_combined";
	private static final String RELATIONSHIP_SNAPSHOT_TABLE = "relationship_s";
	private static final String SNAPSHOT_TABLE = "%_s";

	private static final String ZIP_FILE_EXTENSION = ".zip";

	private static final String UTF_8 = "UTF-8";
	private static final String DELTA_TABLE = "%_d";
	private static final String FULL_TABLE = "%_f";
	
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Autowired
	private ResourceLoader cloudResourceLoader;
	
	@Autowired
	private ReleaseDataManager releaseDataManager;

	@Autowired
	private ValidationReportService reportService;

	@Autowired
	private ResourceDataLoader resourceLoader;

	@Resource(name = "dataSource")
	private BasicDataSource snomedDataSource;
	
	@Value("${rvf.generate.mysql.binary.archive}")
	private boolean generateBinaryArchive;

	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);
	
	public void loadPreviousVersion(MysqlExecutionConfig executionConfig) throws Exception {
		String schemaName = constructRVFSchema(executionConfig.getPreviousVersion());
		if (!releaseDataManager.isKnownRelease(schemaName)) {
			if (executionConfig.getPreviousVersion().endsWith(ZIP_FILE_EXTENSION)) {
				String rvfDbSchema = loadRelease(executionConfig.getPreviousVersion());
				executionConfig.setPreviousVersion(rvfDbSchema);
			} else {
				throw new BusinessServiceException("Previous release specified is not found " 
						+ executionConfig.getPreviousVersion());
			}
		} else {
			logger.info("Previous release exists already " + schemaName);
			executionConfig.setPreviousVersion(schemaName);
		}
	}
		
	public void loadDependncyVersion(MysqlExecutionConfig executionConfig) throws IOException, BusinessServiceException {
		String schemaName = constructRVFSchema(executionConfig.getExtensionDependencyVersion());
		if (!releaseDataManager.isKnownRelease(schemaName)) {
			if (executionConfig.getExtensionDependencyVersion().endsWith(ZIP_FILE_EXTENSION)) {
				String dependencyVersion = loadRelease(executionConfig.getExtensionDependencyVersion());
				executionConfig.setExtensionDependencyVersion(dependencyVersion);
			} else {
				throw new BusinessServiceException("Dependency release specified is not found " 
						+ executionConfig.getExtensionDependencyVersion());
			}
		} else {
			logger.info("Dependency release exists already " + schemaName);
			executionConfig.setExtensionDependencyVersion(schemaName);
		}
	}
	
	public void loadProspectiveVersion(ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig) throws Exception {
		String prospectiveVersion = executionConfig.getProspectiveVersion();
		List<String> rf2FilesLoaded = new ArrayList<>();
		String reportStorage = validationConfig.getStorageLocation();
		if (validationConfig.isRf2DeltaOnly()) {
			List<String> excludeTables = Arrays.asList(RELATIONSHIP_SNAPSHOT_TABLE);
			rf2FilesLoaded.addAll(loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(executionConfig, validationConfig, excludeTables));
		} else {
			//load prospective version alone now as used to combine with dependency for extension testing
			uploadReleaseFileIntoDB(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
		}
		statusReport.setTotalRF2FilesLoaded(rf2FilesLoaded.size());
		Collections.sort(rf2FilesLoaded);
		statusReport.setRF2Files(rf2FilesLoaded);
		reportService.writeProgress("Loading resource data for prospective schema:" + prospectiveVersion, reportStorage);
		resourceLoader.loadResourceData(prospectiveVersion);
		logger.info("completed loading resource data for schema:" + prospectiveVersion);
	}
	
	
	private String constructRVFSchema(String releaseVersion) {
		if (releaseVersion != null) {
			if (releaseVersion.endsWith(ZIP_FILE_EXTENSION)) {
				return RvfReleaseDbSchemaNameGenerator.generate(releaseVersion);
			}
			return releaseVersion.startsWith(RVF_DB_PREFIX) ? releaseVersion : RVF_DB_PREFIX + releaseVersion;
		}
		return releaseVersion;
	}
	
	private String loadRelease(String releaseVersion) throws IOException, BusinessServiceException {
		if (releaseVersion != null && releaseVersion.endsWith(ZIP_FILE_EXTENSION)) {
			String schemaName = RvfReleaseDbSchemaNameGenerator.generate(releaseVersion);
			if (!releaseDataManager.isKnownRelease(schemaName)) {
				if (!releaseDataManager.restoreReleaseFromBinaryArchive(schemaName + ZIP_FILE_EXTENSION)) {
					logger.info("No existing mysql binary release available.");
					releaseDataManager.uploadRelease(releaseVersion, schemaName);
					if (generateBinaryArchive) {
						String archiveFilename = releaseDataManager.generateBinaryArchive(schemaName);
						logger.info("Release mysql binary archive is generated:" + archiveFilename);
					}
				} 
			} 
			return schemaName;	
		}
		return releaseVersion;
	}

	public MysqlExecutionConfig createExecutionConfig(ValidationRunConfig validationConfig) {
		MysqlExecutionConfig executionConfig = new MysqlExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setProspectiveVersion(RVF_DB_PREFIX + executionConfig.getExecutionId().toString());
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setExtensionValidation( isExtension(validationConfig));
		executionConfig.setFirstTimeRelease(validationConfig.isFirstTimeRelease());
		executionConfig.setEffectiveTime(validationConfig.getEffectiveTime());
		executionConfig.setPreviousVersion(validationConfig.getPreviousRelease());
		executionConfig.setExtensionDependencyVersion(validationConfig.getExtensionDependency());
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

	public List<String> loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig,
				List<String> excludeTableNames) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		String prospectiveVersion = executionConfig.getProspectiveVersion();
		if (validationConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, validationConfig.getLocalProspectiveFile());
			if (isExtension(validationConfig)) {
				String previousVersion = executionConfig.getPreviousVersion();
				String extensionDependencyVersion = executionConfig.getExtensionDependencyVersion();
				if (!validationConfig.isFirstTimeRelease()) {
					releaseDataManager.copyTableData(previousVersion, extensionDependencyVersion, prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				} else {
					releaseDataManager.copyTableData(extensionDependencyVersion, prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				}
				
			} else {
				//copy snapshot from previous release
				if (!validationConfig.isFirstTimeRelease()) {
					releaseDataManager.copyTableData(executionConfig.getPreviousVersion(), prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
				}
			}
			releaseDataManager.updateSnapshotTableWithDataFromDelta(prospectiveVersion);
		}
		return filesLoaded;
	}
	

	public void downloadProspectiveFiles(ValidationRunConfig validationConfig) throws Exception {
		File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_RF2", ZIP_FILE_EXTENSION);
		File manifestFile = File.createTempFile("manifest_" + validationConfig.getRunId(), ".xml");
		ResourceManager jobResource = new ResourceManager(jobResourceConfig, cloudResourceLoader);
		InputStream prospectiveInput = null;
		InputStream manifestInput = null;
		//streaming file from S3 to local
		long s3StreamingStart = System.currentTimeMillis();
		String prospectiveFileFullPath = validationConfig.getProspectiveFileFullPath();
		String manifestFileFullPath = validationConfig.getManifestFileFullPath();
		if (jobResourceConfig.isUseCloud() && validationConfig.isProspectiveFileInS3()) {
			if (!jobResourceConfig.getCloud().getBucketName().equals(validationConfig.getBucketName())) {
				ManualResourceConfiguration manualConfig = new ManualResourceConfiguration(true, true, null,
						new Cloud(validationConfig.getBucketName(), ""));
				ResourceManager manualResource = new ResourceManager(manualConfig, cloudResourceLoader);
				prospectiveInput = manualResource.readResourceStreamOrNullIfNotExists(prospectiveFileFullPath);
				if (manifestFileFullPath != null) {
					manifestInput = manualResource.readResourceStreamOrNullIfNotExists(manifestFileFullPath);
				}
			} else {
				//update s3 path if required when full path containing job resource path already
				if (prospectiveFileFullPath.startsWith(jobResourceConfig.getCloud().getPath())) {
					prospectiveFileFullPath = prospectiveFileFullPath.replace(jobResourceConfig.getCloud().getPath(), "");
				}
				if (manifestFileFullPath != null && manifestFileFullPath.startsWith(jobResourceConfig.getCloud().getPath())) {
					manifestFileFullPath = manifestFileFullPath.replace(jobResourceConfig.getCloud().getPath(), "");
				}
			}
		}
		if (prospectiveInput == null) {
			prospectiveInput = jobResource.readResourceStreamOrNullIfNotExists(prospectiveFileFullPath);
		}
		if (manifestInput == null && manifestFileFullPath != null) {
			manifestInput = jobResource.readResourceStreamOrNullIfNotExists(manifestFileFullPath);
		}

		if (prospectiveInput != null && prospectiveFile != null) {
			OutputStream out = new FileOutputStream(prospectiveFile);
			IOUtils.copy(prospectiveInput, out);
			IOUtils.closeQuietly(prospectiveInput);
			IOUtils.closeQuietly(out);
			logger.debug("local prospective file" + prospectiveFile.getAbsolutePath());
			validationConfig.setLocalProspectiveFile(prospectiveFile);
		}
		if (manifestInput != null) {
			Writer output = new FileWriter(manifestFile);
			IOUtils.copy(manifestInput, output, UTF_8);
			IOUtils.closeQuietly(manifestInput);
			IOUtils.closeQuietly(output);
			validationConfig.setLocalManifestFile(manifestFile);
		}
		logger.info("Time taken {} seconds to download files {} from s3", (System.currentTimeMillis()-s3StreamingStart)/1000 ,
				prospectiveFileFullPath);
	}
	
	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependency() != null
				&& !runConfig.getExtensionDependency().trim().isEmpty());
	}

	public boolean isKnownVersion( String vertionToCheck) {
		return releaseDataManager.isKnownRelease(vertionToCheck);
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
	public void combineCurrenExtensionWithDependencySnapshot(MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig) throws BusinessServiceException {
		String extensionVersion = executionConfig.getProspectiveVersion();
		String combinedVersion = executionConfig.getProspectiveVersion() + COMBINED;
		executionConfig.setProspectiveVersion(combinedVersion);
		logger.debug("Combined version:" + combinedVersion);
		String combinedSchema = releaseDataManager.createSchema(combinedVersion);
		if (!isKnownVersion(executionConfig.getExtensionDependencyVersion())) {
			throw new BusinessServiceException("Extension dependency version is not found in DB:" + executionConfig.getExtensionDependencyVersion());
		}
		if (isExtension(validationConfig)) {
			try {
				releaseDataManager.copyTableData(extensionVersion, combinedVersion, DELTA_TABLE, null);
				releaseDataManager.copyTableData(extensionVersion, combinedVersion,FULL_TABLE, null);
				releaseDataManager.copyTableData(executionConfig.getExtensionDependencyVersion(),
						extensionVersion, combinedVersion,SNAPSHOT_TABLE, null);
				resourceLoader.loadResourceData(combinedSchema);
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				if (errorMsg == null) {
					errorMsg = "Failed to combine current extension with the dependency version:" 
							+ executionConfig.getExtensionDependencyVersion();
				}
				throw new BusinessServiceException(errorMsg, e);
			}
		} 
	}
}
