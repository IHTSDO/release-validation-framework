package org.ihtsdo.rvf.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;

public interface AssertionService {
	//Assertion
	Assertion create(Assertion assertion);

	Assertion save(Assertion assertion);
	
	void delete(Assertion assertion);
	
	List<Assertion> findAll();
	
	Assertion find(Long id);
	
	Assertion findAssertionByUUID(UUID uuid);

	//Assertion Test
	List<AssertionTest> getAssertionTests(Assertion assertion);

	//Test
	List<Test> getTests(Assertion assertion);

	List<Test> getTestsByAssertionId(Long assertionId);

	Assertion addTest(Assertion assertion, Test test);

	Assertion addTests(Assertion assertion, Collection<Test> tests);

	Assertion deleteTest(Assertion assertion, Test test);

	Assertion deleteTests(Assertion assertion, Collection<Test> tests);

	Long count();

	//Assertion Group
	List<AssertionGroup> getGroupsForAssertion(Assertion assertion);

	List<AssertionGroup> getGroupsForAssertion(Long assertionId);

	AssertionGroup addAssertionToGroup(Assertion assertion, AssertionGroup group);

	AssertionGroup removeAssertionFromGroup(Assertion assertion, AssertionGroup group);

	AssertionGroup getAssertionGroupByName(String groupName);

	List<AssertionGroup> getAssertionGroupsByNames(List<String> groupNames);
	List<AssertionGroup> getAllAssertionGroups();

	Assertion getAssertionByUuid(UUID assertionUUID);
	
	List<Assertion> getAssertionsByKeyWords(String keyWord, boolean exactMatch);

	AssertionGroup createAssertionGroup(AssertionGroup group);
}
