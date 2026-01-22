package org.ihtsdo.rvf.importer;

import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.service.config.AssertionUuidConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.ihtsdo.rvf.importer.AssertionGroupImporter.AssertionGroupName.*;

@Service
@Transactional
public class AssertionGroupImporter {

	private static final String PREVIOUS = "previous";
	private static final String NEW_INACTIVE_STATES_FOLLOW_ACTIVE_STATES = "New inactive states follow active states";
	public static final String MDRS = "mdrs";

	enum AssertionGroupName {
		FILE_CENTRIC_VALIDATION ("COMMON", "file-centric-validation"),
		COMPONENT_CENTRIC_VALIDATION ("COMMON", "component-centric-validation"),
		RELEASE_TYPE_VALIDATION ("COMMON", "release-type-validation"),
		MDRS_VALIDATION ("mdrs", "mdrs"),
		SPANISH_EDITION ("ES", "SpanishEdition"),
		INTERNATIONAL_EDITION ("INT", "InternationalEdition"),
		COMMON_AUTHORING ("COMMON", "common-authoring"),
		COMMON_AUTHORING_WITHOUT_LANG_REFSETS ("COMMON_AUTHORING_WITHOUT_LANG_REFSETS", "common-authoring-without-lang-refsets"),
		COMMON_EDITION("COMMON", "common-edition"),
		INT_AUTHORING ("INT", "int-authoring"),
		AT_AUTHORING("AT", "at-authoring"),
		AU_AUTHORING("AU", "au-authoring"),
		DK_AUTHORING("DK", "dk-authoring"),
		SE_AUTHORING ("SE", "se-authoring"),
		US_AUTHORING ("US", "us-authoring"),
		BE_AUTHORING("BE", "be-authoring"),
		NO_AUTHORING("NO", "no-authoring"),
		CH_AUTHORING("CH", "ch-authoring"),
		FR_AUTHORING("FR", "fr-authoring"),
		IE_AUTHORING("IE", "ie-authoring"),
		EE_AUTHORING("EE", "ee-authoring"),
		NZ_AUTHORING("NZ", "nz-authoring"),
		ZH_AUTHORING("ZH", "zh-authoring"),
        KR_AUTHORING("KR", "kr-authoring"),
		NL_AUTHORING("NL", "nl-authoring"),
		STANDALONE_RELEASE("STANDALONE_RELEASE", "standalone-release"),
		LOINC_AUTHORING("LOINC", "loinc-authoring"),
		SIMPLEX_RELEASE("SIMPLEX", "simplex-release"),
		COMMON_REFSET ("COMMON", "common-refset"),
		FIRST_TIME_LOINC_VALIDATION ("LOINC", "first-time-loinc-validation"),
		FIRST_TIME_COMMON_EDITION_VALIDATION ("COMMON", "first-time-common-edition"),
		LOINC_EDITION ("LOINC", "LoincEdition"),
		DANISH_EDITION("DK", "DanishEdition"),
		SWEDISH_EDITION("SE", "SwedishEdition"),
		US_EDITION("US", "USEdition"),
		BE_EDITION("BE", "BelgianEdition"),
		NO_EDITION("NO", "NorwegianEdition"),
		CH_EDITION("CH", "SwissEdition"),
		FR_EDITION("FR", "FrenchEdition"),
		IE_EDITION("IE", "IrishEdition"),
		EE_EDITION("EE", "EstonianEdition"),
		AT_EDITION("AT", "AustrianEdition"),
		AU_EDITION("AU", "AustralianEdition"),
		NL_EDITION("NL", "DutchEdition"),
		GPFP_ICPC2("GPFP-ICPC2","GPFP-ICPC2"),
		GMDN("GMDN","GMDN"),
		STATED_RELATIONSHIPS_VALIDATION("STATED_RELATIONSHIPS","stated-relationships-validation"), //Assertions group that contains only stated relationship for file centric and component centric assertions
		STATED_RELATIONSHIPS_RELEASE_VALIDATION("STATED_RELATIONSHIPS_RELEASE_TYPE","stated-relationships-release-validation"), //Assertion group that contains full list of stated relationship assertions
		DERIVATIVE_EDITION("DERIVATIVE","DerivativeEdition");


		private final String name;
		private final String releaseCenter;
		AssertionGroupName(String releaseCenter, String name) {
			this.releaseCenter = releaseCenter;
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
		public String getReleaseCenter() {
			return this.releaseCenter;
		}

		public static AssertionGroupName fromName(String name) {
			if (name == null) {
				throw new IllegalArgumentException("'name' cannot be null.");
			}

			return Arrays.stream(AssertionGroupName.values()).filter(type -> type.getName().equals(name)).findFirst().orElse(null);
		}

	}

	public enum ProductName {
		// TODO FRI-246
		// add the new edition to FILENAME_PATTERN_TO_EDITION_MAP as well in ReleaseDataManager.
			INT("INT", "900000000000207008"),
			AU("AU", "32506021000036107"),
			BE("BE", "11000172109"),
			NL("NL", "11000146104"),
			UK("UK", "999000041000000102"),
			UKCL("UKCL", "999000011000000103"),
			US("US", "731000124108"),
			NZ("NZ", "21000210109"),
			ES("ES", "450829007"),

			DK("DK", "554471000005108"),
			SE("SE", "45991000052106"),
			NO("NO", "51000202101"),
			CH("CH", "2011000195101"),
			IE("IE", "11000220105"),
			EE("EE", "11000181102"),
			AT("AT", "11000234105"),
			TM("TM", "895344001"),
			SV("SNOVET", "332351000009108");

			private final String name;
			private final String moduleId;
			ProductName(String name, String moduleId) {
					this.name = name;
					this.moduleId = moduleId;
			}

			public String getName() {
					return this.name;
			}
			public String getModuleId() {
					return this.moduleId;
			}
			static public String toModuleId(String name) {
				for (ProductName pn: ProductName.values()) {
					if (name.equalsIgnoreCase(pn.getName())) {
						return pn.getModuleId();
					}
				}
				return name;
			}
	}

	private static final String SIMPLE_MAP = "simple map";

	private final AssertionService assertionService;

	private final AssertionUuidConfig assertionUuidConfig;

	@Autowired
	public AssertionGroupImporter(AssertionService assertionService, AssertionUuidConfig assertionUuidConfig) {
		this.assertionService = assertionService;
		this.assertionUuidConfig = assertionUuidConfig;
	}

/* the following were included but feel that they should be validated for project level as well.
	"6b34ab30-79b9-11e1-b0c4-0800200c9a66",
	"72184790-79b9-11e1-b0c4-0800200c9a66",
	"77fc7550-79b9-11e1-b0c4-0800200c9a66",
	"32b41aa0-7d08-11e1-b0c4-0800200c9a66",
*/


	private static final Logger LOGGER = LoggerFactory.getLogger(AssertionGroupImporter.class);

	public boolean isImportRequired() {
		List<AssertionGroup> allGroups = assertionService.getAllAssertionGroups();
		return allGroups == null || allGroups.isEmpty();
	}

	/*
	 * Create assertion groups
	 *
	 * file-centric-validation
	 * release-type-validation
	 * component-centric-validation
	 * SpanishEdition
	 * InternationalEdition
	 * SnapshotContentValidation
	 */

	public void importAssertionGroups() {
		List<AssertionGroup> allGroups = assertionService.getAllAssertionGroups();
		List<String> existingGroups = new ArrayList<>();
		// remove all assertions from existing group
		if (allGroups != null) {
			for (AssertionGroup group : allGroups) {
				LOGGER.info("Validation group is already created: {}", group.getName());

				group.removeAllAssertionsFromGroup();
				existingGroups.add(group.getName());
			}
		}

		// create new assertion group if any
		List<String> allGroupNames = new ArrayList<>();
		for (AssertionGroupName groupName : AssertionGroupName.values()) {
			allGroupNames.add(groupName.getName());
			if (!existingGroups.contains(groupName.getName())) {
				LOGGER.info("creating assertion group: {}", groupName.getName());
				AssertionGroup group = new AssertionGroup();
				group.setName(groupName.getName());
				allGroups.add(assertionService.createAssertionGroup(group));
			}
		}

		// add assertions to assertion group
		List<Assertion> allAssertions = assertionService.findAll();
		allGroups = allGroups.stream().filter(assertionGroup -> allGroupNames.contains(assertionGroup.getName())).collect(Collectors.toList());
		for (AssertionGroup group : allGroups) {
			addAssertionsToAssertionGroup(group, allAssertions);
		}
	}

	private void addAssertionsToAssertionGroup(AssertionGroup assertionGroup, List<Assertion> allAssertions) {
		AssertionGroupName groupName = AssertionGroupName.fromName(assertionGroup.getName());
		if (groupName != null) {
			switch (groupName) {
				case COMMON_REFSET ->
						addAssertionToCommonRefset(allAssertions, assertionGroup);
				case FILE_CENTRIC_VALIDATION, RELEASE_TYPE_VALIDATION, COMPONENT_CENTRIC_VALIDATION ->
						addAssertionsByKeyWord(allAssertions, assertionGroup);
				case MDRS_VALIDATION, STANDALONE_RELEASE ->
						addAllAssertions(getReleaseAssertionsByCenter(allAssertions, groupName.getReleaseCenter()), assertionGroup);
				case SIMPLEX_RELEASE, LOINC_EDITION, SPANISH_EDITION, DANISH_EDITION, SWEDISH_EDITION, INTERNATIONAL_EDITION, US_EDITION, BE_EDITION, COMMON_EDITION, NO_EDITION, CH_EDITION, FR_EDITION, IE_EDITION, GMDN, EE_EDITION, AT_EDITION, AU_EDITION, NL_EDITION, GPFP_ICPC2, DERIVATIVE_EDITION ->
						addAssertionsToReleaseAssertionGroup(allAssertions, assertionGroup);
				case COMMON_AUTHORING -> addAssertionToCommonSnapshotAssertionGroup(allAssertions, assertionGroup);
				case COMMON_AUTHORING_WITHOUT_LANG_REFSETS ->
						addAssertionToCommonSnapshotWithoutLangRefsetsAssertionGroup(allAssertions, assertionGroup);
				case INT_AUTHORING, AT_AUTHORING, AU_AUTHORING, DK_AUTHORING, SE_AUTHORING, US_AUTHORING, BE_AUTHORING, NO_AUTHORING, CH_AUTHORING, FR_AUTHORING, IE_AUTHORING, NZ_AUTHORING, ZH_AUTHORING, EE_AUTHORING, KR_AUTHORING, NL_AUTHORING ->
						addAssertionToSnapshotAssertionGroup(assertionGroup);
				case FIRST_TIME_LOINC_VALIDATION, FIRST_TIME_COMMON_EDITION_VALIDATION ->
						addAssertionsToFirstTimeReleaseGroup(allAssertions, assertionGroup);
				case STATED_RELATIONSHIPS_VALIDATION ->
						addAssertionsToStatedRelationshipAssertionGroup(allAssertions, assertionGroup, false);
				case STATED_RELATIONSHIPS_RELEASE_VALIDATION ->
						addAssertionsToStatedRelationshipAssertionGroup(allAssertions, assertionGroup, true);
				default -> LOGGER.warn("unrecognized group: {}", assertionGroup.getName());
			}
		}
	}

	private void addAssertionToCommonRefset(List<Assertion> allAssertions, AssertionGroup group) {
		for (Assertion assertion : allAssertions) {
			if (assertionUuidConfig.getCommonRefsets().contains(assertion.getUuid().toString())) {
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
	}

	private void addAssertionsToFirstTimeReleaseGroup(List<Assertion> allAssertions, AssertionGroup group) {
		AssertionGroupName groupName = AssertionGroupName.fromName(group.getName());
		int counter = 0;
		String keyWords;
		for (Assertion assertion : allAssertions) {
			if (assertionUuidConfig.getFirstTimeCommonAdditionalExclude().contains(assertion.getUuid().toString())) {
				continue;
			}
			keyWords = assertion.getKeywords();
			//exclude SNOMED RT assertions
			if (assertionUuidConfig.getSnomedRtIdentifier().contains(assertion.getUuid().toString())) {
				continue;
			}
			// Exclude stated relationship assertions
			if (assertionUuidConfig.getStatedRelationship().contains(assertion.getUuid().toString())) {
				continue;
			}

			if (RELEASE_TYPE_VALIDATION.getName().equals(keyWords) || FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)
					|| keyWords.contains("," + groupName.getReleaseCenter())) {
				String assertionText = assertion.getAssertionText();
				if ( !assertionText.contains(PREVIOUS) && !assertionText.contains(NEW_INACTIVE_STATES_FOLLOW_ACTIVE_STATES) ) {
					assertionService.addAssertionToGroup(assertion, group);
					counter++;
				}
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void addAssertionToCommonSnapshotAssertionGroup(List<Assertion> allAssertions, AssertionGroup group) {
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			 if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				 continue;
			 }
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (assertionUuidConfig.getSnapshotExclude().contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getAssertionText().contains(SIMPLE_MAP)) {
					continue;
				}
				// Exclude stated relationship assertions
				if (assertionUuidConfig.getStatedRelationship().contains(assertion.getUuid().toString())) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void addAssertionToCommonSnapshotWithoutLangRefsetsAssertionGroup(List<Assertion> allAssertions, AssertionGroup group) {
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				continue;
			}
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (assertionUuidConfig.getSnapshotExclude().contains(assertion.getUuid().toString())
					|| assertion.getAssertionText().contains(SIMPLE_MAP)
					|| assertionUuidConfig.getStatedRelationship().contains(assertion.getUuid().toString())
					|| assertionUuidConfig.getCommonLanguageRefsets().contains(assertion.getUuid().toString())
				) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}


	private void addAssertionToSnapshotAssertionGroup(AssertionGroup group) {
		AssertionGroupName groupName = AssertionGroupName.fromName(group.getName());
		List<Assertion> allAssertions = assertionService.getAssertionsByKeyWords("," + groupName.getReleaseCenter(), false);
		List<Assertion> releaseTypeAssertions = assertionService.getAssertionsByKeyWords(RELEASE_TYPE_VALIDATION.getName(), false);
		if (INT_AUTHORING.equals(groupName)) {
            allAssertions.addAll(releaseTypeAssertions.stream().filter(assertion -> assertionUuidConfig.getIntAuthoringInclude().contains(assertion.getUuid().toString())).toList());
		} else {
			if (!US_AUTHORING.equals(groupName) && !NL_AUTHORING.equals(groupName) && !AU_AUTHORING.equals(groupName)) {
				allAssertions.addAll(assertionService.getAssertionsByKeyWords(",EXTENSION", false));
			}
			allAssertions.addAll(releaseTypeAssertions.stream().filter(assertion -> assertionUuidConfig.getMsAuthoringInclude().contains(assertion.getUuid().toString())).toList());
        }
		for (Assertion assertion : allAssertions) {
			if (isExcludeAssertionFromSnapshotAssertionGroup(assertion, groupName)) continue;
			assertionService.addAssertionToGroup(assertion, group);
		}
		LOGGER.info("Total assertions added {} for assertion group {}", allAssertions.size(), group.getName() );
	}

	private boolean isExcludeAssertionFromSnapshotAssertionGroup(Assertion assertion, AssertionGroupName groupName) {
		boolean isUSAuthoringExcludedAssertion = (AssertionGroupName.US_AUTHORING.equals(groupName) && assertionUuidConfig.getUsAuthoringExclude().contains(assertion.getUuid().toString()));
		boolean isAuAuthoringExcludedAssertion = (AssertionGroupName.AU_AUTHORING.equals(groupName) && assertionUuidConfig.getAuAuthoringExclude().contains(assertion.getUuid().toString()));
		boolean isINTAuthoringExcludedAssertion = (AssertionGroupName.INT_AUTHORING.equals(groupName) && assertionUuidConfig.getIntAuthoringExclude().contains(assertion.getUuid().toString()));
		boolean isSnapshotExcludedAssertion = (assertionUuidConfig.getSnapshotExclude().contains(assertion.getUuid().toString()) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getMsAuthoringInclude().contains(assertion.getUuid().toString()))
				|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getIntAuthoringInclude().contains(assertion.getUuid().toString()))));
		boolean isSimpleMapExcludedAssertion = (assertion.getAssertionText().contains(SIMPLE_MAP) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getMsAuthoringInclude().contains(assertion.getUuid().toString()))
				|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getIntAuthoringInclude().contains(assertion.getUuid().toString()))));
		boolean isStatedRelationshipExcludedAssertion = (assertionUuidConfig.getStatedRelationship().contains(assertion.getUuid().toString()) && !assertionUuidConfig.getMsAuthoringInclude().contains(assertion.getUuid().toString()));
		boolean isReleaseTypeExcludedAssertion = (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName()) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getMsAuthoringInclude().contains(assertion.getUuid().toString()))
				|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !assertionUuidConfig.getIntAuthoringInclude().contains(assertion.getUuid().toString()))));

		return isReleaseTypeExcludedAssertion || isAuAuthoringExcludedAssertion || isStatedRelationshipExcludedAssertion || isSimpleMapExcludedAssertion || isUSAuthoringExcludedAssertion || isINTAuthoringExcludedAssertion || isSnapshotExcludedAssertion;
	}


	private List<Assertion> getCommonReleaseAssertions(List<Assertion> allAssertions) {
		List<Assertion> result = new ArrayList<>();
		String keywords;
		for (Assertion assertion : allAssertions) {
			 keywords = assertion.getKeywords();
			if (keywords.contains(MDRS) || FILE_CENTRIC_VALIDATION.getName().equals(keywords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keywords)
					|| RELEASE_TYPE_VALIDATION.getName().equals(keywords)) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total common release assertions: {}", result.size());
		return result;
	}

	private List<Assertion> getReleaseAssertionsByCenter(List<Assertion> allAssertions, String releaseCenter) {
		List<Assertion> result = new ArrayList<>();
		for (Assertion assertion : allAssertions) {
			String keywords = assertion.getKeywords();
			if (keywords.endsWith("," + releaseCenter) || keywords.contains("," + releaseCenter + ",")) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total release assertions found :{} for center:{}", result.size(), releaseCenter);
		return result;
	}

	private void addAssertionsToReleaseAssertionGroup(List<Assertion> allAssertions, AssertionGroup assertionGroup) {
		//create international assertion group
		AssertionGroupName groupName = AssertionGroupName.fromName(assertionGroup.getName());
		List<Assertion> assertionsToBeAdded = getCommonReleaseAssertions(allAssertions);
		assertionsToBeAdded.addAll(getReleaseAssertionsByCenter(allAssertions, groupName.getReleaseCenter()));
		boolean isEdition = AssertionGroupName.INTERNATIONAL_EDITION.equals(groupName) || AssertionGroupName.US_EDITION.equals(groupName)
							|| AssertionGroupName.AU_EDITION.equals(groupName) || AssertionGroupName.NL_EDITION.equals(groupName);
		if (!AssertionGroupName.COMMON_EDITION.equals(groupName) && !isEdition) {
			assertionsToBeAdded.addAll(getReleaseAssertionsByCenter(allAssertions, "EXTENSION"));
		}
		for (Assertion assertion : assertionsToBeAdded) {
			if ((AssertionGroupName.SPANISH_EDITION.equals(groupName) && assertionUuidConfig.getSpanishExtensionExclude().contains(assertion.getUuid().toString()))
				|| assertionUuidConfig.getStatedRelationship().contains(assertion.getUuid().toString())
				|| (AssertionGroupName.INTERNATIONAL_EDITION.equals(groupName) && (assertionUuidConfig.getSnomedRtIdentifier().contains(assertion.getUuid().toString())
					|| assertionUuidConfig.getIntEditionExclude().contains(assertion.getUuid().toString())))
				|| (AssertionGroupName.US_EDITION.equals(groupName) && assertionUuidConfig.getUsEditionExclude().contains(assertion.getUuid().toString()))
				|| (AssertionGroupName.DERIVATIVE_EDITION.equals(groupName) && assertionUuidConfig.getMrcmRefsets().contains(assertion.getUuid().toString()))) {
				continue;
			}
			assertionService.addAssertionToGroup(assertion, assertionGroup);
		}
		LOGGER.info("Total assertions added {} for assertion group {}", assertionsToBeAdded.size(), groupName.getName());
	}

	private void addAssertionsByKeyWord(List<Assertion> allAssertions, AssertionGroup group) {
		for (Assertion assertion : allAssertions) {
			if (assertion.getKeywords().equals(group.getName())) {
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
	}

	private void addAllAssertions(List<Assertion> allAssertions, AssertionGroup group) {
		int addedAssertions = 0;
		for (Assertion assertion : allAssertions) {
				assertionService.addAssertionToGroup(assertion, group);
				addedAssertions++;
		}
		LOGGER.info("Total assertions added {} for assertion group {}", addedAssertions, group.getName());
	}

	private void addAssertionsToStatedRelationshipAssertionGroup(List<Assertion> allAssertions, AssertionGroup group, boolean useFullList) {
		List<String> statedRelationshipAssertionIds = assertionUuidConfig.getStatedRelationship();
		int statedRelationshipAssertionCount = statedRelationshipAssertionIds.size();
		int count = 0;
		for (Assertion assertion : allAssertions) {
			if(statedRelationshipAssertionIds.contains(assertion.getUuid().toString())) {
				if(useFullList || !(assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName()))) {
					assertionService.addAssertionToGroup(assertion, group);
				}
				count++;
				if(count == statedRelationshipAssertionCount) break;
			}
		}
	}

}
