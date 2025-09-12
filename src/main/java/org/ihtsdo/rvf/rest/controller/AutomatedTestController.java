package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.rest.helper.ControllerHelper;
import org.ihtsdo.rvf.core.messaging.ValidationQueueManager;
import org.ihtsdo.rvf.core.service.AutomatedTestService;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.ValidationComparisonReport;
import org.ihtsdo.rvf.core.service.ValidationRunner;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.structure.validation.StructuralTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The controller that runs the automated regression test
 */
@RestController
@Tag(name = "Automated Test")
public class AutomatedTestController {

    private static final String ENABLE_MRCM_VALIDATION = "enableMRCMValidation";

    private static final String ENABLE_TRACEABILITY_VALIDATION = "enableTraceabilityValidation";

    private static final String INCLUDED_MODULES = "includedModules";

    private static final String RELEASE_AS_AN_EDITION = "releaseAsAnEdition";

    private static final String EFFECTIVE_TIME = "effectiveTime";

    private static final String ENABLE_DROOLS = "enableDrools";

    private static final String STORAGE_LOCATION = "storageLocation";

    private static final String FAILURE_EXPORT_MAX = "failureExportMax";

    private static final String RUN_ID = "runId";

    private static final String DEPENDENCY_RELEASE = "dependencyRelease";

    private static final String BRANCH_PATH = "branchPath";

    private static final String PREVIOUS_REPORT_URL = "previousReportUrl";

    private static final String PROSPECTIVE_REPORT_URL = "prospectiveReportUrl";

    private static final String PREVIOUS_RELEASE = "previousRelease";

    private static final String DROOLS_RULES_GROUPS = "droolsRulesGroups";

    private static final String GROUPS = "groups";

    private static final String MANIFEST = "manifest";

    private static final String RF2_DELTA_ONLY = "rf2DeltaOnly";

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatedTestController.class);

    @Autowired
    private StructuralTestRunner structureTestRunner;

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private ValidationQueueManager queueManager;

    @Autowired
    private AutomatedTestService automatedTestService;

    @RequestMapping(value = "/compare/{compareId}", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get comparison report.")
    public ResponseEntity <ValidationComparisonReport> getCompareReport(
            @Parameter(description = "Prospective report URL.") @PathVariable(value = "compareId") final String compareId) {
        ValidationComparisonReport report = automatedTestService.getCompareReport(compareId);
        if (report != null) {
            return new ResponseEntity<>(report, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/compare", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Compare 2 RVF reports for given URLs.")
    public ResponseEntity <Void> compareReports(
            @Parameter(description = "Prospective report URL.") @RequestParam(value = PROSPECTIVE_REPORT_URL, required = false) final String prospectiveReportUrl,
            @Parameter(description = "Previous report URL.") @RequestParam(value = PREVIOUS_REPORT_URL, required = false) final String previousReportUrl,
            UriComponentsBuilder uriComponentsBuilder) {
        String compareId = automatedTestService.compareReportGivenUrls(previousReportUrl, prospectiveReportUrl);
        return ControllerHelper.getCreatedResponse(compareId);
    }

    @RequestMapping(value = "/run-post-and-compare", method = RequestMethod.POST, consumes = "multipart/form-data")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Run validations for a RF2 release file package, then compare the results against the previous report")
    public ResponseEntity <Void> runPostTestPackageAndCompare(
            @Parameter(description = "RF2 release package in zip file") @RequestParam(value = "file") final MultipartFile file,
            @Parameter(description = "True if the test file contains RF2 delta files only. Defaults to false.") @RequestParam(value = RF2_DELTA_ONLY, required = false, defaultValue = "false") final boolean isRf2DeltaOnly,
            @Parameter(description = "manifest.xml file(optional)") @RequestParam(value = MANIFEST, required = false) final MultipartFile manifestFile,
            @Parameter(description = "Assertion group names separated by a comma.") @RequestParam(value = GROUPS) final List <String> groupsList,
            @Parameter(description = "Drools rules group names") @RequestParam(value = DROOLS_RULES_GROUPS, required = false) final List <String> droolsRulesGroupsList,
            @Parameter(description = "Required for non-first time international release testing") @RequestParam(value = PREVIOUS_RELEASE, required = false) final String previousRelease,
            @Parameter(description = "Defaults to 10 when not set") @RequestParam(value = FAILURE_EXPORT_MAX, required = false, defaultValue = "10") final Integer exportMax,
            @Parameter(description = "The sub folder for validation reports") @RequestParam(value = STORAGE_LOCATION) final String storageLocation,
            @Parameter(description = "Defaults to false") @RequestParam(value = ENABLE_DROOLS, required = false) final boolean enableDrools,
            @Parameter(description = "Effective time, optionally used in Drools validation, required if Jira creation flag is true") @RequestParam(value = EFFECTIVE_TIME, required = false) final String effectiveTime,
            @Parameter(description = "If release package file is an MS edition, should set to true. Defaults to false") @RequestParam(value = RELEASE_AS_AN_EDITION, required = false) final boolean releaseAsAnEdition,
            @Parameter(description = "Module IDs of components in the MS extension. Used for filtering results in Drools validation. Values are separated by comma")
            @RequestParam(value = INCLUDED_MODULES, required = false) final String includedModules,
            @Parameter(description = "Defaults to false.") @RequestParam(value = ENABLE_MRCM_VALIDATION, required = false) final boolean enableMrcmValidation,
            @Parameter(description = "Enable traceability validation.") @RequestParam(value = ENABLE_TRACEABILITY_VALIDATION, required = false, defaultValue = "false") final boolean enableTraceabilityValidation,
            @Parameter(description = "Terminology Server content branch path, used for traceability check.") @RequestParam(value = BRANCH_PATH, required = false) final String branchPath,
            @Parameter(description = "Previous report URL.") @RequestParam(value = PREVIOUS_REPORT_URL, required = false) final String previousReportUrl,
            UriComponentsBuilder uriComponentsBuilder
    ) throws IOException {

        ValidationRunConfig vrConfig = new ValidationRunConfig();
        String urlPrefix = URI.create(uriComponentsBuilder.toUriString()).toURL().toString();
        long runId = System.currentTimeMillis();

        vrConfig.addFile(file).addRF2DeltaOnly(isRf2DeltaOnly)
                .addGroupsList(groupsList)
                .addDroolsRulesGroupList(droolsRulesGroupsList)
                .addManifestFile(manifestFile)
                .addPreviousRelease(previousRelease)
                .addRunId(runId)
                .addStorageLocation(storageLocation)
                .addFailureExportMax(exportMax)
                .addProspectiveFilesInS3(false)
                .setEnableDrools(enableDrools)
                .setEffectiveTime(effectiveTime)
                .setReleaseAsAnEdition(releaseAsAnEdition)
                .setFirstTimeRelease(!StringUtils.hasLength(previousRelease))
                .setIncludedModules(includedModules)
                .addUrl(urlPrefix)
                .setEnableMRCMValidation(enableMrcmValidation)
                .setEnableTraceabilityValidation(enableTraceabilityValidation)
                .setBranchPath(branchPath);

        // Before we start running, ensure that we've made our mark in the storage location
        // Init will fail if we can't write the "running" state to storage
        final Map <String, String> responseMap = new HashMap <>();
        if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
            // Queue incoming validation request
            queueManager.queueValidationRequest(vrConfig, responseMap);
            URI uri = createResultURI(runId, storageLocation, uriComponentsBuilder);
            String compareId = automatedTestService.compareReportGivenUrls(previousReportUrl, uri.toString());
            return ControllerHelper.getCreatedResponse(compareId);
        }

        return new ResponseEntity <>(HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/run-post-via-s3-and-compare", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Run validations for release files stored in AWS S3, then compare the results against the previous report")
    public ResponseEntity <Void> runPostTestPackageViaS3AndCompare(
            @Parameter(description = "S3 bucket name") @RequestParam(value = "bucketName") String bucketName,
            @Parameter(description = "Release zip file path in S3") @RequestParam(value = "releaseFileS3Path") String releaseFileS3Path,
            @Parameter(description = "True if the test file contains RF2 delta files only. Defaults to false.") @RequestParam(value = RF2_DELTA_ONLY, required = false, defaultValue = "false") final boolean isRf2DeltaOnly,
            @Parameter(description = "manifest.xml file path in AWS S3") @RequestParam(value = "manifestFileS3Path", required = false) final String manifestFileS3Path,
            @Parameter(description = "Assertion group names") @RequestParam(value = GROUPS) final List <String> groupsList,
            @Parameter(description = "Drools rules group names") @RequestParam(value = DROOLS_RULES_GROUPS, required = false) final List <String> droolsRulesGroupsList,
            @Parameter(description = "Required for non-first time international release testing") @RequestParam(value = PREVIOUS_RELEASE, required = false) final String previousRelease,
            @Parameter(description = "Defaults to 10 when not set") @RequestParam(value = FAILURE_EXPORT_MAX, required = false, defaultValue = "10") final Integer exportMax,
            @Parameter(description = "The sub folder for validation reports") @RequestParam(value = STORAGE_LOCATION) final String storageLocation,
            @Parameter(description = "Defaults to false") @RequestParam(value = ENABLE_DROOLS, required = false) final boolean enableDrools,
            @Parameter(description = "Effective time, optionally used in Drools validation, required if Jira creation flag is true")
            @RequestParam(value = EFFECTIVE_TIME, required = false) final String effectiveTime,
            @Parameter(description = "If release package file is an MS edition, should set to true. Defaults to false")
            @RequestParam(value = RELEASE_AS_AN_EDITION, required = false) final boolean releaseAsAnEdition,
            @Parameter(description = "Module IDs of components in the MS extension. Used for filtering results in Drools validation. Values are separated by comma")
            @RequestParam(value = INCLUDED_MODULES, required = false) final String includedModules,
            @Parameter(description = "Defaults to false.") @RequestParam(value = ENABLE_MRCM_VALIDATION, required = false) final boolean enableMrcmValidation,
            @Parameter(description = "Enable traceability validation.") @RequestParam(value = ENABLE_TRACEABILITY_VALIDATION, required = false, defaultValue = "false") final boolean enableTraceabilityValidation,
            @Parameter(description = "Terminology Server content branch path, used for traceability validation.") @RequestParam(value = BRANCH_PATH, required = false) final String branchPath,
            @Parameter(description = "Previous report URL.") @RequestParam(value = PREVIOUS_REPORT_URL, required = false) final String previousReportUrl,
            UriComponentsBuilder uriComponentsBuilder) throws IOException {
        ValidationRunConfig vrConfig = new ValidationRunConfig();
        String urlPrefix = URI.create(uriComponentsBuilder.toUriString()).toURL().toString();
        long runId = System.currentTimeMillis();

        vrConfig.addBucketName(bucketName)
                .addProspectiveFileFullPath(releaseFileS3Path)
                .addRF2DeltaOnly(isRf2DeltaOnly)
                .addGroupsList(groupsList)
                .addDroolsRulesGroupList(droolsRulesGroupsList)
                .addManifestFileFullPath(manifestFileS3Path)
                .addPreviousRelease(previousRelease)
                .addRunId(runId)
                .addStorageLocation(storageLocation)
                .addFailureExportMax(exportMax)
                .addUrl(urlPrefix)
                .addProspectiveFilesInS3(true)
                .setFirstTimeRelease(!StringUtils.hasLength(previousRelease))
                .setEnableDrools(enableDrools)
                .setEffectiveTime(effectiveTime)
                .setReleaseAsAnEdition(releaseAsAnEdition)
                .setIncludedModules(includedModules)
                .setEnableMRCMValidation(enableMrcmValidation)
                .setEnableTraceabilityValidation(enableTraceabilityValidation)
                .setBranchPath(branchPath);

        // Before we start running, ensure that we've made our mark in the storage location
        // Init will fail if we can't write the "running" state to storage
        final Map <String, String> responseMap = new HashMap <>();
        if (isAssertionGroupsValid(vrConfig.getGroupsList(), responseMap)) {
            // Queue incoming validation request
            queueManager.queueValidationRequest(vrConfig, responseMap);
            URI uri = createResultURI(runId, storageLocation, uriComponentsBuilder);
            LOGGER.info("RVF result url: {}", uri.toURL());

            String compareId = automatedTestService.compareReportGivenUrls(previousReportUrl, uri.toString());
            return ControllerHelper.getCreatedResponse(compareId);
        }

        return new ResponseEntity <>(HttpStatus.BAD_REQUEST);
    }

    private boolean isAssertionGroupsValid(List <String> validationGroups,
                                           Map <String, String> responseMap) {
        if (ValidationRunner.EMPTY_TEST_ASSERTION_GROUPS.equals(validationGroups)) {
            return true;
        }

        List <AssertionGroup> groups = assertionService.getAssertionGroupsByNames(validationGroups);
        if (groups.size() != validationGroups.size()) {
            final List <String> found = new ArrayList <>();
            for (final AssertionGroup group : groups) {
                found.add(group.getName());
            }
            final String groupNotFoundMsg = String.format("Assertion groups requested: %s but found in RVF: %s", validationGroups, found);
            responseMap.put("failureMessage", groupNotFoundMsg);
            LOGGER.warn("Invalid assertion groups requested." + groupNotFoundMsg);
            return false;
        }
        return true;
    }

    private URI createResultURI(final Long runId, final String storageLocation, final UriComponentsBuilder uriComponentsBuilder) {
        return uriComponentsBuilder.path("/result/{run_id}").query("storageLocation={storage_location}")
                .buildAndExpand(runId, storageLocation).toUri();
    }
}
