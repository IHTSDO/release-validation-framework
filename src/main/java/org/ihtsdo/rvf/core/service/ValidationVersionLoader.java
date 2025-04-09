package org.ihtsdo.rvf.core.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.core.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.ihtsdo.rvf.core.service.util.RvfReleaseDbSchemaNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.ConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.ihtsdo.rvf.core.service.ReleaseDataManager.RVF_DB_PREFIX;

@Service
public class ValidationVersionLoader {

	private static final String COMBINED = "_combined";
	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final String SNAPSHOT_TABLE = "%_s";
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

	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;

	private ResourceManager releaseSourceManager;

	@Resource(name = "dataSource")
	private BasicDataSource snomedDataSource;
	
	@Value("${rvf.generate.mysql.binary.archive}")
	private boolean generateBinaryArchive;

	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);

	@PostConstruct
	public void init() {
		releaseSourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
	}
	
	public void loadPreviousVersion(MysqlExecutionConfig executionConfig) throws BusinessServiceException, IOException {
		if (executionConfig.getPreviousVersion().endsWith(ZIP_FILE_EXTENSION)) {
			String rvfDbSchema = loadRelease(executionConfig.getPreviousVersion());
			executionConfig.setPreviousVersion(rvfDbSchema);
		} else {
			throw new BusinessServiceException("Previous release specified is not found: "
					+ executionConfig.getPreviousVersion());
		}
	}
		
	public void loadDependencyVersion(MysqlExecutionConfig executionConfig) throws IOException, BusinessServiceException {
		if (executionConfig.getExtensionDependencyVersion().endsWith(ZIP_FILE_EXTENSION)) {
			String dependencyVersion = loadRelease(executionConfig.getExtensionDependencyVersion());
			executionConfig.setExtensionDependencyVersion(dependencyVersion);
		} else {
			throw new BusinessServiceException("Dependency release specified is not found "
					+ executionConfig.getExtensionDependencyVersion());
		}
	}
	
	public void loadProspectiveVersion(ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig) throws BusinessServiceException, ReleaseImportException, SQLException, ConfigurationException {
		String prospectiveVersion = executionConfig.getProspectiveVersion();
		List<String> rf2FilesLoaded = new ArrayList<>();
		String reportStorage = validationConfig.getStorageLocation();
		if (validationConfig.isRf2DeltaOnly()) {
			rf2FilesLoaded.addAll(loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(executionConfig, validationConfig, null));
		} else {
			//load prospective version alone now as used to combine with dependency for extension testing
			uploadReleaseFileIntoDB(prospectiveVersion, null, validationConfig.getLocalProspectiveFile(), rf2FilesLoaded);
		}
		final String schemaName = prospectiveVersion.startsWith(RVF_DB_PREFIX) ? prospectiveVersion : RVF_DB_PREFIX + prospectiveVersion;
		if (!validationConfig.isRf2DeltaOnly() && !checkDeltaFilesExist(validationConfig.getLocalProspectiveFile())) {
			releaseDataManager.insertIntoProspectiveDeltaTables(schemaName, executionConfig);
		}
		if (!checkFullFilesExist(validationConfig.getLocalProspectiveFile())) {
			releaseDataManager.insertIntoProspectiveFullTables(schemaName);
		}

		statusReport.setTotalRF2FilesLoaded(rf2FilesLoaded.size());
		Collections.sort(rf2FilesLoaded);
		statusReport.setRF2Files(rf2FilesLoaded);
		reportService.writeProgress("Loading resource data for prospective schema:" + prospectiveVersion, reportStorage);
		resourceLoader.loadResourceData(prospectiveVersion);
		logger.info("completed loading resource data for schema: {}", prospectiveVersion);
	}

	private boolean checkDeltaFilesExist(File localProspectiveFile) throws ReleaseImportException {
		try {
			String deltaDirectoryPath = new ReleaseImporter().unzipRelease(new FileInputStream(localProspectiveFile), ReleaseImporter.ImportType.DELTA).getAbsolutePath();
			try(Stream<Path> pathStream = Files.find(new File(deltaDirectoryPath).toPath(), 50,
					(path, basicFileAttributes) -> path.toFile().getName().matches("x?(sct|rel)2_Concept_[^_]*Delta_.*.txt"))) {
				if (pathStream.findFirst().isPresent()) {
					return true;
				}
			}
		} catch (IOException | IllegalStateException e) {
			if (e.getMessage().contains("No Delta files found")) {
				return false;
			}
			throw new ReleaseImportException("Error while searching input files.", e);
		}
		return false;
	}

	private boolean checkFullFilesExist(File localProspectiveFile) throws ReleaseImportException {
		try {
			String deltaDirectoryPath = new ReleaseImporter().unzipRelease(new FileInputStream(localProspectiveFile), ReleaseImporter.ImportType.FULL).getAbsolutePath();
			try(Stream<Path> pathStream = Files.find(new File(deltaDirectoryPath).toPath(), 50,
					(path, basicFileAttributes) -> path.toFile().getName().matches("x?(sct|rel)2_Concept_[^_]*Full_.*.txt"))) {
				if (pathStream.findFirst().isPresent()) {
					return true;
				}
			}
		} catch (IOException | IllegalStateException e) {
			if (e.getMessage().contains("No Full files found")) {
				return false;
			}
			throw new ReleaseImportException("Error while searching input files.", e);
		}
		return false;
	}

	private String loadRelease(String releaseVersion) throws IOException, BusinessServiceException {
		if (releaseVersion != null && releaseVersion.endsWith(ZIP_FILE_EXTENSION)) {
			String schemaName = RvfReleaseDbSchemaNameGenerator.generate(releaseVersion);
			long publishedReleaseLastModifiedDate = releaseDataManager.getPublishedReleaseLastModifiedDate(releaseVersion);
			long binaryArchiveSchemaLastModifiedDate = releaseDataManager.getBinaryArchiveSchemaLastModifiedDate(schemaName);

			// If the binary archive has been deleted (- or it has not been generated yet), OR the release file has been changed,
			// then the schema and the binary archive schema need to be re-generated
			if (binaryArchiveSchemaLastModifiedDate == 0 || publishedReleaseLastModifiedDate > binaryArchiveSchemaLastModifiedDate) {
				logger.info("The Binary Archive file was deleted (- or it has not been generated yet), OR a new version of published release has been detected.");
				if (releaseDataManager.isKnownRelease(schemaName)) {
					releaseDataManager.dropSchema(schemaName);
				}
				uploadPublishedReleaseThenGenerateBinaryArchive(releaseVersion, schemaName);
			} else {
				// Restore schema from binary archive file
				if (!releaseDataManager.isKnownRelease(schemaName) && !releaseDataManager.restoreReleaseFromBinaryArchive(schemaName)) {
					logger.info("No existing mysql binary release available.");
					uploadPublishedReleaseThenGenerateBinaryArchive(releaseVersion, schemaName);
				}
			}

			return schemaName;
		}
		return releaseVersion;
	}

	private void uploadPublishedReleaseThenGenerateBinaryArchive(String releaseVersion, String schemaName) throws BusinessServiceException {
		releaseDataManager.uploadPublishedReleaseFromStore(releaseVersion, schemaName);
		if (generateBinaryArchive) {
			String archiveFilename = releaseDataManager.generateBinaryArchive(schemaName);
			logger.info("Release mysql binary archive is generated: {}", archiveFilename);
		}
	}

	public MysqlExecutionConfig createExecutionConfig(ValidationRunConfig validationConfig) throws BusinessServiceException {
		MysqlExecutionConfig executionConfig = new MysqlExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setProspectiveVersion(RVF_DB_PREFIX + getProspectiveVersionFromFileNames(validationConfig) 
							+ "_" + executionConfig.getExecutionId().toString());
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setAssertionExclusionList(validationConfig.getAssertionExclusionList());
		executionConfig.setExtensionValidation(isExtension(validationConfig));
		executionConfig.setFirstTimeRelease(validationConfig.isFirstTimeRelease());
		executionConfig.setEffectiveTime(validationConfig.getEffectiveTime());
		executionConfig.setPreviousEffectiveTime(validationConfig.isFirstTimeRelease() ? null : extractEffectiveTimeFromVersion(validationConfig.getPreviousRelease()));
		executionConfig.setPreviousVersion(validationConfig.getPreviousRelease());
		executionConfig.setExtensionDependencyVersion(validationConfig.getExtensionDependency());
		executionConfig.setPreviousDependencyEffectiveTime(validationConfig.getPreviousDependencyEffectiveTime());
		if(validationConfig.getExtensionDependency() != null) {
			executionConfig.setDependencyEffectiveTime(extractEffectiveTimeFromVersion(validationConfig.getExtensionDependency()));
		}
		//default to 10
		executionConfig.setFailureExportMax(10);
		if (validationConfig.getFailureExportMax() != null) {
			executionConfig.setFailureExportMax(validationConfig.getFailureExportMax());
		}

		executionConfig.setDefaultModuleId(validationConfig.getDefaultModuleId());
		List<String> includedModules = new ArrayList<>();
		if (validationConfig.getIncludedModules() != null) {
			includedModules.addAll(Arrays.stream(validationConfig.getIncludedModules().split(",")).map(String::trim).toList());
		}
		executionConfig.setIncludedModules(includedModules);
		executionConfig.setStandAloneProduct(validationConfig.isStandAloneProduct());
		executionConfig.setReleaseValidation(!validationConfig.isRf2DeltaOnly());
		return executionConfig;
	}

	private String getProspectiveVersionFromFileNames(ValidationRunConfig validationConfig) throws BusinessServiceException {
		if (validationConfig.getLocalProspectiveFile() == null) {
			return null;
		}
		return releaseDataManager.getEditionAndVersion(validationConfig.getLocalProspectiveFile());
	}

	public List<String> loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig,
				List<String> excludeTableNames) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		String prospectiveVersion = executionConfig.getProspectiveVersion();
		if (validationConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, validationConfig.getLocalProspectiveFile());
			//copy snapshot from previous release
			if (!validationConfig.isFirstTimeRelease()) {
				releaseDataManager.copyTableData(executionConfig.getPreviousVersion(), prospectiveVersion,SNAPSHOT_TABLE, excludeTableNames);
			}
			releaseDataManager.updateSnapshotTableWithDataFromDelta(prospectiveVersion);
		}
		return filesLoaded;
	}
	

	public void downloadProspectiveFiles(ValidationRunConfig validationConfig) throws IOException {
		File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_RF2", ZIP_FILE_EXTENSION);
		File manifestFile = File.createTempFile("manifest_" + validationConfig.getRunId(), ".xml");
		ResourceManager jobResource = new ResourceManager(jobResourceConfig, cloudResourceLoader);

		//streaming file from S3 to local
		long s3StreamingStart = System.currentTimeMillis();
		InputStream prospectiveInput = downloadProspectiveReleaseFile(validationConfig, jobResource);
		InputStream manifestInput = downloadProspectiveManifestFile(validationConfig, jobResource);
		if (prospectiveInput != null) {
			try (OutputStream out = new FileOutputStream(prospectiveFile)) {
				IOUtils.copy(prospectiveInput, out);
			} finally {
				IOUtils.closeQuietly(prospectiveInput, null);
			}
			logger.debug("local prospective file {}", prospectiveFile.getAbsolutePath());
			validationConfig.setLocalProspectiveFile(prospectiveFile);
		}
		if (manifestInput != null) {
			// Copy manifest input stream to local file
			try (Writer out = new FileWriter(manifestFile)) {
				IOUtils.copy(manifestInput, out, StandardCharsets.UTF_8);
			} finally {
				IOUtils.closeQuietly(manifestInput, null);
			}
			validationConfig.setLocalManifestFile(manifestFile);
		}
		logger.info("Time taken {} seconds to download files {} from s3", (System.currentTimeMillis()-s3StreamingStart)/1000 ,
				validationConfig.getProspectiveFileFullPath());
	}

	private InputStream downloadProspectiveReleaseFile(ValidationRunConfig validationConfig, ResourceManager jobResource) throws IOException {
		InputStream prospectiveInput = null;
		//streaming file from S3 to local
		String prospectiveFileFullPath = validationConfig.getProspectiveFileFullPath();
		if (jobResourceConfig.isUseCloud() && validationConfig.isProspectiveFileInS3()) {
			if (!jobResourceConfig.getCloud().getBucketName().equals(validationConfig.getBucketName())) {
				ManualResourceConfiguration manualConfig = new ManualResourceConfiguration(true, true, null,
						new Cloud(validationConfig.getBucketName(), ""));
				ResourceManager manualResource = new ResourceManager(manualConfig, cloudResourceLoader);
				prospectiveInput = manualResource.readResourceStreamOrNullIfNotExists(prospectiveFileFullPath);
			} else {
				//update s3 path if required when full path containing job resource path already
				if (prospectiveFileFullPath.startsWith(jobResourceConfig.getCloud().getPath())) {
					prospectiveFileFullPath = prospectiveFileFullPath.replace(jobResourceConfig.getCloud().getPath(), "");
				}
			}
		}
		if (prospectiveInput == null) {
			prospectiveInput = jobResource.readResourceStreamOrNullIfNotExists(prospectiveFileFullPath);
		}
		return prospectiveInput;
	}

	private InputStream downloadProspectiveManifestFile(ValidationRunConfig validationConfig, ResourceManager jobResource) throws IOException {
		InputStream manifestInput = null;
		//streaming file from S3 to local
		String manifestFileFullPath = validationConfig.getManifestFileFullPath();
		if (jobResourceConfig.isUseCloud() && validationConfig.isProspectiveFileInS3()) {
			if (!jobResourceConfig.getCloud().getBucketName().equals(validationConfig.getBucketName())) {
				ManualResourceConfiguration manualConfig = new ManualResourceConfiguration(true, true, null,
						new Cloud(validationConfig.getBucketName(), ""));
				ResourceManager manualResource = new ResourceManager(manualConfig, cloudResourceLoader);
				if (manifestFileFullPath != null) {
					manifestInput = manualResource.readResourceStreamOrNullIfNotExists(manifestFileFullPath);
				}
			} else {
				//update s3 path if required when full path containing job resource path already
				if (manifestFileFullPath != null && manifestFileFullPath.startsWith(jobResourceConfig.getCloud().getPath())) {
					manifestFileFullPath = manifestFileFullPath.replace(jobResourceConfig.getCloud().getPath(), "");
				}
			}
		}
		if (manifestInput == null && manifestFileFullPath != null) {
			manifestInput = jobResource.readResourceStreamOrNullIfNotExists(manifestFileFullPath);
		}

		return manifestInput;
	}

	public void downloadPreviousReleaseAndDependencyFiles(ValidationRunConfig validationConfig) throws IOException {
		if (StringUtils.hasLength(validationConfig.getExtensionDependency())) {
			InputStream dependencyStream = releaseSourceManager.readResourceStreamOrNullIfNotExists(validationConfig.getExtensionDependency());
			if (dependencyStream != null) {
				File dependencyFile = File.createTempFile(validationConfig.getRunId() + "_DEPENDENCY_RF2", ZIP_FILE_EXTENSION);
				try (OutputStream out = new FileOutputStream(dependencyFile)) {
					IOUtils.copy(dependencyStream, out);
				} finally {
					IOUtils.closeQuietly(dependencyStream, null);
				}
				validationConfig.setLocalDependencyReleaseFile(dependencyFile);
			}
		}

		if (StringUtils.hasLength(validationConfig.getPreviousRelease())) {
			InputStream previousStream = releaseSourceManager.readResourceStreamOrNullIfNotExists(validationConfig.getPreviousRelease());
			if (previousStream != null) {
				File previousFile = File.createTempFile(validationConfig.getRunId() + "_PREVIOUS_RF2", ZIP_FILE_EXTENSION);
				try (OutputStream out = new FileOutputStream(previousFile)) {
					IOUtils.copy(previousStream, out);
				} finally {
					IOUtils.closeQuietly(previousStream, null);
				}
				validationConfig.setLocalPreviousReleaseFile(previousFile);
			}
		}
	}
	
	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependency() != null
				&& !runConfig.getExtensionDependency().trim().isEmpty());
	}

	public boolean isUnknownVersion( String versionToCheck) {
		return !releaseDataManager.isKnownRelease(versionToCheck);
	}
	
	private String extractEffectiveTimeFromVersion(String dependencyVersion) {
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
			logger.info("Baseline version: {} will be combined with prospective release file: {}", knownVersion, tempFile.getName());
			//load them together here as opposed to clone the existing DB so that to make sure it is clean.
			String versionDate = knownVersion;
			if (knownVersion.length() > 8) {
				versionDate = knownVersion.substring(knownVersion.length() - 8);
			}
			final List<File> filesFound = releaseDataManager.getZipFileForKnownRelease(versionDate);
			if (filesFound != null && !filesFound.isEmpty()) {
				File preLoadedZipFile = filesFound.get(0);
				if (filesFound.size() > 1) {
					logger.info("Found more than release files with date: {}", versionDate);
					String[] splits = knownVersion.split("_");
					logger.info("Release center short name: {}", splits[0]);
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
	 * @param validationConfig
	 * @return
	 * @throws BusinessServiceException 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void combineCurrentExtensionWithDependencySnapshot(MysqlExecutionConfig executionConfig, ValidationRunConfig validationConfig) throws BusinessServiceException {
		String extensionVersion = executionConfig.getProspectiveVersion();
		String combinedVersion = executionConfig.getProspectiveVersion() + COMBINED;
		executionConfig.setProspectiveVersion(combinedVersion);
		logger.debug("Combined version: {}", combinedVersion);
		String combinedSchema = releaseDataManager.createSchema(combinedVersion);
		if (isUnknownVersion(executionConfig.getExtensionDependencyVersion())) {
			throw new BusinessServiceException("Extension dependency version is not found in DB:" + executionConfig.getExtensionDependencyVersion());
		}
		if (isExtension(validationConfig)) {
			try {
				releaseDataManager.copyTableData(extensionVersion, combinedVersion, DELTA_TABLE, null);
				releaseDataManager.copyTableData(extensionVersion, combinedVersion, FULL_TABLE, null);
				releaseDataManager.copyTableData(executionConfig.getExtensionDependencyVersion(),
						extensionVersion, combinedVersion, SNAPSHOT_TABLE, null);
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
