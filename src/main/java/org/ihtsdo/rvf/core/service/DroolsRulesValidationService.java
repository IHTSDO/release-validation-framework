package org.ihtsdo.rvf.core.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.drools.domain.*;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.ValidationResourceConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.ihtsdo.rvf.core.service.whitelist.WhitelistItem;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.SimpleStorageResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DroolsRulesValidationService {

	private static final String ERROR_COMPONENT_RULE_ID = "component-loading-error";

	private static final String WARNING_COMPONENT_RULE_ID = "component-loading-warning";

	private static final String COMMA = ",";

	@Value("${rvf.drools.rule.directory}")
	private String droolsRuleDirectoryPath;

	@Value("${rvf.drools.rule.repository.url}")
	private String droolsRuleRepositoryUrl;

	@Value("${rvf.assertion.whitelist.batchsize:1000}")
	private int whitelistBatchSize;

	@Autowired
	private ValidationResourceConfig testResourceConfig;

	@Autowired
	private WhitelistService whitelistService;

	private ResourceManager testResourceManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(DroolsRulesValidationService.class);

	private static final String EXT_ZIP = ".zip";

	private static final List<Assertion> assertions = new ArrayList<>();

	private static final Pattern rulePattern = Pattern.compile("\"(.*?)\"");
	private static final Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");

	@PostConstruct
	public void init() {
		S3Client s3Client = S3Client.builder().region(DefaultAwsRegionProviderChain.builder().build().getRegion())
				.credentialsProvider(AnonymousCredentialsProvider.create()).build();
		testResourceManager = new ResourceManager(testResourceConfig, new SimpleStorageResourceLoader(s3Client), s3Client);
		loadAndConvertDroolsRulesToAssertions();
	}

	private void loadAndConvertDroolsRulesToAssertions() {
		File droolsRuleDir = new File(droolsRuleDirectoryPath);
		if (!droolsRuleDir.isDirectory()) {
			LOGGER.error("Drools rules directory path {} is not a directory or inaccessible", droolsRuleDirectoryPath);
			return;
		}
		Collection<File> droolsRulesSubFiles = FileUtils.listFiles(droolsRuleDir, new String[] {"drl"}, true);
		for (File droolsRulesSubfile : droolsRulesSubFiles) {
			if (droolsRulesSubfile.isDirectory()) {
				continue;
			}
			try {
				convertDroolsRuleToAssertion(droolsRulesSubfile);
			} catch (IOException e) {
				LOGGER.error("Failed to read file {} ", droolsRulesSubfile.getAbsolutePath(), e);
			}
		}
		LOGGER.info("Loaded {} Drools rules assertions", assertions.size());
	}

	private void convertDroolsRuleToAssertion(File droolsRulesSubfile) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(droolsRulesSubfile.toPath())) {
			String line;
			UUID uuid;
			Assertion assertion = null;
			boolean severityWarning = false;
			boolean severityError = false;
			String group = getAssertionGroup(droolsRulesSubfile.getAbsolutePath());
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("rule") && !line.contains("Always passes")) {
					Matcher matcher = rulePattern.matcher(line);
					if (matcher.find()) {
						assertion = new Assertion();
						assertion.setAssertionText(matcher.group(1));
						assertion.setType(TestType.DROOL_RULES.name());
						assertion.setUrl(droolsRulesSubfile.getAbsolutePath().replace(droolsRuleDirectoryPath, droolsRuleRepositoryUrl));
						assertion.addGroup(group);
						assertion.setSeverity("ERROR");
						severityWarning = false;
						severityError = false;
						assertions.add(assertion);
					}
				}
				if (assertion != null && line.contains("new InvalidContent")) {
					uuid = extractUuid(line);
					assertion.setUuid(uuid);
				}
				// check for severity
				severityWarning = severityWarning || detectSeverity(line, "Severity.WARNING");
				severityError = severityError || detectSeverity(line, "Severity.ERROR");
				if (assertion != null && "end".equals(line.trim())) {
					assertion.setSeverity(buildSeverity(severityWarning, severityError));
				}
			}
		}
	}

	private static UUID extractUuid(String line) {
		Matcher matcher = uuidPattern.matcher(line);
		if (matcher.find()) {
			return UUID.fromString(matcher.group(1));
		}
		return null;
	}

	private static boolean detectSeverity(String line, String type) {
		return line.contains(type);
	}

	@NotNull
	private static String buildSeverity(boolean severityWarning, boolean severityError) {
		Set<String> severities = new HashSet<>();
		if (severityWarning) severities.add("WARNING");
		if (severityError) severities.add("ERROR");
		return String.join(",", severities);
	}


	private String getAssertionGroup(String absolutePath) {
		String relativePath = absolutePath.substring(droolsRuleDirectoryPath.length() + 1);
		return relativePath.substring(0, relativePath.indexOf("/"));
	}

	public List<Assertion> getAssertions() {
		return assertions;
	}

	public ValidationStatusReport runDroolsAssertions(ValidationRunConfig validationConfig, ValidationStatusReport statusReport) {
		try {
			long timeStart = new Date().getTime();
			String effectiveDate = StringUtils.isNotBlank(validationConfig.getEffectiveTime()) ? validationConfig.getEffectiveTime().replace("-","") : "";
			// Check if Drools rules assertion group is specified
			if (validationConfig.getDroolsRulesGroupList() == null || validationConfig.getDroolsRulesGroupList().isEmpty()) {
				throw new RVFExecutionException("No drools rules assertion group specified");
			}
			Set<String> droolsRulesSets = getDroolsRulesSetFromAssertionGroups(Sets.newHashSet(validationConfig.getDroolsRulesGroupList()));
			// Filter only Drools rules set from all the assertion groups
			if (droolsRulesSets.isEmpty()) {
				LOGGER.info("No drools rules found for assertion group {}", validationConfig.getDroolsRulesGroupList());
				statusReport.getReportSummary().put(TestType.DROOL_RULES.name(),"No drools rules found for assertion group " + validationConfig.getDroolsRulesGroupList());
				return statusReport;
			}
			// Run Drools rules validation to validate the RF2 files
			List<InvalidContent> invalidContents = validateFiles(validationConfig, effectiveDate, droolsRulesSets);

			// Construct the validation report
			ValidationReport validationReport = constructValidationReport(validationConfig, invalidContents);
			validationReport.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
			statusReport.setResultReport(validationReport);
		} catch (Exception ex) {
			String message = "Drools validation has stopped";
			LOGGER.error(message, ex);
			message = ex.getMessage() != null ? message + " due to error: " + ex.getMessage() : message;
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(TestType.DROOL_RULES.name(),message);
		}
		return statusReport;
	}

	private ValidationReport constructValidationReport(ValidationRunConfig validationConfig, List<InvalidContent> invalidContents) {
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
		Map<UUID, Assertion> uuidToAssertionMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, Function.identity()));

		// Convert the Drools validation report into RVF report format
		invalidContentMap.keySet().forEach(ruleId -> {
			TestRunItem validationRule = new TestRunItem();
			validationRule.setAssertionUuid(parseUUID(ruleId));
			validationRule.setTestType(TestType.DROOL_RULES);
			validationRule.setTestCategory("");
			List<InvalidContent> invalidContentList = invalidContentMap.get(ruleId);
			if (uuidToAssertionMap.containsKey(parseUUID(ruleId))) {
				validationRule.setAssertionText(uuidToAssertionMap.get(parseUUID(ruleId)).getAssertionText());
			} else {
				validationRule.setAssertionText(invalidContentList.get(0).getMessage().replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}","<UUID>").replaceAll("\\d{6,20}","<SCTID>"));
			}
			validationRule.setFailureCount((long) invalidContentList.size());
			if (!whitelistService.isWhitelistDisabled()) {
				validationRule.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
						.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()).setComponentId(item.getComponentId()).setFullComponent(getAdditionalFields(item.getComponent())))
						.toList());
			} else {
				validationRule.setFirstNInstances(invalidContentList.stream().limit(failureExportMax)
						.map(item -> new FailureDetail(item.getConceptId(), item.getMessage(), item.getConceptFsn()).setComponentId(item.getComponentId()))
						.toList());
			}

			Severity severity = invalidContentList.get(0).getSeverity();
			if(Severity.WARNING.equals(severity)) {
				warningAssertions.add(validationRule);
			} else {
				failedAssertions.add(validationRule);
			}
		});
		ValidationReport validationReport = new ValidationReport();
		validationReport.addFailedAssertions(failedAssertions);
		validationReport.addWarningAssertions(warningAssertions);
		return validationReport;
	}

	private List<InvalidContent> validateFiles(ValidationRunConfig validationConfig, String effectiveDate, Set<String> droolsRulesSets) throws RVFExecutionException, ReleaseImportException, IOException, RestClientException {
		List<InvalidContent> invalidContents;
		Set<String> extractedRF2FilesDirectories = new HashSet<>();
		Set<String> previousReleaseDirectories = new HashSet<>();
		try {
			// extract files locally
			extractFiles(validationConfig, extractedRF2FilesDirectories, previousReleaseDirectories);

			// Get the assertion exclusion list
			Set<String> assertionExclusionList = null;
			if (validationConfig.getAssertionExclusionList() != null) {
				assertionExclusionList = new HashSet<>(validationConfig.getAssertionExclusionList());
			}

			// Run validation
			DroolsRF2Validator droolsRF2Validator = new DroolsRF2Validator(droolsRuleDirectoryPath, testResourceManager);
			invalidContents = droolsRF2Validator.validateRF2Files(extractedRF2FilesDirectories, CollectionUtils.isEmpty(previousReleaseDirectories) ? null : previousReleaseDirectories, droolsRulesSets, assertionExclusionList, effectiveDate, null, true);

			// Filter the results based on component's module IDs if the package is an extension
			String moduleIdStr = validationConfig.getIncludedModules();
			if (StringUtils.isNotBlank(moduleIdStr)) {
				Set<String> modules = Sets.newHashSet(moduleIdStr.split(","));
				if (!modules.isEmpty()) {
					Map<UUID, Assertion> uuidAssertionMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, Function.identity()));
					invalidContents = invalidContents.stream().filter(item -> ERROR_COMPONENT_RULE_ID.equals(item.getRuleId())
							|| WARNING_COMPONENT_RULE_ID.equals(item.getRuleId())
							|| !uuidAssertionMap.get(UUID.fromString(item.getRuleId())).getGroups().contains("common-authoring")
							|| (uuidAssertionMap.get(UUID.fromString(item.getRuleId())).getGroups().contains("common-authoring")
							&& modules.contains(item.getComponent().getModuleId()))).toList();
				}
			}

			// Checking whether or not the failures are whitelisted
			if (!invalidContents.isEmpty() && !whitelistService.isWhitelistDisabled()) {
				invalidContents = checkAgainstWhiteListedItems(invalidContents);
			}
		} finally {
			extractedRF2FilesDirectories.forEach(directoryPath -> FileUtils.deleteQuietly(new File(directoryPath)));
			previousReleaseDirectories.forEach(directoryPath -> FileUtils.deleteQuietly(new File(directoryPath)));
		}
		return invalidContents;
	}

	@NotNull
	private List<InvalidContent> checkAgainstWhiteListedItems(List<InvalidContent> invalidContents) throws RestClientException {
		List<InvalidContent> newInvalidContents = new ArrayList<>();
		for (List<InvalidContent> batch : Iterables.partition(invalidContents, whitelistBatchSize)) {
			// Convert to WhitelistItem
			List<WhitelistItem> whitelistItems = batch.stream()
					.filter(invalidContent -> !ERROR_COMPONENT_RULE_ID.equals(invalidContent.getRuleId()) && !WARNING_COMPONENT_RULE_ID.equals(invalidContent.getRuleId()))
					.map(invalidContent -> new WhitelistItem(invalidContent.getRuleId(), StringUtils.isEmpty(invalidContent.getComponentId())? "" : invalidContent.getComponentId(), invalidContent.getConceptId(), getAdditionalFields(invalidContent.getComponent())))
					.toList();

			// Send to Authoring acceptance gateway
			LOGGER.info("Checking {} whitelist items in batch", whitelistItems.size());
			List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

			// Find the failures which are not in the whitelisted item
			newInvalidContents.addAll(batch.stream().filter(invalidContent ->
					whitelistedItems.stream().noneMatch(whitelistedItem -> invalidContent.getRuleId().equals(whitelistedItem.getValidationRuleId()) && invalidContent.getComponentId().equals(whitelistedItem.getComponentId()))
			).toList());
		}
		return newInvalidContents;
	}

	private void extractFiles(ValidationRunConfig validationConfig, Set<String> extractedRF2FilesDirectories, Set<String> previousReleaseDirectories) throws RVFExecutionException, ReleaseImportException, IOException {
		try (InputStream testedReleaseFileStream = new FileInputStream(validationConfig.getLocalProspectiveFile())) {
			// If the validation is Delta validation, previous snapshot file must be loaded to snapshot files list.
			if (validationConfig.isRf2DeltaOnly()) {
				if (StringUtils.isBlank(validationConfig.getPreviousRelease()) || !validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
					throw new RVFExecutionException("Drools validation cannot execute when Previous Release is empty or not a .zip file: " + validationConfig.getPreviousRelease());
				}
				extractedRF2FilesDirectories.add(new ReleaseImporter().unzipRelease(testedReleaseFileStream, ReleaseImporter.ImportType.DELTA).getAbsolutePath());
			} else {
				// If the validation is Snapshot validation, current file must be loaded to snapshot files list
				extractedRF2FilesDirectories.add(new ReleaseImporter().unzipRelease(testedReleaseFileStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath());
			}
		}

		// Load the dependency package from S3 to snapshot files list before validating if the package is an MS extension and not an edition release
		// If the package is an MS edition, it is not necessary to load the dependency
		if (validationConfig.getExtensionDependency() != null && !validationConfig.isReleaseAsAnEdition() && !validationConfig.isStandAloneProduct()) {
			if (StringUtils.isBlank(validationConfig.getExtensionDependency()) || !validationConfig.getExtensionDependency().endsWith(EXT_ZIP)) {
				throw new RVFExecutionException("Drools validation cannot execute when Extension Dependency is empty or not a .zip file: " + validationConfig.getExtensionDependency());
			}
			try (InputStream dependencyStream = new FileInputStream(validationConfig.getLocalDependencyReleaseFile())) {
				extractedRF2FilesDirectories.add(new ReleaseImporter().unzipRelease(dependencyStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath());
			}
		}

		if (StringUtils.isNotBlank(validationConfig.getPreviousRelease()) && validationConfig.getPreviousRelease().endsWith(EXT_ZIP)) {
			try (InputStream previousReleaseStream = new FileInputStream(validationConfig.getLocalPreviousReleaseFile())) {
				previousReleaseDirectories.add(new ReleaseImporter().unzipRelease(previousReleaseStream, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath());
			}
		}
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
		if (component instanceof Concept concept) {
			return additionalFields + COMMA + concept.getDefinitionStatusId();
		} else if (component instanceof Description description) {
			return additionalFields + COMMA + description.getConceptId() + COMMA + description.getLanguageCode() + COMMA + description.getTypeId() + COMMA + description.getTerm() + COMMA + description.getCaseSignificanceId();
		} else if (component instanceof Relationship relationship) {
			return additionalFields + COMMA + relationship.getSourceId() + COMMA + relationship.getDestinationId() + COMMA + relationship.getRelationshipGroup() + COMMA + relationship.getTypeId() + COMMA + relationship.getCharacteristicTypeId();
		} else if (component instanceof OntologyAxiom axiom) {
			return additionalFields + COMMA + axiom.getReferencedComponentId() + COMMA + ((OntologyAxiom) component).getOwlExpression();
		}
		return "";
	}


	private Set<String> getDroolsRulesSetFromAssertionGroups(Set<String> assertionGroups) throws RVFExecutionException {
		File droolsRuleDir = new File(droolsRuleDirectoryPath);
		if (!droolsRuleDir.isDirectory()) {
			throw new RVFExecutionException("Drools rules directory path " + droolsRuleDirectoryPath + " is not a directory or inaccessible");
		}
		Set<String> droolsRulesModules = new HashSet<>();
		File[] droolsRuleFiles = droolsRuleDir.listFiles();
		if (droolsRuleFiles != null) {
			for (File ruleFile : droolsRuleFiles) {
				if (ruleFile.isDirectory()) {
					droolsRulesModules.add(ruleFile.getName());
				}
			}
		}
		// Only keep the assertion groups with matching Drools Rule modules in the Drools Directory
		droolsRulesModules.retainAll(assertionGroups);
		return droolsRulesModules;
	}
}
