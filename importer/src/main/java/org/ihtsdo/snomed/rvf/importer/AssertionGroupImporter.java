package org.ihtsdo.snomed.rvf.importer;

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
	private static final String SIMPLE_MAP = "simple map";
	private static final String SPANISH_EDITION = "SpanishEdition";
	private static final String INTERNATIONAL_EDITION = "InternationalEdition";
	private static final String SNAPSHOT_CONTENT_VALIDATION = "SnapshotContentValidation";
	private static final String RESOURCE = "resource";
	private static final String LOINC = "LOINC";
	private static final String COMPONENT_CENTRIC_VALIDATION = "component-centric-validation";
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
	private static final String FILE_CENTRIC_VALIDATION = "file-centric-validation";
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
			"4572d730-7d08-11e1-b0c4-0800200c9a66"};
	
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
		List<Assertion> allAssertions = assertionService.findAll();
		//assertion group by keywords
		createAssertionGroupByKeyWord(allAssertions,FILE_CENTRIC_VALIDATION);
		createAssertionGroupByKeyWord(allAssertions,RELEASE_TYPE_VALIDATION);
		createAssertionGroupByKeyWord(allAssertions,COMPONENT_CENTRIC_VALIDATION);
		//Spanish edition
		createSpanishEditionAssertionGroup(allAssertions);
		//international
		createInternationalAssertionGroup(allAssertions);
		//Snapshot
		createSnapshotContentAssertionGroup(allAssertions);
	}



	private void createSpanishEditionAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(SPANISH_EDITION);
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
		group.setName(SNAPSHOT_CONTENT_VALIDATION);
		group = assertionGroupDao.create(group);
		//TODO we should create a SCA task level validation assertion group
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getKeywords().contains(COMPONENT_CENTRIC_VALIDATION ) && assertion.getAssertionText().contains(SIMPLE_MAP)) {
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
		internationalGroup.setName(INTERNATIONAL_EDITION);
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
