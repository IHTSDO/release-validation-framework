package org.ihtsdo.rvf.execution.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.drools.domain.*;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.entity.ValidationReport;
import org.ihtsdo.rvf.execution.service.config.ValidationResourceConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.whitelist.WhitelistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DroolsRulesValidationService {

	private static final String COMMA = ",";

	@Value("${rvf.drools.rule.directory}")
	private String droolsRuleDirectoryPath;

	@Value("${cloud.aws.region.static}")
	private String awsRegion;

	@Value("${rvf.assertion.whitelist.batchsize:1000}")
	private int whitelistBatchSize;

	@Autowired
	private ValidationResourceConfig testResourceConfig;

	@Autowired
	private WhitelistService whitelistService;

	private ResourceManager testResourceManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(DroolsRulesValidationService.class);

	private static final String EXT_ZIP = ".zip";

	@PostConstruct
	public void init() {
		AmazonS3 anonymousClient = AmazonS3ClientBuilder.standard()
				.withRegion(awsRegion)
				.withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
				.build();
		testResourceManager = new ResourceManager(testResourceConfig, new SimpleStorageResourceLoader(anonymousClient));
	}

	public ValidationStatusReport runDroolsAssertions(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) throws RVFExecutionException {
		Set<String> extractedRF2FilesDirectories = new HashSet<>();
		Set<String> previousReleaseDirectories = new HashSet<>();
		try {
			long timeStart = new Date().getTime();
			String effectiveDate = StringUtils.isNotBlank(validationConfig.getEffectiveTime()) ? validationConfig.getEffectiveTime().replaceAll("-","") : "";
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
				Set<InputStream> snapshotsInputStream = new HashSet<>();
				InputStream testedReleaseFileStream = new FileInputStream(validationConfig.getLocalProspectiveFile());
				
				InputStream deltaInputStream = null;
				//If the validation is Delta validation, previous snapshot file must be loaded to snapshot files list.
				if (validationConfig.isRf2DeltaOnly()) {
					if(StringUtils.isBlank(validationConfig.getPreviousRelease()) || !validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
						throw new RVFExecutionException("Drools validation cannot execute when Previous Release is empty or not a .zip file: " + validationConfig.getPreviousRelease());
					}
					InputStream previousStream = new FileInputStream(validationConfig.getLocalPreviousReleaseFile());
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
					InputStream dependencyStream = new FileInputStream(validationConfig.getLocalDependencyReleaseFile());
					snapshotsInputStream.add(dependencyStream);

					//Will filter the results based on component's module IDs if the package is an extension only
					String moduleIds = validationConfig.getIncludedModules();
					if(StringUtils.isNotBlank(moduleIds)) {
						modulesSet = Sets.newHashSet(moduleIds.split(","));
					}
				}

				DroolsRF2Validator droolsRF2Validator = new DroolsRF2Validator(droolsRuleDirectoryPath, testResourceManager);

				//Unzip the release files
				for (InputStream inputStream : snapshotsInputStream) {
					String snapshotDirectoryPath = new ReleaseImporter().unzipRelease(inputStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
					extractedRF2FilesDirectories.add(snapshotDirectoryPath);
				}
				if(deltaInputStream != null) {
					extractedRF2FilesDirectories.add(new ReleaseImporter().unzipRelease(deltaInputStream, ReleaseImporter.ImportType.DELTA).getAbsolutePath());
				}

				if(StringUtils.isNotBlank(validationConfig.getPreviousRelease()) && validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
					InputStream previousReleaseStream = new FileInputStream(validationConfig.getLocalPreviousReleaseFile());
					previousReleaseDirectories.add(new ReleaseImporter().unzipRelease(previousReleaseStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath());
				}

				//Run validation
				invalidContents = droolsRF2Validator.validateRF2Files(extractedRF2FilesDirectories, CollectionUtils.isEmpty(previousReleaseDirectories) ? null : previousReleaseDirectories, droolsRulesSets, effectiveDate, modulesSet, true);

				if (invalidContents.size() != 0 && !whitelistService.isWhitelistDisabled()) {
					List<InvalidContent> newInvalidContents = new ArrayList<>();
					for (List<InvalidContent> batch : Iterables.partition(invalidContents, whitelistBatchSize)) {
						// Convert to WhitelistItem
						List<WhitelistItem> whitelistItems = batch.stream()
								.map(invalidContent -> new WhitelistItem(invalidContent.getRuleId(), org.springframework.util.StringUtils.isEmpty(invalidContent.getComponentId())? "" : invalidContent.getComponentId(), invalidContent.getConceptId(), getAdditionalFields(invalidContent.getComponent())))
								.collect(Collectors.toList());

						// Send to Authoring acceptance gateway
						LOGGER.info("Checking %s whitelist items in batch", whitelistItems.size());
						List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

						// Find the failures which are not in the whitelisted item
						newInvalidContents.addAll(batch.stream().filter(invalidContent ->
								whitelistedItems.stream().noneMatch(whitelistedItem ->
										invalidContent.getRuleId().equals(whitelistedItem.getValidationRuleId())
										&& invalidContent.getComponentId().equals(whitelistedItem.getComponentId()))
						).collect(Collectors.toList())) ;
					}
					invalidContents = newInvalidContents;
				}
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
				if (!invalidContentMap.containsKey(invalidContent.getRuleId())) {
					List<InvalidContent> invalidContentArrayList = new ArrayList<>();
					invalidContentArrayList.add(invalidContent);
					invalidContentMap.put(invalidContent.getRuleId(), invalidContentArrayList);
				} else {
					invalidContentMap.get(invalidContent.getRuleId()).add(invalidContent);
				}
			}
			invalidContents.clear();
			List<TestRunItem> failedAssertions = new ArrayList<>();
			List<TestRunItem> warningAssertions = new ArrayList<>();
			int failureExportMax = validationConfig.getFailureExportMax() != null ? validationConfig.getFailureExportMax() : 10;
			Map<String, List<InvalidContent>> groupRules = new HashMap<>();

			//Convert the Drools validation report into RVF report format
			for (String ruleId : invalidContentMap.keySet()) {
				TestRunItem validationRule = new TestRunItem();
				validationRule.setAssertionUuid(parseUUID(ruleId));
				validationRule.setTestType(TestType.DROOL_RULES);
				validationRule.setTestCategory("");
				List<InvalidContent> invalidContentList = invalidContentMap.get(ruleId);
				validationRule.setAssertionText(invalidContentList.get(0).getMessage().replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}","<UUID>").replaceAll("\\d{6,20}","<SCTID>"));

				validationRule.setFailureCount((long) invalidContentList.size());
				if (!whitelistService.isWhitelistDisabled()) {
					validationRule.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
							.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()).setComponentId(item.getComponentId()).setFullComponent(getAdditionalFields(item.getComponent())))
							.collect(Collectors.toList()));
				} else {
					validationRule.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
							.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()).setComponentId(item.getComponentId()))
							.collect(Collectors.toList()));
				}

				Severity severity = invalidContentList.get(0).getSeverity();
				if(Severity.WARNING.equals(severity)) {
					warningAssertions.add(validationRule);
				} else {
					failedAssertions.add(validationRule);
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
			for (String directoryPath : extractedRF2FilesDirectories) {
				FileUtils.deleteQuietly(new File(directoryPath));
			}
			for (String directoryPath : previousReleaseDirectories) {
				FileUtils.deleteQuietly(new File(directoryPath));
			}
		}
		return statusReport;
	}

	private UUID parseUUID(String uuid) {
		try{
			return UUID.fromString(uuid);
		} catch (IllegalArgumentException exception){
			return null;
		}
	}

	private String getAdditionalFields(Component component) {
		String additionalFields = (component.isActive() ? "1" : "0") + COMMA + component.getModuleId();
		if (component instanceof Concept) {
			return additionalFields + COMMA + ((Concept) component).getDefinitionStatusId();
		} else if (component instanceof Description) {
			Description description = (Description) component;
			return additionalFields + COMMA + description.getConceptId() + COMMA + description.getLanguageCode() + COMMA + description.getTypeId() + COMMA + description.getTerm() + COMMA + description.getCaseSignificanceId();
		} else if (component instanceof Relationship) {
			Relationship relationship = (Relationship) component;
			return additionalFields + COMMA + relationship.getSourceId() + COMMA + relationship.getDestinationId() + COMMA + relationship.getRelationshipGroup() + COMMA + relationship.getTypeId() + COMMA + relationship.getCharacteristicTypeId();
		} else if (component instanceof OntologyAxiom) {
			return additionalFields + COMMA + ((OntologyAxiom) component).getReferencedComponentId() + COMMA + ((OntologyAxiom) component).getOwlExpression();
		}
		return "";
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
