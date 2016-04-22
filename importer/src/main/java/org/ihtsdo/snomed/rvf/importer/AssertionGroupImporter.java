package org.ihtsdo.snomed.rvf.importer;

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
	
	 enum AssertionGroupName {
		FILE_CENTRIC_VALIDATION ("file-centric-validation"),
		COMPONENT_CENTRIC_VALIDATION ("component-centric-validation"),
		RELEASE_TYPE_VALIDATION ("release-type-validation"),
		SPANISH_EDITION ("SpanishEdition"),
		INTERNATIONAL_EDITION ("InternationalEdition"),
		SNAPSHOT_CONTENT_VALIDATION ("SnapshotContentValidation"),
		FIRST_TIME_LOINC_VALIDATION ("firstTimeLOINCValidation"),
		FIRST_TIME_INTERNATIONAL_RELEASE_VALIDATION("firstTimeInternationalReleaseValidation");
		private String name;
		private AssertionGroupName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
	};
	
	private static final String SIMPLE_MAP = "simple map";
	private static final String RESOURCE = "resource";
	private static final String LOINC = "LOINC";
	@Autowired	
	private AssertionService assertionService;
	@Autowired
	private AssertionGroupDao assertionGroupDao;
	
	private static final String[] SPANISH_EDITION_EXCLUDE_LIST = {"dd0d0406-7481-444a-9f04-b6fc7db49039","cc9c5340-84f0-11e1-b0c4-0800200c9a66","c3249e80-84f0-11e1-b0c4-0800200c9a66"};
	
	private static final String[] SNAPSHOT_EXCLUDE_LIST = {"4dbfed80-79b9-11e1-b0c4-0800200c9a66",
			"6336ec40-79b9-11e1-b0c4-0800200c9a66",
			"6b34ab30-79b9-11e1-b0c4-0800200c9a66",
			"72184790-79b9-11e1-b0c4-0800200c9a66",
			"77fc7550-79b9-11e1-b0c4-0800200c9a66",
			"2e4fd620-7d08-11e1-b0c4-0800200c9a66",
			"32b41aa0-7d08-11e1-b0c4-0800200c9a66",
			"4572d730-7d08-11e1-b0c4-0800200c9a66",
			"411e9840-7d08-11e1-b0c4-0800200c9a66",
			"36f43550-7d08-11e1-b0c4-0800200c9a66",
			"3ab84230-7d08-11e1-b0c4-0800200c9a66"};
	
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
			case SPANISH_EDITION :
				createSpanishEditionAssertionGroup(allAssertions);
				break;
			case INTERNATIONAL_EDITION :
				createInternationalAssertionGroup(allAssertions);
				break;
			case SNAPSHOT_CONTENT_VALIDATION :
				createSnapshotContentAssertionGroup(allAssertions);
				break;
			case FIRST_TIME_LOINC_VALIDATION :
				createLoincFirstTimeValidationGroup(allAssertions);
				break;
			case FIRST_TIME_INTERNATIONAL_RELEASE_VALIDATION :
				createFirstTimeInternational(allAssertions);
				break;
			default :
			  break;
		}
		
	}

	private void createFirstTimeInternational(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.FIRST_TIME_INTERNATIONAL_RELEASE_VALIDATION.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE)) {
				String assertionText = assertion.getAssertionText();
				if (!assertion.getKeywords().contains(LOINC) && !assertionText.contains("previous") && !assertionText.contains("New inactive states follow active states") ) {
					assertionService.addAssertionToGroup(assertion, group);
					counter++;
				}
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void createLoincFirstTimeValidationGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.FIRST_TIME_LOINC_VALIDATION.getName());
		group = assertionGroupDao.create(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(AssertionGroupName.RELEASE_TYPE_VALIDATION.getName())) {
				if (assertion.getKeywords().contains(LOINC)) {
					assertionService.addAssertionToGroup(assertion, group);
					counter++;
				}
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
		
	}

	private void createSpanishEditionAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.SPANISH_EDITION.getName());
		group = assertionGroupDao.create(group);
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(LOINC)) {
				if (!Arrays.asList(SPANISH_EDITION_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					assertionService.addAssertionToGroup(assertion, group);
				}
			}
		}
	}

	private void createSnapshotContentAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.SNAPSHOT_CONTENT_VALIDATION.getName());
		group = assertionGroupDao.create(group);
		//TODO we should create a SCA task level validation assertion group
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(AssertionGroupName.RELEASE_TYPE_VALIDATION.getName())) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getKeywords().contains(AssertionGroupName.COMPONENT_CENTRIC_VALIDATION.getName() ) && assertion.getAssertionText().contains(SIMPLE_MAP)) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void createInternationalAssertionGroup(List<Assertion> allAssertions) {
		//create international assertion group
		AssertionGroup internationalGroup = new AssertionGroup();
		internationalGroup.setName(AssertionGroupName.INTERNATIONAL_EDITION.getName());
		internationalGroup = assertionGroupDao.create(internationalGroup);
		for (Assertion assertion : allAssertions) {
			String keywords = assertion.getKeywords();
			if (!keywords.contains(RESOURCE) && !keywords.contains(LOINC)) {
				assertionService.addAssertionToGroup(assertion, internationalGroup);
			}
		}
	}
	
	private void createAssertionGroupByKeyWord(List<Assertion> allAssertions, String assertionGroupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(assertionGroupName);
		group = assertionGroupDao.create(group);
		for (Assertion assertion : allAssertions) {
			if (assertion.getKeywords().contains(assertionGroupName)) {
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
	}
}
