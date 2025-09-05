package org.ihtsdo.rvf.core.service;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.ihtsdo.rvf.core.service.structure.listing.Folder;
import org.ihtsdo.rvf.core.service.util.RvfReleaseDbSchemaNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.module.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
	public static final String FILE_ALREADY_EXISTS_MSG = "File already exists: ";

	@Autowired
	private ModuleStorageCoordinator moduleStorageCoordinator;
	
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

	@Value("${rvf.generate.mysql.binary.archive}")
	private boolean generateBinaryArchive;

	@Value("${rvf.empty-release-file}")
	private String emptyRf2Filename;

	private final Logger logger = LoggerFactory.getLogger(ValidationVersionLoader.class);

	public void loadPreviousVersion(String previousRelease, Map<String, Long> releaseFileToCreationTimeMap, MysqlExecutionConfig executionConfig) throws BusinessServiceException, IOException {
		String previous = StringUtils.hasLength(previousRelease) ? previousRelease : emptyRf2Filename;
		if (previous.endsWith(ZIP_FILE_EXTENSION)) {
			if (emptyRf2Filename.equals(previous)) {
				executionConfig.setPreviousVersion(generateEmptySchema());
			} else {
				String rvfDbSchema = loadRelease(executionConfig.getLocalReleaseFiles(), previous, releaseFileToCreationTimeMap, executionConfig.getExcludedRF2Files());
				executionConfig.setPreviousVersion(rvfDbSchema);
			}
		} else {
			throw new BusinessServiceException("Previous release specified is not found: "
					+ executionConfig.getPreviousVersion());
		}
	}
		
	public void loadDependencyVersion(List<String> extensionDependencies, Map<String, Long> releaseFileToCreationTimeMap, MysqlExecutionConfig executionConfig, Set<String> schemasToRemove) throws IOException, BusinessServiceException {
		if (CollectionUtils.isEmpty(extensionDependencies)) {
			executionConfig.setExtensionDependencyVersion(generateEmptySchema());
		} else if (extensionDependencies.stream().allMatch(item -> item.endsWith(ZIP_FILE_EXTENSION))) {
			if (extensionDependencies.size() == 1) {
				loadSingleDependency(executionConfig.getLocalReleaseFiles(), extensionDependencies.get(0), releaseFileToCreationTimeMap, executionConfig, schemasToRemove);
			} else {
				loadMultipleDependencies(executionConfig.getLocalReleaseFiles(), extensionDependencies, releaseFileToCreationTimeMap, executionConfig, schemasToRemove);
			}
		} else {
			throw new BusinessServiceException("Dependency release specified is not found "
					+ executionConfig.getExtensionDependencyVersion());
		}
	}

	private String generateEmptySchema() throws BusinessServiceException {
		String schemaName = RvfReleaseDbSchemaNameGenerator.generate(emptyRf2Filename);
		if (releaseDataManager.isKnownRelease(schemaName)) {
			releaseDataManager.dropSchema(schemaName);
		}
		return releaseDataManager.createSchema(schemaName);
	}

	private void loadSingleDependency(List<File> localReleaseFiles, String extensionDependency, Map<String, Long> releaseFileToCreationTimeMap, MysqlExecutionConfig executionConfig, Set<String> schemasToRemove) throws IOException, BusinessServiceException {
		String schema = loadRelease(localReleaseFiles, extensionDependency, releaseFileToCreationTimeMap, executionConfig.getExcludedRF2Files());
		executionConfig.setExtensionDependencyVersion(schema);
		executionConfig.addCurrentDependencyRelease(extensionDependency, schema);
		if (!CollectionUtils.isEmpty(executionConfig.getExcludedRF2Files())) {
			schemasToRemove.add(schema);
		}
	}

	private void loadMultipleDependencies(List<File> localReleaseFiles, List<String> extensionDependencies, Map<String, Long> releaseFileToCreationTimeMap, MysqlExecutionConfig executionConfig, Set<String> schemasToRemove) throws IOException, BusinessServiceException {
		List<String> dependencyVersions = new ArrayList<>();
		for(String dependency : extensionDependencies) {
			String schema = loadRelease(localReleaseFiles, dependency, releaseFileToCreationTimeMap, executionConfig.getExcludedRF2Files());
			dependencyVersions.add(schema);
			executionConfig.addCurrentDependencyRelease(dependency, schema);
			if (!CollectionUtils.isEmpty(executionConfig.getExcludedRF2Files())) {
				schemasToRemove.add(schema);
			}
		}
		String targetDependencyVersion = RVF_DB_PREFIX + "dependency" + "_" + executionConfig.getExecutionId();
		executionConfig.setExtensionDependencyVersion(targetDependencyVersion);
		schemasToRemove.add(targetDependencyVersion);
		boolean success = releaseDataManager.combineKnownVersions(targetDependencyVersion, dependencyVersions.toArray(String[]::new));
		if (!success) {
			throw new BusinessServiceException("Failed to combine multiple dependencies into one dependency version");
		}
	}

	public void loadProspectiveVersion(File localProspectiveFile, ValidationStatusReport statusReport, MysqlExecutionConfig executionConfig, String reportStorage) throws BusinessServiceException, ReleaseImportException, SQLException {
		if (localProspectiveFile == null) {
			throw new BusinessServiceException("Prospective file can't be null");
		}
		String prospectiveVersion = RVF_DB_PREFIX + getProspectiveVersionFromFileNames(localProspectiveFile) + "_" + executionConfig.getExecutionId().toString();
		executionConfig.setProspectiveVersion(prospectiveVersion);
		List<String> rf2FilesLoaded = new ArrayList<>();
		if (executionConfig.isRf2DeltaOnly()) {
			rf2FilesLoaded.addAll(loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(executionConfig, localProspectiveFile, null));
		} else {
			//load prospective version alone now as used to combine with dependency for extension testing
			uploadReleaseFileIntoDB(prospectiveVersion, localProspectiveFile, rf2FilesLoaded, executionConfig.getExcludedRF2Files());

			if (!rf2DeltaFileExists(localProspectiveFile)) {
				releaseDataManager.insertIntoProspectiveDeltaTables(prospectiveVersion, executionConfig);
			}

			if (!rf2FullFileExists(localProspectiveFile)) {
				releaseDataManager.insertIntoProspectiveFullTables(prospectiveVersion);
			}
		}

		statusReport.setTotalRF2FilesLoaded(rf2FilesLoaded.size());
		Collections.sort(rf2FilesLoaded);
		statusReport.setRF2Files(rf2FilesLoaded);
		reportService.writeProgress("Loading resource data for prospective schema:" + prospectiveVersion, reportStorage);
		resourceLoader.loadResourceData(prospectiveVersion);
		logger.info("completed loading resource data for schema: {}", prospectiveVersion);
	}

	private boolean rf2DeltaFileExists(File localProspectiveFile) throws ReleaseImportException {
		File deltaDirectory = null;
		try (FileInputStream fis = new FileInputStream(localProspectiveFile)){
			deltaDirectory = new ReleaseImporter().unzipRelease(fis, ReleaseImporter.ImportType.DELTA);
			try(Stream<Path> pathStream = Files.find(deltaDirectory.toPath(), 50,
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
		} finally {
			deleteDirectory(deltaDirectory);
		}
		return false;
	}

	private boolean rf2FullFileExists(File localProspectiveFile) throws ReleaseImportException {
		File fullDirectory = null;
		try (FileInputStream fis = new FileInputStream(localProspectiveFile)) {
			fullDirectory = new ReleaseImporter().unzipRelease(fis, ReleaseImporter.ImportType.FULL);
			try(Stream<Path> pathStream = Files.find(fullDirectory.toPath(), 50,
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
		} finally {
			deleteDirectory(fullDirectory);
		}
		return false;
	}

	private void deleteDirectory(File file) {
		if (file == null) return;
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			logger.warn("Failed to remove directory {}", file.getAbsolutePath());
		}
	}

	private String loadRelease(List<File> localReleaseFiles, String releaseVersion, Map<String, Long> releaseFileToCreationTimeMap, List<String> excludedRF2Files) throws IOException, BusinessServiceException {
		if (releaseVersion == null || !releaseVersion.endsWith(ZIP_FILE_EXTENSION)) return releaseVersion;

		String schemaName = RvfReleaseDbSchemaNameGenerator.generate(releaseVersion);
		if (!CollectionUtils.isEmpty(excludedRF2Files)) {
			if (releaseDataManager.isKnownRelease(schemaName)) {
				releaseDataManager.dropSchema(schemaName);
			}
			releaseDataManager.uploadPublishedReleaseFromStore(localReleaseFiles, releaseVersion, schemaName, excludedRF2Files);
			return schemaName;
		}

		long publishedReleaseLastModifiedDate = releaseDataManager.getPublishedReleaseLastModifiedDate(releaseFileToCreationTimeMap, releaseVersion);
		long binaryArchiveSchemaLastModifiedDate = releaseDataManager.getBinaryArchiveSchemaLastModifiedDate(schemaName);

		// If the binary archive has been deleted (- or it has not been generated yet), OR the release file has been changed,
		// then the schema and the binary archive schema need to be re-generated
		if (binaryArchiveSchemaLastModifiedDate == 0 || publishedReleaseLastModifiedDate > binaryArchiveSchemaLastModifiedDate) {
			logger.info("The Binary Archive file was deleted (- or it has not been generated yet), OR a new version of published release has been detected.");
			if (releaseDataManager.isKnownRelease(schemaName)) {
				releaseDataManager.dropSchema(schemaName);
			}
			uploadPublishedReleaseThenGenerateBinaryArchive(localReleaseFiles, releaseVersion, schemaName, Collections.emptyList());
		} else {
			// Restore schema from binary archive file
			if (!releaseDataManager.isKnownRelease(schemaName) && !releaseDataManager.restoreReleaseFromBinaryArchive(schemaName)) {
				logger.info("No existing mysql binary release available.");
				uploadPublishedReleaseThenGenerateBinaryArchive(localReleaseFiles, releaseVersion, schemaName, Collections.emptyList());
			}
		}

		return schemaName;
	}

	private void uploadPublishedReleaseThenGenerateBinaryArchive(List<File> localReleaseFiles, String releaseVersion, String schemaName, List<String> excludedRF2Files) throws BusinessServiceException, FileNotFoundException {
		releaseDataManager.uploadPublishedReleaseFromStore(localReleaseFiles, releaseVersion, schemaName, excludedRF2Files);
		if (generateBinaryArchive) {
			String archiveFilename = releaseDataManager.generateBinaryArchive(schemaName);
			logger.info("Release mysql binary archive is generated: {}", archiveFilename);
		}
	}

	public MysqlExecutionConfig createExecutionConfig(ValidationRunConfig validationConfig) {
		MysqlExecutionConfig executionConfig = new MysqlExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setAssertionExclusionList(validationConfig.getAssertionExclusionList());
		executionConfig.setExcludedRF2Files(validationConfig.getExcludedRF2Files());
		executionConfig.setExtensionValidation(isExtension(validationConfig));
		executionConfig.setFirstTimeRelease(validationConfig.isFirstTimeRelease());
		executionConfig.setEffectiveTime(validationConfig.getEffectiveTime());
		executionConfig.setReleaseAsAnEdition(validationConfig.isReleaseAsAnEdition());
		executionConfig.setPreviousEffectiveTime(validationConfig.isFirstTimeRelease() ? null : extractEffectiveTimeFromVersion(validationConfig.getPreviousRelease()));
		executionConfig.setStandAloneProduct(validationConfig.isStandAloneProduct());
		executionConfig.setRf2DeltaOnly(validationConfig.isRf2DeltaOnly());
		executionConfig.setLocalReleaseFiles(validationConfig.getLocalReleaseFiles());

		if (validationConfig.getCurrentDependencyToIdentifyingModuleMap() != null) {
			for (Map.Entry<String, String> entry : validationConfig.getCurrentDependencyToIdentifyingModuleMap().entrySet()) {
				String currentDependency = entry.getKey();
				String identifyingModule = entry.getValue();
				if (validationConfig.getPreviousDependencyEffectiveTimeMap() != null && validationConfig.getPreviousDependencyEffectiveTimeMap().containsKey(identifyingModule)) {
					String previousDependencyEffectiveTime = validationConfig.getPreviousDependencyEffectiveTimeMap().get(identifyingModule);
					executionConfig.addCurrentDependencyToPreviousEffectiveTime(currentDependency, previousDependencyEffectiveTime);
					logger.info("Current dependency {} - found previous dependency effective {}.", currentDependency, previousDependencyEffectiveTime);
				}
			}
		}

		// Max failure export. Default to 10
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
		return executionConfig;
	}

	private String getProspectiveVersionFromFileNames(File localProspectiveFile) throws BusinessServiceException {
		return localProspectiveFile != null ? releaseDataManager.getEditionAndVersion(localProspectiveFile) : "";
	}

	public List<String> loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(MysqlExecutionConfig executionConfig, File localProspectiveFile,
				List<String> excludeTableNames) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		String prospectiveVersion = executionConfig.getProspectiveVersion();
		if (executionConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, executionConfig.getExcludedRF2Files(), localProspectiveFile);

			// copy snapshot from previous release. If no previous release - then the empty schema will be used
			releaseDataManager.copyTableData(executionConfig.getPreviousVersion(), prospectiveVersion, SNAPSHOT_TABLE, excludeTableNames);

			releaseDataManager.updateSnapshotTableWithDataFromDelta(prospectiveVersion);
		}
		return filesLoaded;
	}
	

	public void downloadProspectiveFiles(ValidationRunConfig validationConfig) throws IOException {
		String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
		String prospectiveFilename = validationConfig.getProspectiveFileFullPath().substring(validationConfig.getProspectiveFileFullPath().lastIndexOf(Folder.SEPARATOR) + 1);
		File prospectiveFile = new File (localDirectory + Folder.SEPARATOR + prospectiveFilename);
		if (prospectiveFile.isFile() && prospectiveFile.exists()) {
			Files.delete(prospectiveFile.toPath());
		}
		if (!prospectiveFile.createNewFile()) {
			throw new FileExistsException(FILE_ALREADY_EXISTS_MSG + prospectiveFile.getAbsolutePath());
		}
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
			validationConfig.addLocalReleaseFile(prospectiveFile);
		}
		if (manifestInput != null) {
			String manifestFilename = validationConfig.getManifestFileFullPath().substring(validationConfig.getManifestFileFullPath().lastIndexOf(Folder.SEPARATOR) + 1);
			File manifestFile = new File (localDirectory + Folder.SEPARATOR + manifestFilename);
			if (manifestFile.isFile() && manifestFile.exists()) {
				Files.delete(manifestFile.toPath());
			}
			if (!manifestFile.createNewFile()) {
				throw new FileExistsException(FILE_ALREADY_EXISTS_MSG + manifestFile.getAbsolutePath());
			}

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

	public void downloadDependencyReleases(ValidationRunConfig validationConfig) throws IOException {
		RF2Service rf2Service = new RF2Service();
		Set<RF2Row> mdrsRows = rf2Service.getMDRS(validationConfig.getLocalProspectiveFile(), validationConfig.isRf2DeltaOnly());
		Set<ModuleMetadata> dependencies = moduleStorageCoordinator.getDependencies(mdrsRows, true);
		if (!dependencies.isEmpty()) {
			String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
			for (ModuleMetadata dependency : dependencies) {
				File releaseFile = dependency.getFile();
				// At the moment, RVF only allows one dependency. So that the first one will be picked up
				File localDependency = new File (localDirectory + Folder.SEPARATOR + dependency.getFilename());
				if (localDependency.isFile() && localDependency.exists()) {
					Files.delete(localDependency.toPath());
				}
				if (localDependency.createNewFile()) {
					Files.copy(releaseFile.toPath(), localDependency.toPath(), StandardCopyOption.REPLACE_EXISTING);
					validationConfig.addExtensionDependency(dependency.getFilename());
					validationConfig.addLocalReleaseFile(localDependency);
					validationConfig.addReleaseCreationTime(dependency.getFilename(), dependency.getFileTimeStamp().getTime());
					validationConfig.addCurrentDependencyToIdentifyingModuleMap(dependency.getFilename(), dependency.getIdentifyingModuleId());
					Files.delete(releaseFile.toPath());
					logger.info("Dependency {} found from Module Storage Coordinator", dependency.getFilename());
				} else {
					throw new FileExistsException(FILE_ALREADY_EXISTS_MSG + localDependency.getAbsolutePath());
				}
			}
		} else {
			logger.info("No dependency found from Module Storage Coordinator");
		}

	}

	public void downloadPreviousRelease(ValidationRunConfig validationConfig) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException, IOException {
		if (!StringUtils.hasLength(validationConfig.getPreviousRelease()) || emptyRf2Filename.equals(validationConfig.getPreviousRelease())) return;

		// Get all releases from MSC
		Map<String, List<ModuleMetadata>> allReleasesMap = moduleStorageCoordinator.getAllReleases();
		List<ModuleMetadata> allModuleMetadata = new ArrayList<>();
		allReleasesMap.values().forEach(allModuleMetadata::addAll);

		ModuleMetadata moduleMetadata = allModuleMetadata.stream().filter(item -> item.getFilename().equals(validationConfig.getPreviousRelease())).findFirst().orElse(null);
		if (moduleMetadata != null) {
			String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
			File localPreviousRelease = new File (localDirectory + Folder.SEPARATOR + moduleMetadata.getFilename());
			if (localPreviousRelease.isFile() && localPreviousRelease.exists()) {
				Files.delete(localPreviousRelease.toPath());
			}
			if (localPreviousRelease.createNewFile()) {
				List<ModuleMetadata> moduleMetadataList = moduleStorageCoordinator.getRelease(moduleMetadata.getCodeSystemShortName(), moduleMetadata.getIdentifyingModuleId(), moduleMetadata.getEffectiveTimeString(), true, false);
				File releaseFile = moduleMetadataList.get(0).getFile();
				Files.copy(releaseFile.toPath(), localPreviousRelease.toPath(), StandardCopyOption.REPLACE_EXISTING);
				validationConfig.addLocalReleaseFile(localPreviousRelease);
				validationConfig.addReleaseCreationTime(moduleMetadata.getFilename(), moduleMetadataList.get(0).getFileTimeStamp().getTime());

				RF2Service rf2Service = new RF2Service();
				Set<RF2Row> mdrsRows = rf2Service.getMDRS(localPreviousRelease, false);
				Set<ModuleMetadata> dependencies = moduleStorageCoordinator.getDependencies(mdrsRows, false);
				if (!CollectionUtils.isEmpty(dependencies)) {
					dependencies.forEach(dependency -> validationConfig.addPreviousDependencyEffectiveTime(dependency.getIdentifyingModuleId(), dependency.getEffectiveTimeString()));
				}
				Files.delete(releaseFile.toPath());
			} else {
				throw new FileExistsException(FILE_ALREADY_EXISTS_MSG + localPreviousRelease.getAbsolutePath());
			}
		} else {
			String error = String.format("Previous release %s not found from Module Storage Coordinator", validationConfig.getPreviousRelease());
			logger.error(error);
			throw new ModuleStorageCoordinatorException.ResourceNotFoundException(error);
		}
	}

	private String createRunningDirectory(String runId) throws IOException {
		String tmpDirsLocation = System.getProperty("java.io.tmpdir");
		Path path = Paths.get(tmpDirsLocation, runId);
		File directory = new File (path.toString());
		if (directory.exists() && directory.isDirectory()) {
			return directory.getAbsolutePath();
		}
		return Files.createDirectories(path).toFile().getAbsolutePath();
	}

	private boolean isExtension(final ValidationRunConfig runConfig) {
		return (runConfig.getExtensionDependencies() != null
				&& !runConfig.getExtensionDependencies().isEmpty());
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

	private void uploadReleaseFileIntoDB(final String prospectiveVersion, final File tempFile,
										 final List<String> rf2FilesLoaded, List<String> excludedRF2Files) throws BusinessServiceException {
		logger.info("Start loading release version {} with release file {}", prospectiveVersion, tempFile.getName());
		releaseDataManager.loadSnomedData(prospectiveVersion, rf2FilesLoaded, excludedRF2Files, tempFile);
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
