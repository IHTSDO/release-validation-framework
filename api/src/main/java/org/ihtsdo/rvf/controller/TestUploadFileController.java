package org.ihtsdo.rvf.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
import org.ihtsdo.rvf.messaging.ValidationQueueManager;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.ihtsdo.rvf.validation.StructuralTestRunner;
import org.ihtsdo.rvf.validation.TestReportable;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;
import org.ihtsdo.rvf.validation.resource.TextFileResourceProvider;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The controller that handles uploaded files for the validation to run
 */
@Controller
@Api(position = 4, value = "Validate release files")
public class TestUploadFileController {

	private static final String ZIP = ".zip";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TestUploadFileController.class);

	@Autowired
	private StructuralTestRunner structureTestRunner;
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	@Autowired
	private ReleaseDataManager releaseDataManager;
	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	Provider<ValidationRunner> validationRunnerProvider;
	@Autowired
	ValidationQueueManager queueManager;

	@RequestMapping(value = "/test-file", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "Structure tests", notes = "Uploaded files should be in RF2 format. Service can accept zip file or txt file. ")
	@ApiIgnore
	public ResponseEntity uploadTestPackage(
			@RequestParam(value = "file") final MultipartFile file,
			@RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			final HttpServletResponse response) throws IOException {
		// load the filename
		final String filename = file.getOriginalFilename();
		// must be a zip
		if (filename.endsWith(ZIP)) {
			return uploadPostTestPackage(file, writeSucceses, manifestFile,
					response);

		} else if (filename.endsWith(".txt")) {
			return uploadPreTestPackage(file, writeSucceses, response);
		} else {
			throw new IllegalArgumentException(
					"File should be pre or post and either a .txt or .zip is expected");
		}
	}

	@RequestMapping(value = "/test-post", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(position = 2, value = "Structure tests for RF2 release files", notes = "Structure tests for RF2 release files in zip file format. The manifest file is optional.")
	public ResponseEntity uploadPostTestPackage(
			@ApiParam(value="RF2 release file in zip format")@RequestParam(value = "file") final MultipartFile file,
			@ApiParam(required = false, value = "Defaults to false when not provided") @RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@ApiParam(required = false, value = "manifest.xml file") @RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			final HttpServletResponse response) throws IOException {
		// load the filename
		final String filename = file.getOriginalFilename();

		if (!filename.endsWith(ZIP)) {
			throw new IllegalArgumentException(
					"Post condition test package has to be zipped up");
		}
		File tempFile = null;
		File tempManifestFile = null;
		try {
			tempFile = File.createTempFile(filename, ZIP);
			// set up the response in order to strean directly to the response
			response.setContentType("text/csv;charset=utf-8");
			response.setHeader("Content-Disposition",
					"attachment; filename=\"report_" + filename + "_"
							+ new Date() + "\"");
			try (PrintWriter writer = response.getWriter()) {
				// must be a zip
				file.transferTo(tempFile);
				final ResourceProvider resourceManager = new ZipFileResourceProvider(
						tempFile);

				TestReportable report;

				if (manifestFile == null) {
					report = structureTestRunner.execute(resourceManager,
							writer, writeSucceses);
				} else {
					final String originalFilename = manifestFile
							.getOriginalFilename();
					tempManifestFile = File.createTempFile(originalFilename,
							".xml");
					manifestFile.transferTo(tempManifestFile);
					final ManifestFile mf = new ManifestFile(tempManifestFile);
					report = structureTestRunner.execute(resourceManager,
							writer, writeSucceses, mf);
				}
				// store the report to disk for now with a timestamp
				if (report.getNumErrors() > 0) {
					LOGGER.error("No Errors expected but got "
							+ report.getNumErrors() + " errors");
				}
			}
		} finally {

			if (tempFile != null) {
				tempFile.delete();
			}
			if (tempManifestFile != null) {
				tempManifestFile.delete();
			}
		}
		return null;
	}

	@RequestMapping(value = "/run-post", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(position = 3, value = "Run validations for a RF2 release file package.", notes = "It runs structure tests and assertion validations specified by the assertion groups. You can specify mutilple assertion group names separated by a comma. e.g common-authoring,int-authoring")
	public ResponseEntity<Map<String, String>> runPostTestPackage(
			@ApiParam(value = "RF2 release package in zip file") @RequestParam(value = "file") final MultipartFile file,
			@ApiParam(value = "True if the test file contains RF2 delta files only. Defaults to false.") @RequestParam(value = "rf2DeltaOnly", required = false) final boolean isRf2DeltaOnly,
			@ApiParam(value = "Default to false to reduce the size of report file") @RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@ApiParam(value = "manifest.xml file") @RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			@ApiParam(value = "Assertion group names separated by a comma.") @RequestParam(value = "groups") final List<String> groupsList,
			@ApiParam(value = "Required for non-first time international release testing") @RequestParam(value = "previousIntReleaseVersion", required = false) final String prevIntReleaseVersion,
			@ApiParam(value = "Required for non-first time extension release testing") @RequestParam(value = "previousExtensionReleaseVersion", required = false) final String previousExtVersion,
			@ApiParam(value = "Required for extension release testing") @RequestParam(value = "extensionDependencyReleaseVersion", required = false) final String extensionDependency,
			@ApiParam(value = "Unique number e.g Timestamp") @RequestParam(value = "runId") final Long runId,
			@ApiParam(value = "Defaults to 10 when not set") @RequestParam(value = "failureExportMax", required = false) final Integer exportMax,
			@ApiParam(value = "The sub folder for validaiton reports") @RequestParam(value = "storageLocation") final String storageLocation,
			@ApiParam(value = "True if require to create JIRA issue. Defaults to false.") @RequestParam(value = "jiraIssueCreationFlag", required = false) final boolean jiraIssueCreationFlag,
			@ApiParam(value = "Release product name (e.g SNOMED CT International edition, SNOMED CT Spanish edition, SNOMED CT Managed Service - Denmark Extension (DK), SNOMED CT Managed Service - Sweden Extension (SE), SNOMED CT to GMDN Simple Map)") @RequestParam(value = "productName", required = false) final String productName,
			@ApiParam(value = "Reproting State (e.g Pre-Alpha, Alpha feedback, Beta feedback, Pre-Production feedback, Post-Production)") @RequestParam(value = "reportingStage", required = false) final String reportingStage,
			@ApiParam(value = "Defaults to false") @RequestParam(value = "enableDrools", required = false) final boolean enableDrools,
			final HttpServletRequest request) throws IOException {

		final String requestUrl = String.valueOf(request.getRequestURL());
		final String urlPrefix = requestUrl.substring(0,
				requestUrl.lastIndexOf(request.getPathInfo()));

		final ValidationRunConfig vrConfig = new ValidationRunConfig();
		vrConfig.addFile(file).addRF2DeltaOnly(isRf2DeltaOnly)
				.addWriteSucceses(writeSucceses).addGroupsList(groupsList)
				.addManifestFile(manifestFile)
				.addPrevIntReleaseVersion(prevIntReleaseVersion)
				.addPreviousExtVersion(previousExtVersion)
				.addExtensionDependencyVersion(extensionDependency)
				.addRunId(runId).addStorageLocation(storageLocation)
				.addFailureExportMax(exportMax).addUrl(urlPrefix)
				.addJiraIssueCreationFlag(jiraIssueCreationFlag)
				.addProductName(productName)
				.addReportingStage(reportingStage)
				.addProspectiveFilesInS3(false)
				.setEnableDrools(enableDrools);

		// Before we start running, ensure that we've made our mark in the
		// storage location
		// Init will fail if we can't write the "running" state to storage
		final Map<String, String> responseMap = new HashMap<>();
		HttpStatus returnStatus = HttpStatus.OK;

		if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
			// Queue incoming validation request
			queueManager.queueValidationRequest(vrConfig, responseMap);
			final String urlToPoll = urlPrefix + "/result/" + runId
					+ "?storageLocation=" + storageLocation;
			responseMap.put("resultURL", urlToPoll);
		} else {
			returnStatus = HttpStatus.PRECONDITION_FAILED;
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}

	@RequestMapping(value = "/run-post-via-s3", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(position = 4, value = "Run validations for the release files stored in AWS S3", notes = "This api is mainly used by the RVF autoscalling instances to validate release files stored in AWS S3.")
	public ResponseEntity<Map<String, String>> runPostTestPackageViaS3(
			@ApiParam(value = "Release zip file path in AWS S3 bucket") @RequestParam(value = "releaseFileS3Path") final String releaseFileS3Path,
			@ApiParam(value = "True if the test file contains RF2 delta files only. Defaults to false.") @RequestParam(value = "rf2DeltaOnly", required = false) final boolean isRf2DeltaOnly,
			@ApiParam(value = "Defaults to false to reduce the size of report file") @RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@ApiParam(value = "manifest.xml file path in AWS S3") @RequestParam(value = "manifestFileS3Path", required = false) final String manifestFileS3Path,
			@ApiParam(value = "Assertion group names") @RequestParam(value = "groups") final List<String> groupsList,
			@ApiParam(value = "Required for non-first time international release testing") @RequestParam(value = "previousIntReleaseVersion", required = false) final String prevIntReleaseVersion,
			@ApiParam(value = "Required for non-first time extension release testing") @RequestParam(value = "previousExtensionReleaseVersion", required = false) final String previousExtVersion,
			@ApiParam(value = "Required for extension release testing") @RequestParam(value = "extensionDependencyReleaseVersion", required = false) final String extensionDependency,
			@ApiParam(value = "Unique run id e.g Timestamp") @RequestParam(value = "runId") final Long runId,
			@ApiParam(value = "Defaults to 10") @RequestParam(value = "failureExportMax", required = false) final Integer exportMax,
			@ApiParam(value = "The sub folder for validaiton reports") @RequestParam(value = "storageLocation") final String storageLocation,
			@ApiParam(value = "True if require to create JIRA issue. Defaults to false.") @RequestParam(value = "jiraIssueCreationFlag", required = false) final boolean jiraIssueCreationFlag,
			@ApiParam(value = "Release product name (e.g SNOMED CT International edition, SNOMED CT Spanish edition, SNOMED CT Managed Service - Denmark Extension (DK), SNOMED CT Managed Service - Sweden Extension (SE), SNOMED CT to GMDN Simple Map)") @RequestParam(value = "productName", required = false) final String productName,
			@ApiParam(value = "Reproting State (e.g Pre-Alpha, Alpha feedback, Beta feedback, Pre-Production feedback, Post-Production)") @RequestParam(value = "reportingStage", required = false) final String reportingStage,
			@ApiParam(value = "Defaults to false") @RequestParam(value = "enableDrools", required = false) final boolean enableDrools,
			final HttpServletRequest request) throws IOException {

		final String requestUrl = String.valueOf(request.getRequestURL());
		final String urlPrefix = requestUrl.substring(0,
				requestUrl.lastIndexOf(request.getPathInfo()));

		final ValidationRunConfig vrConfig = new ValidationRunConfig();
		vrConfig.addProspectiveFileFullPath(releaseFileS3Path)
				.addRF2DeltaOnly(isRf2DeltaOnly)
				.addWriteSucceses(writeSucceses).addGroupsList(groupsList)
				.addManifestFileFullPath(manifestFileS3Path)
				.addPrevIntReleaseVersion(prevIntReleaseVersion)
				.addPreviousExtVersion(previousExtVersion)
				.addExtensionDependencyVersion(extensionDependency)
				.addRunId(runId).addStorageLocation(storageLocation)
				.addFailureExportMax(exportMax).addUrl(urlPrefix)
				.addJiraIssueCreationFlag(jiraIssueCreationFlag)
				.addProductName(productName)
				.addReportingStage(reportingStage)
				.addProspectiveFilesInS3(true)
				.setEnableDrools(enableDrools);

		// Before we start running, ensure that we've made our mark in the
		// storage location
		// Init will fail if we can't write the "running" state to storage
		final Map<String, String> responseMap = new HashMap<>();
		HttpStatus returnStatus = HttpStatus.OK;

		if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
			// Queue incoming validation request
			queueManager.queueValidationRequest(vrConfig, responseMap);
			final String urlToPoll = urlPrefix + "/result/" + runId
					+ "?storageLocation=" + storageLocation;
			responseMap.put("resultURL", urlToPoll);
		} else {
			returnStatus = HttpStatus.PRECONDITION_FAILED;
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}

	private boolean isAssertionGroupsValid(List<String> validationGroups,
			Map<String, String> responseMap) {
		// check assertion groups
		final List<AssertionGroup> groups = assertionService
				.getAssertionGroupsByNames(validationGroups);
		if (groups.size() != validationGroups.size()) {
			final List<String> found = new ArrayList<>();
			for (final AssertionGroup group : groups) {
				found.add(group.getName());
			}
			final String groupNotFoundMsg = String.format(
					"Assertion groups requested: %s but found in RVF: %s",
					validationGroups, found);
			responseMap.put("failureMessage", groupNotFoundMsg);
			LOGGER.warn("Invalid assertion groups requested."
					+ groupNotFoundMsg);
			return false;
		}
		return true;
	}

	@RequestMapping(value = "/test-pre", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(position = 1, value = "Structure testing for release input files.", notes = "This API is for structure testing the RF2 text files used as inputs for release builds. These RF2 files are prefixed with rel2 e.g rel2_Concept_Delta_INT_20160731.txt")
	public ResponseEntity uploadPreTestPackage(
			@ApiParam(value = "RF2 input file prefixed with rel2", required = true) @RequestParam(value = "file") final MultipartFile file,
			@RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			final HttpServletResponse response) throws IOException {
		// load the filename
		final String filename = file.getOriginalFilename();

		if (!filename.startsWith("rel")) {
			LOGGER.error("Not a valid pre condition file " + filename);
			return null;
		}

		final File tempFile = File.createTempFile(filename, ".txt");
		try {
			if (!filename.endsWith(".txt")) {
				throw new IllegalArgumentException(
						"Pre condition file should always be a .txt file");
			}
			response.setContentType("text/csv;charset=utf-8");
			response.setHeader("Content-Disposition",
					"attachment; filename=\"report_" + filename + "_"
							+ new Date() + "\"");

			try (PrintWriter writer = response.getWriter()) {
				file.transferTo(tempFile);
				final ResourceProvider resourceManager = new TextFileResourceProvider(
						tempFile, filename);
				final TestReportable report = structureTestRunner.execute(
						resourceManager, writer, writeSucceses, null);
				// store the report to disk for now with a timestamp
				if (report.getNumErrors() > 0) {
					LOGGER.error("No Errors expected but got "
							+ report.getNumErrors() + " errors");
				}
			}
			return null;
		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
	}
	
	
	@RequestMapping(value = "/run-adhoc-extension-post", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(position = 3, value = "Run validations for a RF2 extension release packages.")
	public ResponseEntity<Map<String, String>> runAdhocPostTestPackage(
			@ApiParam(value = "Prospective RF2 release package in zip file") @RequestParam(value = "prospectiveFile") final MultipartFile prospectiveFile,
			@ApiParam(value = "Assertion group names separated by a comma.") @RequestParam(value = "groups") final List<String> groupsList,
			@ApiParam(value = "Required for non-first time extension release testing") @RequestParam(value = "previousExtensionReleaseVersion", required = false) final String previousExtVersion,
			@ApiParam(value = "The depdenent international release") @RequestParam(value = "extensionDependencyReleaseVersion", required = true) final String extensionDependency,
			@ApiParam(value = "Unique number e.g Timestamp") @RequestParam(value = "runId") final Long runId,
			@ApiParam(value = "Defaults to 10 when not set") @RequestParam(value = "failureExportMax", required = false) final Integer exportMax,
			@ApiParam(value = "The sub folder for validaiton reports") @RequestParam(value = "storageLocation") final String storageLocation,
			@ApiParam(value = "Defaults to false") @RequestParam(value = "enableDrools", required = false) final boolean enableDrools,
			final HttpServletRequest request) throws IOException {

		final String requestUrl = String.valueOf(request.getRequestURL());
		final String urlPrefix = requestUrl.substring(0,
				requestUrl.lastIndexOf(request.getPathInfo()));

		final ValidationRunConfig vrConfig = new ValidationRunConfig();
		vrConfig.addFile(prospectiveFile)
				.addGroupsList(groupsList)
				.addPreviousExtVersion(previousExtVersion)
				.addExtensionDependencyVersion(extensionDependency)
				.addRunId(runId).addStorageLocation(storageLocation)
				.addFailureExportMax(exportMax).addUrl(urlPrefix)
				.addProspectiveFilesInS3(false)
				.setEnableDrools(enableDrools);

		// Before we start running, ensure that we've made our mark in the
		// storage location
		// Init will fail if we can't write the "running" state to storage
		final Map<String, String> responseMap = new HashMap<>();
		HttpStatus returnStatus = HttpStatus.OK;

		if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
			// Queue incoming validation request
			queueManager.queueValidationRequest(vrConfig, responseMap);
			final String urlToPoll = urlPrefix + "/result/" + runId
					+ "?storageLocation=" + storageLocation;
			responseMap.put("resultURL", urlToPoll);
		} else {
			returnStatus = HttpStatus.PRECONDITION_FAILED;
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}
}
