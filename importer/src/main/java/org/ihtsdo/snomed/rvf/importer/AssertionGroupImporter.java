package org.ihtsdo.snomed.rvf.importer;

import java.util.List;

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
	@Autowired	
	private AssertionService assertionService;
	@Autowired
	private AssertionGroupDao assertionGroupDao;
	
	public boolean isImportRequired() {
		List<AssertionGroup> allGroups = assertionGroupDao.findAll();
		if (allGroups == null || allGroups.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	public void importAssertionGroups() {
		List<Assertion> allAssertions = assertionService.findAll();
		AssertionGroup internationalGroup = new AssertionGroup();
		internationalGroup.setName("InternationalEdition");
		internationalGroup = assertionGroupDao.create(internationalGroup);
		for (Assertion assertion : allAssertions) {
			if (!assertion.getKeywords().contains("resource")) {
				assertionService.addAssertionToGroup(assertion, internationalGroup);
			}
		}
	}
}
