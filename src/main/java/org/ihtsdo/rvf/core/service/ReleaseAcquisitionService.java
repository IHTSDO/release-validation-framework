package org.ihtsdo.rvf.core.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.core.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.structure.listing.Folder;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acquires the release files a validation run needs: the prospective package
 * and its manifest, the previous release, and the dependency releases resolved
 * through {@link ModuleStorageCoordinator}.
 *
 * <p>Moved unchanged out of {@link ValidationVersionLoader}, which did two jobs:
 * fetching the files, and loading them into MySQL. Only the loading depends on
 * MySQL; the files have to be fetched whichever way they are then read.
 */
@Service
public class ReleaseAcquisitionService {

	private static final String ZIP_FILE_EXTENSION = ".zip";
	public static final String FILE_ALREADY_EXISTS_MSG = "File already exists: ";

	@Autowired
	private ModuleStorageCoordinator moduleStorageCoordinator;

	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;

	@Autowired
	private ResourceLoader cloudResourceLoader;

	@Value("${rvf.empty-release-file}")
	private String emptyRf2Filename;

	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;

	private ResourceManager releaseSourceManager;

	private final Logger logger = LoggerFactory.getLogger(ReleaseAcquisitionService.class);

	@PostConstruct
	public void init() {
		releaseSourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
	}


	public MysqlExecutionConfig createExecutionConfig(ValidationRunConfig validationConfig) {
		MysqlExecutionConfig executionConfig = new MysqlExecutionConfig(validationConfig.getRunId(), validationConfig.isFirstTimeRelease());
		executionConfig.setGroupNames(validationConfig.getGroupsList());
		executionConfig.setAssertionExclusionList(validationConfig.getAssertionExclusionList());
		executionConfig.setAssertionPacks(validationConfig.getAssertionPacks());
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
		if (mdrsRows.isEmpty()) {
			logger.info("No MDRS found from prospective file");
			return;
		}
		Set<String> expectedModules = new HashSet<>();
		if (validationConfig.getIncludedModules() != null) {
			expectedModules.addAll(Arrays.stream(validationConfig.getIncludedModules().split(",")).map(String::trim).toList());
		}

		Set<ModuleMetadata> dependencies;
		try {
			dependencies = moduleStorageCoordinator.getDependencies(mdrsRows, expectedModules, true);
		} catch (ModuleStorageCoordinatorException e) {
			throw new IOException("Failed to load dependencies via given MDRS", e);
		}

		if (!dependencies.isEmpty()) {
			String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
			for (ModuleMetadata dependency : dependencies) {
				File releaseFile = dependency.getFile();
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

	public void downloadPreviousRelease(ValidationRunConfig validationConfig) throws ModuleStorageCoordinatorException, IOException, BusinessServiceException {
		if (!StringUtils.hasLength(validationConfig.getPreviousRelease()) || emptyRf2Filename.equals(validationConfig.getPreviousRelease())) {
			return;
		}

		ModuleMetadata moduleMetadata = findModuleMetadataByFilename(validationConfig.getPreviousRelease());
		if (moduleMetadata != null) {
			downloadPreviousReleaseFromModuleStorageCoordinator(validationConfig, moduleMetadata);
		} else {
			downloadPreviousReleaseFromFallbackSource(validationConfig);
		}
	}

	private ModuleMetadata findModuleMetadataByFilename(String filename) throws ModuleStorageCoordinatorException {
		Map<String, List<ModuleMetadata>> allReleasesMap = moduleStorageCoordinator.getAllReleases();
		List<ModuleMetadata> allModuleMetadata = new ArrayList<>();
		allReleasesMap.values().forEach(allModuleMetadata::addAll);
		return allModuleMetadata.stream()
				.filter(item -> item.getFilename().equals(filename))
				.findFirst()
				.orElse(null);
	}

	private void downloadPreviousReleaseFromModuleStorageCoordinator(ValidationRunConfig validationConfig, ModuleMetadata moduleMetadata) throws ModuleStorageCoordinatorException, IOException {
		String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
		File localPreviousRelease = prepareLocalFile(localDirectory, moduleMetadata.getFilename());

		List<ModuleMetadata> moduleMetadataList = moduleStorageCoordinator.getRelease(
				moduleMetadata.getCodeSystemShortName(),
				moduleMetadata.getIdentifyingModuleId(),
				moduleMetadata.getEffectiveTimeString(),
				true,
				false);
		File releaseFile = moduleMetadataList.get(0).getFile();
		try {
			Files.copy(releaseFile.toPath(), localPreviousRelease.toPath(), StandardCopyOption.REPLACE_EXISTING);
			validationConfig.addLocalReleaseFile(localPreviousRelease);
			validationConfig.addReleaseCreationTime(moduleMetadata.getFilename(), moduleMetadataList.get(0).getFileTimeStamp().getTime());
			if (validationConfig.isReleaseAsAnEdition()) {
				processDependenciesFromFile(localPreviousRelease, validationConfig);
			}
		} finally {
			Files.delete(releaseFile.toPath());
		}
	}

	private void downloadPreviousReleaseFromFallbackSource(ValidationRunConfig validationConfig) throws IOException, BusinessServiceException {
		String warning = String.format("Previous release %s not found from Module Storage Coordinator", validationConfig.getPreviousRelease());
		logger.warn(warning);

		InputStream previousStream = releaseSourceManager.readResourceStreamOrNullIfNotExists(validationConfig.getPreviousRelease());
		if (previousStream == null) {
			throw new BusinessServiceException(String.format("Previous package %s could not be found", validationConfig.getPreviousRelease()));
		}

		String localDirectory = createRunningDirectory(validationConfig.getRunId().toString());
		File localPreviousRelease = prepareLocalFile(localDirectory, validationConfig.getPreviousRelease());
		try (OutputStream out = new FileOutputStream(localPreviousRelease)) {
			IOUtils.copy(previousStream, out);
		} finally {
			IOUtils.closeQuietly(previousStream, null);
		}
		validationConfig.addLocalReleaseFile(localPreviousRelease);
		validationConfig.addReleaseCreationTime(validationConfig.getPreviousRelease(), releaseSourceManager.getResourceLastModifiedDate(validationConfig.getPreviousRelease()));
		if (validationConfig.isReleaseAsAnEdition()) {
			try {
				processDependenciesFromFile(localPreviousRelease, validationConfig);
			} catch (ModuleStorageCoordinatorException e) {
				throw new BusinessServiceException("Failed to load dependencies via given MDRS", e);
			}
		}
	}

	private File prepareLocalFile(String localDirectory, String filename) throws IOException {
		File localFile = new File(localDirectory + Folder.SEPARATOR + filename);
		if (localFile.isFile() && localFile.exists()) {
			Files.delete(localFile.toPath());
		}
		if (!localFile.createNewFile()) {
			throw new FileExistsException(FILE_ALREADY_EXISTS_MSG + localFile.getAbsolutePath());
		}
		return localFile;
	}

	private void processDependenciesFromFile(File releaseFile, ValidationRunConfig validationConfig) throws ModuleStorageCoordinatorException {
		RF2Service rf2Service = new RF2Service();
		Set<RF2Row> mdrsRows = rf2Service.getMDRS(releaseFile, false);
		Set<String> expectedModules = new HashSet<>();
		if (validationConfig.getIncludedModules() != null) {
			expectedModules.addAll(Arrays.stream(validationConfig.getIncludedModules().split(",")).map(String::trim).toList());
		}
		Set<ModuleMetadata> dependencies = moduleStorageCoordinator.getDependencies(mdrsRows, expectedModules, false);
		if (!CollectionUtils.isEmpty(dependencies)) {
			dependencies.forEach(dependency -> {
				logger.info("Found previous dependency effective time: IdentifyingModuleId {}, EffectiveTime {}", dependency.getIdentifyingModuleId(), dependency.getEffectiveTimeString());
				validationConfig.addPreviousDependencyEffectiveTime(
					dependency.getIdentifyingModuleId(),
					dependency.getEffectiveTimeString());
			});
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
}
