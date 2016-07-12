package org.ihtsdo.snomed.rvf.importer;

import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.COMPONENT_CENTRIC_VALIDATION;
import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.FILE_CENTRIC_VALIDATION;
import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.RELEASE_TYPE_VALIDATION;
import static org.ihtsdo.snomed.rvf.importer.AssertionGroupImporter.AssertionGroupName.SNAPSHOT_CONTENT_VALIDAITON;

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
		COMPONENT_CENTRIC_VALIDATION ("COMMON","component-centric-validation"),
		RELEASE_TYPE_VALIDATION ("COMMON","release-type-validation"),
		SPANISH_EDITION ("ES","SpanishEdition"),
		INTERNATIONAL_EDITION ("INT","InternationalEdition"),
		COMMON_SNAPSHOT_VALIDATION ("COMMON","CommonSnapshotValidation"),
		INT_SNAPSHOT_VALIDATION ("INT","IntSnapshotValidation"),
		DK_SNAPSHOT_VALIDATION ("DK","DkSnapshotValidation"),
		SE_SNAPSHOT_VALIDATION ("SE","SeSnapshotValidation"),
		FIRST_TIME_LOINC_VALIDATION ("LOINC","firstTimeLOINCValidation"),
		FIRST_TIME_INTERNATIONAL_RELEASE_VALIDATION("INT","firstTimeInternationalReleaseValidation"),
		FIRST_TIME_COMMON_SNAPSHOT_VALIDATION ("COMMON","FirstTimeCommonSnapshotValidation"),
		LOINC_EDITION ("LOINC","LoincEdition"),
		DANISH_EDITION("DK","DanishEdition"),
		SWEDISH_EDITION("SE","SwedishEdition"),
		//Remove this when the orchestration service has been updated.
		SNAPSHOT_CONTENT_VALIDAITON("INT","SnapshotContentValidation");
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
		"2e4fd620-7d08-11e1-b0c4-0800200c9a66"};
	
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
				LOGGER.debug("Validation group is already created:" + group.getName());
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
				createReleaseAssertionGroup(allAssertions, groupName);
				break;
			case COMMON_SNAPSHOT_VALIDATION :
				createCommonSnapshotAssertionGroup(allAssertions);
				break;
			case INT_SNAPSHOT_VALIDATION :
			case DK_SNAPSHOT_VALIDATION :
			case SE_SNAPSHOT_VALIDATION :
				createSnapshotAssertionGroup(groupName);
				break;
			case SNAPSHOT_CONTENT_VALIDAITON:
				createLegacySnapshotContentValidation(allAssertions);
				break;
			case FIRST_TIME_LOINC_VALIDATION :
			case FIRST_TIME_INTERNATIONAL_RELEASE_VALIDATION :
			case FIRST_TIME_COMMON_SNAPSHOT_VALIDATION :
				createFirstTimeReleaseGroup(allAssertions, groupName);
				break;
			default :
			  break;
		}
		
	}

	
	private void createLegacySnapshotContentValidation(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(SNAPSHOT_CONTENT_VALIDAITON.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				continue;
			}
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords) 
					|| keyWords.contains(SNAPSHOT_CONTENT_VALIDAITON.getReleaseCenter())) {
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

	private void createFirstTimeReleaseGroup(List<Assertion> allAssertions, AssertionGroupName groupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(groupName.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		String keyWords;
		for (Assertion assertion : allAssertions) {
			keyWords=assertion.getKeywords();
			if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				continue;
			} 
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)
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
		group.setName(AssertionGroupName.COMMON_SNAPSHOT_VALIDATION.getName());
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
				if (!assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName())) {
					assertionService.addAssertionToGroup(assertion, group);
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
			
			if ( AssertionGroupName.SPANISH_EDITION.equals(groupName) && Arrays.asList(SPANISH_EXTENSION_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
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
