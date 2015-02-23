package org.ihtsdo.rvf.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.ihtsdo.rvf.validation.TestReportable;
import org.ihtsdo.rvf.validation.ValidationTestRunner;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceManager;
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
import com.google.common.io.Files;

/**
 * The controller that handles uploaded files for the validation to run
 */
@Controller
@Api(value = "Test Files")
public class TestUploadFileController {

	private static final String FAILURE_MESSAGE = "failureMessage";

	private static final Logger LOGGER = LoggerFactory.getLogger(TestUploadFileController.class);

	@Autowired
	private ValidationTestRunner validationRunner;
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

		final File tempFile = File.createTempFile(filename, ".zip");
		tempFile.deleteOnExit();
		if (!filename.endsWith(".zip")) {
			throw new IllegalArgumentException("Post condition test package has to be zipped up");
		}

		// set up the response in order to strean directly to the response
		response.setContentType("text/csv;charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
		try (PrintWriter writer = response.getWriter()) {
			// must be a zip
			copyUploadToDisk(file, tempFile);
			final ResourceManager resourceManager = new ZipFileResourceProvider(tempFile);

			TestReportable report;

			if (manifestFile == null) {
				report = validationRunner.execute(resourceManager, writer, writeSucceses);
			} else {
				final String originalFilename = manifestFile.getOriginalFilename();
				final File tempManifestFile = File.createTempFile(originalFilename, ".xml");
				tempManifestFile.deleteOnExit();
				copyUploadToDisk(manifestFile, tempManifestFile);

				final ManifestFile mf = new ManifestFile(tempManifestFile);
				report = validationRunner.execute(resourceManager, writer, writeSucceses, mf);
			}

			// store the report to disk for now with a timestamp
			if (report.getNumErrors() > 0) {
				LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
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
			@RequestParam(value = "previousIntReleaseVersion") final String prevIntReleaseVersion,
			@RequestParam(value = "previousExtensionReleaseVersion", required = false) final String previousExtVersion,
			@RequestParam(value = "extensionBaseLineReleaseVersion", required = false) final String extensionBaseLine,
			@RequestParam(value = "runId") final Long runId,
            final HttpServletRequest request) throws IOException {
		//TODO needs to refactor this method further
        final Calendar startTime = Calendar.getInstance();
        LOGGER.info(String.format("Started execution with runId [%1s] : ", runId));
        // generate url from request so we can display in response
        final String requestUrl = String.valueOf(request.getRequestURL());
        final String urlPrefix = requestUrl.substring(0, requestUrl.lastIndexOf(request.getPathInfo()));
        final Map<String , Object> responseMap = new HashMap<>();
        // load the filename
        final String filename = file.getOriginalFilename();
        final File   tempFile = File.createTempFile(filename, ".zip");
        tempFile.deleteOnExit();
        if (!filename.endsWith(".zip")) {
        	responseMap.put("type", "pre");
        	responseMap.put("assertionsFailed", -1L);
        	responseMap.put(FAILURE_MESSAGE, "Post condition test package has to be zipped up");
        	return new ResponseEntity<>(responseMap, HttpStatus.OK);
        }
        // must be a zip
        copyUploadToDisk(file, tempFile);
        boolean isFailed = verifyZipFileStructure(responseMap, tempFile, runId, manifestFile, writeSucceses, urlPrefix);
        if (isFailed) {
        	  return new ResponseEntity<>(responseMap, HttpStatus.OK);
        } else {
        	isFailed = checkKnownVersion(prevIntReleaseVersion,previousExtVersion,extensionBaseLine,responseMap);
        	if (isFailed) {
        		return new ResponseEntity<>(responseMap, HttpStatus.OK);
        	}
        	// TODO We could move all the following code to another thread using TaskExecutor and return the URL for 
        	//assertion results something like: https://rvf.ihtsdotools.org/api/v1/assertionresults/{run_id}
        	//We need to create AssertionResultService which queries the qa_result table for a given run_id

        	final String[] tokens = Files.getNameWithoutExtension(file.getOriginalFilename()).split("_");
        	final String releaseDate = tokens[tokens.length-1];
    		String prospectiveVersion = releaseDate ;
    		String prevReleaseVersion = prevIntReleaseVersion;
    		final boolean isExtension = previousExtVersion != null ? true : false;
        	if (isExtension) {
        		//SnomedCT_Release-es_INT_20140430.zip
        		//SnomedCT_SpanishRelease_INT_20141031.zip
        		final String extensionName = tokens[1].replace("Release", "").replace("-", "").concat("edition_");
        		prospectiveVersion = extensionName.toLowerCase() + releaseDate;
        		prevReleaseVersion = previousExtVersion;
        		if (prevIntReleaseVersion != null) {
        			//previous extension release is not merged
            		prevReleaseVersion = extensionName.toLowerCase() + previousExtVersion;
//            		combineKnownVersions(prevReleaseVersion, prevIntReleaseVersion, previousExtVersion);
            		final boolean isSuccess = releaseDataManager.combineKnownVersions(prevReleaseVersion, prevIntReleaseVersion, previousExtVersion);
            		if (!isSuccess) {
            			responseMap.put(FAILURE_MESSAGE, "Failed to combine knwon versions:" 
            					+ prevIntReleaseVersion + " and"+ previousExtVersion + "into" + prevReleaseVersion);
            			return new ResponseEntity<>(responseMap, HttpStatus.OK);
            		}
        		}
        	} 
        	uploadProspectiveVersion(prospectiveVersion, extensionBaseLine, tempFile);
        	runAssertionTests(prospectiveVersion, prevReleaseVersion,runId,groupsList,responseMap);
        	final long timeTaken = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())/60000;
        	LOGGER.info(String.format("Finished execution with runId : [%1s] in [%2s] minutes ", runId, timeTaken));
        	return new ResponseEntity<>(responseMap, HttpStatus.OK);
        }
	}

	private void combineKnownVersions(final String combinedVersion, final String firstKnown, final String secondKnown) {
		LOGGER.info("Start combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
		final File firstZipFile = releaseDataManager.getZipFileForKnownRelease(firstKnown);
		final File secondZipFile = releaseDataManager.getZipFileForKnownRelease(secondKnown);
		releaseDataManager.loadSnomedData(combinedVersion, firstZipFile , secondZipFile);
		LOGGER.info("Complete combining two known versions {}, {} into {}", firstKnown, secondKnown, combinedVersion);
	}

	private boolean checkKnownVersion(final String prevIntReleaseVersion, final String previousExtVersion, 
			final String extensionBaseLine, final Map<String, Object> responseMap) {
		
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

	private void uploadProspectiveVersion(final String prospectiveVersion, final String knownVersion, final File tempFile) {
		
		if (knownVersion != null) {
			//load them together here as opposed to clone the existing DB so that to make sure it is clean.
			String versionDate = knownVersion;
			if (knownVersion.length() > 8) {
				versionDate = knownVersion.substring(knownVersion.length() -8);
			}
			final File preLoadedZipFile = releaseDataManager.getZipFileForKnownRelease(versionDate);
			if (preLoadedZipFile != null) {
				LOGGER.info("Start loading release version {} with release file {} and baseline {}", 
						prospectiveVersion, tempFile.getName(), preLoadedZipFile.getName());
				releaseDataManager.loadSnomedData(prospectiveVersion, tempFile, preLoadedZipFile);
			}
			LOGGER.error("Can't find the cached release zip file for known version:" + versionDate);
		}
		else {
			LOGGER.info("Start loading release version{} with release file {}", prospectiveVersion, tempFile.getName());
			releaseDataManager.loadSnomedData(prospectiveVersion, tempFile);
		}
		LOGGER.info("Completed loading release version{}", prospectiveVersion);
	}

	private boolean isKnownVersion(final String vertionToCheck, final Map<String, Object> responseMap) {
		if(!releaseDataManager.isKnownRelease(vertionToCheck)){
			// the previous published release must already be present in database, otherwise we throw an error!
			responseMap.put("type", "post");
			final String errorMsg = "Please load published release data in RVF first for version: " + vertionToCheck;
			responseMap.put(FAILURE_MESSAGE, errorMsg);
			LOGGER.info(errorMsg);
			return false;
		}
		return true;
	}

	private void runAssertionTests(final String prospectiveReleaseVersion, final String previousReleaseVersion, final long runId, final List<String> groupNames,
			final Map<String, Object> responseMap) throws IOException {
	      final List<AssertionGroup> groups = getAssertionGroupsByNames(groupNames);
		//execute common resources for assertions before executing group in the future we should run tests concurrently
		final List<Assertion> resourceAssertions = assertionService.getResourceAssertions();
		LOGGER.info("Found total resource assertions need to be run before test: " + resourceAssertions.size());
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

	
	
	private boolean verifyZipFileStructure(final Map<String, Object> responseMap, final File tempFile, final Long runId, final MultipartFile manifestFile, 
			final boolean writeSucceses, final String urlPrefix ) throws IOException {
		 boolean isFailed = false;
		// convert groups which is passed as string to assertion groups
        // set up the response in order to stream directly to the response
        final File manifestTestReport = new File(validationRunner.getReportDataFolder(), "manifest_validation_"+runId+".txt");
        try (PrintWriter writer = new PrintWriter(manifestTestReport)) {
        	final ResourceManager resourceManager = new ZipFileResourceProvider(tempFile);

            TestReportable report;

            if (manifestFile == null) {
            	report = validationRunner.execute(resourceManager, writer, writeSucceses);
            } else {
            	final String originalFilename = manifestFile.getOriginalFilename();
            	final File tempManifestFile = File.createTempFile(originalFilename, ".xml");
            	tempManifestFile.deleteOnExit();
            	copyUploadToDisk(manifestFile, tempManifestFile);

            	final ManifestFile mf = new ManifestFile(tempManifestFile);
            	report = validationRunner.execute(resourceManager, writer, writeSucceses, mf);
            }

            // verify if manifest is valid
            if(report.getNumErrors() > 0){

            	LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
            	responseMap.put("type", "pre");
            	responseMap.put("assertionsRun", report.getNumTestRuns());
            	responseMap.put("assertionsFailed", report.getNumErrors());
            	LOGGER.info("reportPhysicalUrl : " + manifestTestReport.getAbsolutePath());
            	// pass file name without extension - we add this back when we retrieve using controller
            	responseMap.put("reportUrl", urlPrefix+"/reports/"+ FilenameUtils.removeExtension(manifestTestReport.getName()));

            	LOGGER.info("report.getNumErrors() = " + report.getNumErrors());
            	LOGGER.info("report.getNumTestRuns() = " + report.getNumTestRuns());
            	final double threshold = report.getNumErrors() / report.getNumTestRuns();
            	LOGGER.info("threshold = " + threshold);
            	// bail out only if number of test failures exceeds threshold
            	if(threshold > validationRunner.getFailureThreshold()){
            		isFailed = true;
            	}
            }
        }
        
        return isFailed;
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
                LOGGER.info(String.format("Started executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
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
                LOGGER.info(String.format("Finished executing assertion [%1s] of [%2s] with uuid : [%3s]", counter, assertions.size(), assertion.getUuid()));
                counter++;
            }
			catch (final MissingEntityException e) {
				failedAssertionCount++;
			}
		}
		return failedAssertionCount;
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
		tempFile.deleteOnExit();
		if (!filename.endsWith(".txt")) {
			throw new IllegalArgumentException("Pre condition file should always be a .txt file");
		}
		response.setContentType("text/csv;charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
		
		try (PrintWriter writer = response.getWriter()) {
			copyUploadToDisk(file, tempFile);
			final ResourceManager resourceManager = new TextFileResourceProvider(tempFile, filename);
			final TestReportable report = validationRunner.execute(resourceManager, writer, writeSucceses);
			// store the report to disk for now with a timestamp
			if (report.getNumErrors() > 0) {
				LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
			}
		}
		return null;
	}

	@RequestMapping(value = "/reports/{id}", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Returns a report",
		notes = "Returns a report as txt file for a valid report id " )
	public FileSystemResource getFile(@PathVariable final String id) {
		return new FileSystemResource(new File(validationRunner.getReportDataFolder(), id+".txt"));
	}

	private void copyUploadToDisk(final MultipartFile file, final File tempFile) throws IOException {
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			try (InputStream inputStream = file.getInputStream()){
				IOUtils.copy(inputStream, out);
			}
		}
	}
	
	
	private List<AssertionGroup> getAssertionGroupsByNames( final List<String> groupNames) {
		final List<AssertionGroup> result = new ArrayList<>();
		for (final String groupName : groupNames) {
			final AssertionGroup group = assertionService.getAssertionGroupByName(groupName);
			if (group != null) {
				result.add(group);
			}
		}
		return result;
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
