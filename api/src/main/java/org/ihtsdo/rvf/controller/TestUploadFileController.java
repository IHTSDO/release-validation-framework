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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * The controller that handles uploaded files for the validation to run
 */
@Controller
@Api(value = "Test Files")
public class TestUploadFileController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestUploadFileController.class);

	@Autowired
	private StructuralTestRunner validationRunner;
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
	@ApiOperation( value = "Upload test files",
		notes = "Uploaded files should be in RF2 format files. Service can accept zip file or txt file. " )
	public ResponseEntity uploadTestPackage(@RequestParam(value = "file") final MultipartFile file,
			@RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			final HttpServletResponse response) throws IOException {
		// load the filename
		final String filename = file.getOriginalFilename();
		// must be a zip
		if (filename.endsWith(".zip")) {
			return uploadPostTestPackage(file, writeSucceses, manifestFile, response);

		} else if (filename.endsWith(".txt")) {
			return uploadPreTestPackage(file, writeSucceses, response);
		} else {
			throw new IllegalArgumentException("File should be pre or post and either a .txt or .zip is expected");
		}
	}

	@RequestMapping(value = "/test-post", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation( value = "Upload test files",
			notes = "? - TBD" )
	public ResponseEntity uploadPostTestPackage(@RequestParam(value = "file") final MultipartFile file,
			@RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			final HttpServletResponse response) throws IOException {
		// load the filename
		final String filename = file.getOriginalFilename();
	
		if (!filename.endsWith(".zip")) {
			throw new IllegalArgumentException("Post condition test package has to be zipped up");
		}
		File tempFile = null;
		File tempManifestFile = null;
		try {
			tempFile = File.createTempFile(filename, ".zip");
			// set up the response in order to strean directly to the response
			response.setContentType("text/csv;charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
			try (PrintWriter writer = response.getWriter()) {
				// must be a zip
				file.transferTo(tempFile);
				final ResourceProvider resourceManager = new ZipFileResourceProvider(tempFile);

				TestReportable report;

				if (manifestFile == null) {
					report = validationRunner.execute(resourceManager, writer, writeSucceses);
				} else {
					final String originalFilename = manifestFile.getOriginalFilename();
					tempManifestFile = File.createTempFile(originalFilename, ".xml");
					manifestFile.transferTo(tempManifestFile);
					final ManifestFile mf = new ManifestFile(tempManifestFile);
					report = validationRunner.execute(resourceManager, writer, writeSucceses, mf);
				}
				// store the report to disk for now with a timestamp
				if (report.getNumErrors() > 0) {
					LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
				}
			} 
		}finally {
			
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
	@ApiOperation( value = "Upload test files",
			notes = "? - TBD" )
	public ResponseEntity runPostTestPackage(
			@RequestParam(value = "file") final MultipartFile file,
			@RequestParam(value = "writeSuccesses", required = false) final boolean writeSucceses,
			@RequestParam(value = "manifest", required = false) final MultipartFile manifestFile,
			@RequestParam(value = "groups") final List<String> groupsList,
			@RequestParam(value = "previousIntReleaseVersion" ,required = false) final String prevIntReleaseVersion,
			@RequestParam(value = "previousExtensionReleaseVersion", required = false) final String previousExtVersion,
			@RequestParam(value = "extensionDependencyReleaseVersion", required = false) final String extensionDependency,
			@RequestParam(value = "runId") final Long runId,
			@RequestParam(value = "failureExportMax", required = false) final Integer exportMax,
			@RequestParam(value = "storageLocation") final String storageLocation,
            final HttpServletRequest request) throws IOException {

		final String requestUrl = String.valueOf(request.getRequestURL());
		final String urlPrefix = requestUrl.substring(0, requestUrl.lastIndexOf(request.getPathInfo()));

		final ValidationRunConfig vrConfig = new ValidationRunConfig();
		vrConfig.addFile(file)
				.addWriteSucceses(writeSucceses)
				.addGroupsList(groupsList)
				.addManifestFile(manifestFile)
				.addPrevIntReleaseVersion(prevIntReleaseVersion)
				.addPreviousExtVersion(previousExtVersion)
				.addExtensionDependencyVersion(extensionDependency)
				.addRunId(runId)
				.addStorageLocation(storageLocation)
				.addFailureExportMax(exportMax)
				.addUrl(urlPrefix)
				.addFirstTimeRelease( isFirstTimeRelease(prevIntReleaseVersion));
		
		
		//Before we start running, ensure that we've made our mark in the storage location
		//Init will fail if we can't write the "running" state to storage
		final Map <String, String> responseMap = new HashMap<>();
//		final ValidationRunner validationRunner = validationRunnerProvider.get();
//		final boolean initialisedOK = validationRunner.init(vrConfig, responseMap);
		HttpStatus returnStatus =  HttpStatus.OK;
		
		if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
			//Queue incoming validation request
			queueManager.queueValidationRequest( vrConfig, responseMap);
			
//			final Thread asyncValidationProcess = new Thread(validationRunner);
//			asyncValidationProcess.start();
			final String urlToPoll = urlPrefix + "/result/" + runId + "?storageLocation=" + storageLocation;
			responseMap.put("resultURL", urlToPoll);
		} else {
			returnStatus = HttpStatus.PRECONDITION_FAILED;
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}
	
	private boolean isAssertionGroupsValid(List<String> validationGroups, Map<String,String> responseMap) {
		//check assertion groups
		final List<AssertionGroup> groups = assertionService.getAssertionGroupsByNames(validationGroups);
		if (groups.size() != validationGroups.size()) {
			final List<String> found = new ArrayList<>();
			for (final AssertionGroup group : groups) {
				found.add(group.getName());
			}
			final String groupNotFoundMsg = String.format("Assertion groups requested: %s but found in RVF: %s",validationGroups, found);
			responseMap.put("failureMessage", groupNotFoundMsg);
			LOGGER.warn("Invalid assertion groups requested." + groupNotFoundMsg);
			return false;
		}
		return true;
	}

	private boolean isFirstTimeRelease(String prevIntReleaseVersion) {
		
		 return prevIntReleaseVersion == null || prevIntReleaseVersion.isEmpty() ? true: false;
	}

	@RequestMapping(value = "/test-pre", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation( value = "Upload test files",
		notes = "? - TBD" )
	public ResponseEntity uploadPreTestPackage(@RequestParam(value = "file") final MultipartFile file,
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
				throw new IllegalArgumentException("Pre condition file should always be a .txt file");
			}
			response.setContentType("text/csv;charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
			
			try (PrintWriter writer = response.getWriter()) {
				file.transferTo(tempFile);
				final ResourceProvider resourceManager = new TextFileResourceProvider(tempFile, filename);
				final TestReportable report = validationRunner.execute(resourceManager, writer, writeSucceses);
				// store the report to disk for now with a timestamp
				if (report.getNumErrors() > 0) {
					LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
				}
			}
			return null;
		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
	}

	@RequestMapping(value = "/reports/{id}", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Returns a report",
		notes = "Returns a report as txt file for a valid report id " )
	public FileSystemResource getFile(@PathVariable final String id) {
		return new FileSystemResource(new File(validationRunner.getReportDataFolder(), id+".txt"));
	}

	private List<AssertionGroup> getAssertionGroups(final List<String> items) throws IOException{

		final List<AssertionGroup> groups = new ArrayList<>();
		for(final String item: items){
			 if(item.matches("\\d+")){
                 // treat as group id and retrieve associated group
                 final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, Long.valueOf(item));
                 if(group != null){
                     groups.add(group);
                 }
             }
             else{
                 groups.add(objectMapper.readValue(item, AssertionGroup.class));
             }
		}
		return groups;
	}
}
