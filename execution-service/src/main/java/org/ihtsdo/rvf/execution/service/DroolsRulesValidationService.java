package org.ihtsdo.rvf.execution.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationResourceConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.Sets;

@Service
public class DroolsRulesValidationService {
	
	@Value("${rvf.drools.rule.directory}")
	private String droolsRuleDirectoryPath;

	@Value("${cloud.aws.region.static}")
	private String awsRegion;

	@Autowired
	private ValidationResourceConfig testResourceConfig;
	
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;

	@Autowired
	private ResourceLoader cloudResourceLoader;
	
	private ResourceManager releaseSourceManager;

	private ResourceManager testResourceManager;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DroolsRulesValidationService.class);

	private static final String EXT_ZIP = ".zip";

	@PostConstruct
	public void init() {
		releaseSourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);

		AmazonS3 anonymousClient = AmazonS3ClientBuilder.standard()
				.withRegion(awsRegion)
				.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
				.build();
		testResourceManager = new ResourceManager(testResourceConfig, new SimpleStorageResourceLoader(anonymousClient));
	}
	
	public ValidationStatusReport runDroolsAssertions(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws RVFExecutionException {
		Set<String> directoryPaths = new HashSet<>();
		try {
			long timeStart = new Date().getTime();
			//Filter only Drools rules set from all the assertion groups
			Set<String> droolsRulesSets = getDroolsRulesSetFromAssertionGroups(Sets.newHashSet(validationConfig.getDroolsRulesGroupList()));
			ValidationReport validationReport = statusReport.getResultReport();
			//Skip running Drools rules set altogether if there is no Drools rules set in the assertion groups
			if (droolsRulesSets.isEmpty()) {
				LOGGER.info("No drools rules found for assertion group " + validationConfig.getDroolsRulesGroupList());
				statusReport.getReportSummary().put(TestType.DROOL_RULES.name(),"No drools rules found for assertion group " + validationConfig.getDroolsRulesGroupList());
				return statusReport;
			}
			List<InvalidContent> invalidContents = null;
			try {
				ResourceManager validationJobResourceManager = new ResourceManager(jobResourceConfig, cloudResourceLoader);
				Set<InputStream> snapshotsInputStream = new HashSet<>();
				String prospectiveFileFullPath = validationConfig.getProspectiveFileFullPath();
				InputStream testedReleaseFileStream = null;
				
				if (jobResourceConfig.isUseCloud() && validationConfig.isProspectiveFileInS3()) {
					if (!jobResourceConfig.getCloud().getBucketName().equals(validationConfig.getBucketName())) {
						ManualResourceConfiguration manualConfig = new ManualResourceConfiguration(true, true, null,
								new ResourceConfiguration.Cloud(validationConfig.getBucketName(), ""));
						ResourceManager manualResource = new ResourceManager(manualConfig, cloudResourceLoader);
						testedReleaseFileStream = manualResource.readResourceStreamOrNullIfNotExists(prospectiveFileFullPath);
					} else {
						//update s3 path if required when full path containing job resource path already
						if (prospectiveFileFullPath.startsWith(jobResourceConfig.getCloud().getPath())) {
							prospectiveFileFullPath = prospectiveFileFullPath.replace(jobResourceConfig.getCloud().getPath(), "");
						}
					}
				}
				if(testedReleaseFileStream == null) {
					testedReleaseFileStream = validationJobResourceManager.readResourceStream(prospectiveFileFullPath);
				}
				
				InputStream deltaInputStream = null;
				//If the validation is Delta validation, previous snapshot file must be loaded to snapshot files list.
				if (validationConfig.isRf2DeltaOnly()) {
					if(StringUtils.isBlank(validationConfig.getPreviousRelease()) || !validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
						throw new RVFExecutionException("Drools validation cannot execute when Previous Release is empty or not a .zip file: " + validationConfig.getPreviousRelease());
					}
					InputStream previousStream = releaseSourceManager.readResourceStream(validationConfig.getPreviousRelease());
					snapshotsInputStream.add(previousStream);
					deltaInputStream = testedReleaseFileStream;
				} else {
					//If the validation is Snapshot validation, current file must be loaded to snapshot files list
					snapshotsInputStream.add(testedReleaseFileStream);
				}

				//Load the dependency package from S3 to snapshot files list before validating if the package is a MS extension and not an edition release
				//If the package is an MS edition, it is not necessary to load the dependency
				Set<String> modulesSet = null;
				if (validationConfig.getExtensionDependency() != null && !validationConfig.isReleaseAsAnEdition()) {
					if(StringUtils.isBlank(validationConfig.getExtensionDependency()) || !validationConfig.getExtensionDependency().endsWith(EXT_ZIP)) {
						throw new RVFExecutionException("Drools validation cannot execute when Extension Dependency is empty or not a .zip file: " + validationConfig.getExtensionDependency());
					}
					InputStream dependencyStream = releaseSourceManager.readResourceStream(validationConfig.getExtensionDependency());
					snapshotsInputStream.add(dependencyStream);

					//Will filter the results based on component's module IDs if the package is an extension only
					String moduleIds = validationConfig.getIncludedModules();
					if(StringUtils.isNotBlank(moduleIds)) {
						modulesSet = Sets.newHashSet(moduleIds.split(","));
					}
				}

				//Get effectiveTime
				DroolsRF2Validator droolsRF2Validator = new DroolsRF2Validator(droolsRuleDirectoryPath, testResourceManager);
				String effectiveTime = validationConfig.getEffectiveTime();
				if (StringUtils.isNotBlank(effectiveTime)) {
					effectiveTime = effectiveTime.replaceAll("-", "");
				} else {
					effectiveTime = "";
				}

				//Unzip the release files
				for (InputStream inputStream : snapshotsInputStream) {
					String snapshotDirectoryPath = new ReleaseImporter().unzipRelease(inputStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
					directoryPaths.add(snapshotDirectoryPath);
				}
				String deltaDirectoryPath = null;
				if(deltaInputStream != null) {
					deltaDirectoryPath = new ReleaseImporter().unzipRelease(deltaInputStream, ReleaseImporter.ImportType.DELTA).getAbsolutePath();
				}

				String prevReleasePath = null;
				if(StringUtils.isNotBlank(validationConfig.getPreviousRelease()) && validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
					InputStream previousReleaseStream = releaseSourceManager.readResourceStream(validationConfig.getPreviousRelease());
					prevReleasePath = new ReleaseImporter().unzipRelease(previousReleaseStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
				}

				//Run validation
				invalidContents = droolsRF2Validator.validateSnapshots(directoryPaths, deltaDirectoryPath, prevReleasePath, droolsRulesSets, effectiveTime, modulesSet, true);
			} catch (Exception e) {
				String message = "Drools validation has stopped";
				LOGGER.error(message, e);
				message = e.getMessage() != null ? message + " due to error: " + e.getMessage() : message;
				statusReport.addFailureMessage(message);
				statusReport.getReportSummary().put(TestType.DROOL_RULES.name(),message);
				return statusReport;
			}
			HashMap<String, List<InvalidContent>> invalidContentMap = new HashMap<>();
			for (InvalidContent invalidContent : invalidContents) {
				if (!invalidContentMap.containsKey(invalidContent.getMessage())) {
					List<InvalidContent> invalidContentArrayList = new ArrayList<>();
					invalidContentArrayList.add(invalidContent);
					invalidContentMap.put(invalidContent.getMessage(), invalidContentArrayList);
				} else {
					invalidContentMap.get(invalidContent.getMessage()).add(invalidContent);
				}
			}
			invalidContents.clear();
			List<TestRunItem> failedAssertions = new ArrayList<>();
			List<TestRunItem> warningAssertions = new ArrayList<>();
			int failureExportMax = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 10;
			Map<String, List<InvalidContent>> groupRules = new HashMap<>();

			//Convert the Drools validation report into RVF report format
			for (String rule : invalidContentMap.keySet()) {
				TestRunItem validationRule = new TestRunItem();
				validationRule.setTestType(TestType.DROOL_RULES);
				validationRule.setTestCategory("");
				//Some Drools validations message has SCTID, making it is impossible to group the same failures together unless the message is generalized by replacing the SCTID
				String groupedRuleName = rule.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}","<UUID>")
						.replaceAll("\\d{6,20}","<SCTID>");
				if(groupedRuleName.contains("<UUID>") || groupedRuleName.contains("<SCTID>")) {
					groupDroolsRules(groupRules, groupedRuleName, invalidContentMap.get(rule));
				} else {
					validationRule.setAssertionText(rule);
					List<InvalidContent> invalidContentList = invalidContentMap.get(rule);
					validationRule.setFailureCount((long) invalidContentList.size());
					validationRule.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
							.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()))
							.collect(Collectors.toList()));
					Severity severity = invalidContentList.get(0).getSeverity();
					if(Severity.WARNING.equals(severity)) {
						warningAssertions.add(validationRule);
					} else {
						failedAssertions.add(validationRule);
					}
				}
			}
			if(!groupRules.isEmpty()) {
				for (String rule : groupRules.keySet()) {
					TestRunItem testRunItem = new TestRunItem();
					testRunItem.setTestType(TestType.DROOL_RULES);
					testRunItem.setTestCategory("");
					testRunItem.setAssertionText(rule);
					List<InvalidContent> invalidContentList = groupRules.get(rule);
					testRunItem.setFailureCount((long)invalidContentList.size());
					testRunItem.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
							.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()))
							.collect(Collectors.toList()));
					Severity severity = invalidContentList.get(0).getSeverity();
					if(Severity.WARNING.equals(severity)) {
						warningAssertions.add(testRunItem);
					} else {
						failedAssertions.add(testRunItem);
					}
				}
			}
			validationReport.addFailedAssertions(failedAssertions);
			validationReport.addWarningAssertions(warningAssertions);
			validationReport.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
		} catch (Exception ex) {
			String message = "Drools validation has stopped";
			LOGGER.error(message, ex);
			message = ex.getMessage() != null ? message + " due to error: " + ex.getMessage() : message;
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(TestType.DROOL_RULES.name(),message);
		} finally {
			for (String directoryPath : directoryPaths) {
				FileUtils.deleteQuietly(new File(directoryPath));
			}
		}
		return statusReport;
	}
	

	private Set<String> getDroolsRulesSetFromAssertionGroups(Set<String> assertionGroups) throws RVFExecutionException {
		File droolsRuleDir = new File(droolsRuleDirectoryPath);
		if (!droolsRuleDir.isDirectory()) {
			throw new RVFExecutionException("Drools rules directory path " + droolsRuleDirectoryPath + " is not a directory or inaccessible");
		}
		Set<String> droolsRulesModules = new HashSet<>();
		File[] droolsRulesSubfiles = droolsRuleDir.listFiles();
		for (File droolsRulesSubfile : droolsRulesSubfiles) {
			if(droolsRulesSubfile.isDirectory()) droolsRulesModules.add(droolsRulesSubfile.getName());
		}
		//Only keep the assertion groups with matching Drools Rule modules in the Drools Directory
		droolsRulesModules.retainAll(assertionGroups);
		return droolsRulesModules;
	}

	private Map<String, List<InvalidContent>> groupDroolsRules(Map<String, List<InvalidContent>> groupedRules, 
			String rule, List<InvalidContent> invalidContents) {
		if(!groupedRules.containsKey(rule)) {
			groupedRules.put(rule, new ArrayList<>());
		}
		groupedRules.get(rule).addAll(invalidContents);
		return groupedRules;
	}
}
