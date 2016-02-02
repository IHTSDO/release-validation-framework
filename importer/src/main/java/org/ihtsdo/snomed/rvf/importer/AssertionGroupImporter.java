package org.ihtsdo.snomed.rvf.importer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.dao.AssertionGroupDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class AssertionGroupImporter {
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
		//international
		createInternationalAssertionGroup(allAssertions);
		//assertion group by keywords
		createAssertionGroupByKeyWord(allAssertions,FILE_CENTRIC_VALIDATION);
		createAssertionGroupByKeyWord(allAssertions,RELEASE_TYPE_VALIDATION);
		createAssertionGroupByKeyWord(allAssertions,COMPONENT_CENTRIC_VALIDATION);
		//Snapshot
		createSnapshotContentAssertionGroup(allAssertions);
		//Spanish edition
		createSpanishEditionAssertionGroup(allAssertions);
		
	}



	private void createSpanishEditionAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(SPANISH_EDITION);
		group = assertionGroupDao.create(group);
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(LOINC)) {
				if (!Arrays.asList(SPANISH_EDITION_EXCLUDE_LIST).contains(assertion.getUuid())) {
					assertionService.addAssertionToGroup(assertion, group);
				}
			}
		}
	}

	private void createSnapshotContentAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(SNAPSHOT_CONTENT_VALIDATION);
		group = assertionGroupDao.create(group);
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE) && !assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (UUID.fromString("4572d730-7d08-11e1-b0c4-0800200c9a66").equals(assertion.getUuid())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getKeywords().contains(COMPONENT_CENTRIC_VALIDATION ) && assertion.getAssertionText().contains("simple map")) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
		
	}

	private void createInternationalAssertionGroup(List<Assertion> allAssertions) {
		//create international assertion group
		AssertionGroup internationalGroup = new AssertionGroup();
		internationalGroup.setName(INTERNATIONAL_EDITION);
		internationalGroup = assertionGroupDao.create(internationalGroup);
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains(RESOURCE)) {
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
