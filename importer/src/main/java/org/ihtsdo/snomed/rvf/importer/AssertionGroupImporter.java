package org.ihtsdo.snomed.rvf.importer;

import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.COMPONENT_CENTRIC_VALIDATION;
import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.FILE_CENTRIC_VALIDATION;
import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.RELEASE_TYPE_VALIDATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ihtsdo.rvf.dao.AssertionGroupDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class AssertionGroupImporter {
	
	 private static final String PREVIOUS = "previous";
	private static final String NEW_INACTIVE_STATES_FOLLOW_ACTIVE_STATES = "New inactive states follow active states";

	enum AssertionGroupName {
		FILE_CENTRIC_VALIDATION ("COMMON", "file-centric-validation"),
		COMPONENT_CENTRIC_VALIDATION ("COMMON", "component-centric-validation"),
		RELEASE_TYPE_VALIDATION ("COMMON", "release-type-validation"),
		SPANISH_EDITION ("ES", "SpanishEdition"),
		INTERNATIONAL_EDITION ("INT", "InternationalEdition"),
		COMMON_AUTHORING ("COMMON", "common-authoring"),
		COMMON_EDITION("COMMON", "common-edition"),
		INT_AUTHORING ("INT", "int-authoring"),
		DK_AUTHORING("DK", "dk-authoring"),
		SE_AUTHORING ("SE", "se-authoring"),
		US_AUTHORING ("US", "us-authoring"),
		BE_AUTHORING("BE", "be-authoring"),
		NO_AUTHORING("NO", "no-authoring"),
		FIRST_TIME_LOINC_VALIDATION ("LOINC", "first-time-loinc-validation"),
		FIRST_TIME_COMMON_EDITION_VALIDATION ("COMMON", "first-time-common-edition"),
		LOINC_EDITION ("LOINC", "LoincEdition"),
		DANISH_EDITION("DK", "DanishEdition"),
		SWEDISH_EDITION("SE", "SwedishEdition"),
		US_EDITION("US", "USEdition"),
		BE_EDITION("BE", "BelgianEdition"),
		NO_EDITION("NO", "NorwegianEdition");
		private String name;
		private String releaseCenter;
		private AssertionGroupName(String releaseCenter, String name) {
			this.releaseCenter = releaseCenter;
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		public String getReleaseCenter() {
			return this.releaseCenter;
		}
		
	};
	
	private static final String SIMPLE_MAP = "simple map";
	private static final String RESOURCE = "resource";
	private static final String LOINC = "LOINC";
	private static final String ICD_9_COMPLEX_MAP ="ICD-9-CM";
	@Autowired	
	private AssertionService assertionService;
	@Autowired
	private AssertionGroupDao assertionGroupDao;
	
	private static final String[] SPANISH_EXTENSION_EXCLUDE_LIST = {"dd0d0406-7481-444a-9f04-b6fc7db49039","c3249e80-84f0-11e1-b0c4-0800200c9a66"};
	
	private static final String[] SNAPSHOT_EXCLUDE_LIST = {"4dbfed80-79b9-11e1-b0c4-0800200c9a66",
		"6336ec40-79b9-11e1-b0c4-0800200c9a66",
		"4572d730-7d08-11e1-b0c4-0800200c9a66",
		"411e9840-7d08-11e1-b0c4-0800200c9a66",
		"36f43550-7d08-11e1-b0c4-0800200c9a66",
		"3ab84230-7d08-11e1-b0c4-0800200c9a66",
		"5c6b6bc0-79b9-11e1-b0c4-0800200c9a66",
		"88315a11-4e71-49d2-977f-a5d5ac2a4dc4",
		"2e4fd620-7d08-11e1-b0c4-0800200c9a66",
		"6dbaed71-f031-4290-b74f-f35561c2e283",
		"c2975dd5-3869-4bf7-ac75-53fd53b90144"};
	
	private static final String[] COMMON_AUTHORING_ONLY_LIST = {"a49fabee-0d72-41b0-957d-32983c79f26c"};
	
	//SNOMED RT Identifier is deprecated from the international 20170731 release onwards.
	private static final String[] SNOMED_RT_IDENTIFIER_ASSERTIONS = {"730720b0-7f25-11e1-b0c4-0800200c9a66","83638340-7f25-11e1-b0c4-0800200c9a66",
																	"5e80ea3e-c4dd-4ae3-8b75-f0567e42b962","695cea40-7f25-11e1-b0c4-0800200c9a66"};
	
	private static final String[] US_EXCLUDE_LIST = {"31f5e2c8-b0b9-42ee-a9bf-87d95edad83b"};
	
	private static final String[] FIRST_TIME_COMMON_ADDITIONAL_EXCLUDE_LIST = {"1852b9d6-38cf-11e7-8b36-3c15c2c6e32e", "3cb10511-33b7-4eca-ba0e-93bcccf70d86", "48118153-d32a-4d1c-bfbc-23ed953e9991"};
		
/* the following were included but feel that they should be validated for project level as well.
	"6b34ab30-79b9-11e1-b0c4-0800200c9a66",
	"72184790-79b9-11e1-b0c4-0800200c9a66",
	"77fc7550-79b9-11e1-b0c4-0800200c9a66",
	"32b41aa0-7d08-11e1-b0c4-0800200c9a66",
*/
	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AssertionGroupImporter.class);
	
	public boolean isImportRequired() {
		List<AssertionGroup> allGroups = assertionGroupDao.findAll();
		if (allGroups == null || allGroups.isEmpty()) {
			return true;
		} else {
			return false;
		}
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
		List<AssertionGroup> allGroups = assertionGroupDao.findAll();
		List<String> existingGroups = new ArrayList<>();
		if (allGroups != null) {
			for (AssertionGroup group : allGroups) {
				LOGGER.info("Validation group is already created:" + group.getName());
				existingGroups.add(group.getName());
			}
		}
		List<Assertion> allAssertions = assertionService.findAll();
		for ( AssertionGroupName groupName : AssertionGroupName.values()) {
			if (!existingGroups.contains(groupName.getName())) {
				LOGGER.info("creating assertion group:" + groupName.getName());
				createAssertionGroup(groupName, allAssertions);
				LOGGER.info("assertion group created:" + groupName.getName());
			}
		}
	}


	private void createAssertionGroup(AssertionGroupName groupName, List<Assertion> allAssertions) {
		
		switch (groupName) {
			case FILE_CENTRIC_VALIDATION :
			case RELEASE_TYPE_VALIDATION :
			case COMPONENT_CENTRIC_VALIDATION :
				createAssertionGroupByKeyWord(allAssertions, groupName.getName());
				break;
			case LOINC_EDITION :
			case SPANISH_EDITION :
			case DANISH_EDITION	:
			case SWEDISH_EDITION :
			case INTERNATIONAL_EDITION :
			case US_EDITION :
			case BE_EDITION :
			case COMMON_EDITION :
			case NO_EDITION :
				createReleaseAssertionGroup(allAssertions, groupName);
				break;
			case COMMON_AUTHORING :
				createCommonSnapshotAssertionGroup(allAssertions);
				break;
			case INT_AUTHORING :
			case DK_AUTHORING :
			case SE_AUTHORING :
			case US_AUTHORING :
			case BE_AUTHORING :
			case NO_AUTHORING : 
				createSnapshotAssertionGroup(groupName);
				break;
			case FIRST_TIME_LOINC_VALIDATION :
			case FIRST_TIME_COMMON_EDITION_VALIDATION :
				createFirstTimeReleaseGroup(allAssertions, groupName);
				break;
			default :
			  break;
		}
		
	}

	private void createFirstTimeReleaseGroup(List<Assertion> allAssertions, AssertionGroupName groupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(groupName.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		String keyWords;
		for (Assertion assertion : allAssertions) {
			if (Arrays.asList(COMMON_AUTHORING_ONLY_LIST).contains(assertion.getUuid().toString()) || Arrays.asList(FIRST_TIME_COMMON_ADDITIONAL_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			keyWords=assertion.getKeywords();
			//exclude SNOMED RT assertions
			if ( Arrays.asList(SNOMED_RT_IDENTIFIER_ASSERTIONS).contains(assertion.getUuid().toString())) {
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
	
	private void createCommonSnapshotAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.COMMON_AUTHORING.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			 if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				 continue;
			 }
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getAssertionText().contains(SIMPLE_MAP)) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}
	

	private void createSnapshotAssertionGroup( AssertionGroupName groupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(groupName.getName());
		group = assertionGroupDao.create(group);
		List<Assertion> allAssertions = assertionService.getAssertionsByKeyWord("," + groupName.getReleaseCenter(), false);
		for (Assertion assertion : allAssertions) {
			//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
			if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			//exclude simple map file checking as term server extracts don't contain these
			if (assertion.getAssertionText().contains(SIMPLE_MAP)) {
				continue;
			}
			
			if (!assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName())) {
				assertionService.addAssertionToGroup(assertion, group);
			}
			if ( AssertionGroupName.US_AUTHORING.equals(groupName) && Arrays.asList(US_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", allAssertions.size(), group.getName() );
	}

	
	private List<Assertion> getCommonReleaseAssertions(List<Assertion> allAssertions) {
		List<Assertion> result = new ArrayList<>();
		String keywords;
		for (Assertion assertion : allAssertions) {
			 keywords = assertion.getKeywords();
			if (FILE_CENTRIC_VALIDATION.getName().equals(keywords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keywords)
					|| RELEASE_TYPE_VALIDATION.getName().equals(keywords)) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total common release assertions:" + result.size());
		return result;
	}
	
	private List<Assertion> getReleaseAssertionsByCenter(List<Assertion> allAssertions, String releaseCenter) {
		List<Assertion> result = new ArrayList<>();
		for (Assertion assertion : allAssertions) {
			String keywords = assertion.getKeywords();
			if (keywords.contains("," + releaseCenter)) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total release assertions found :{} for center:{}", result.size(), releaseCenter);
		return result;
	}
	
	private void createReleaseAssertionGroup(List<Assertion> allAssertions, AssertionGroupName groupName) {
		//create international assertion group
		AssertionGroup assertionGroup = new AssertionGroup();
		assertionGroup.setName(groupName.getName());
		assertionGroup = assertionGroupDao.create(assertionGroup);
		List<Assertion> assertionsToBeAdded = getCommonReleaseAssertions(allAssertions);
		assertionsToBeAdded.addAll(getReleaseAssertionsByCenter(allAssertions, groupName.getReleaseCenter()));
		
		for (Assertion assertion : assertionsToBeAdded) {
			if (Arrays.asList(COMMON_AUTHORING_ONLY_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			if ( AssertionGroupName.SPANISH_EDITION.equals(groupName) && Arrays.asList(SPANISH_EXTENSION_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			//exclude SNOMED RT assertions
			if ( AssertionGroupName.INTERNATIONAL_EDITION.equals(groupName) && Arrays.asList(SNOMED_RT_IDENTIFIER_ASSERTIONS).contains(assertion.getUuid().toString())) {
				continue;
			}
			if ( AssertionGroupName.US_EDITION.equals(groupName) && Arrays.asList(US_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			assertionService.addAssertionToGroup(assertion, assertionGroup);
		}
		LOGGER.info("Total assertions added {} for assertion group {}", assertionsToBeAdded.size(), groupName.getName() );
	}
	
	private void createAssertionGroupByKeyWord(List<Assertion> allAssertions, String assertionGroupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(assertionGroupName);
		group = assertionGroupDao.create(group);
		for (Assertion assertion : allAssertions) {
			if (assertion.getKeywords().equals(assertionGroupName)) {
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
	}
}
