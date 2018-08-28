package org.ihtsdo.rvf.dao;

import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;

public interface AssertionDao extends EntityDao<Assertion> {

	List<Assertion> findAll();

	AssertionTest getAssertionTests(Long assertionId, Long testId);

	AssertionTest getAssertionTests(UUID uuid, Long testId);

	List<AssertionTest> getAssertionTests(Long assertionId);

	List<AssertionTest> getAssertionTests(UUID uuid);

	AssertionTest getAssertionTests(Assertion assertion, Test test);

	List<AssertionTest> getAssertionTests(Assertion assertion);

	List<Test> getTests(Assertion assertion);

	List<Test> getTests(Long assertionid);

	List<Test> getTests(UUID uuid);

	List<AssertionGroup> getGroupsForAssertion(Assertion assertion);

	List<AssertionGroup> getGroupsForAssertion(Long assertionId);

	List<Assertion> getAssertionsForGroup(AssertionGroup group);

	List<Assertion> getAssertionsForGroup(Long groupId);

	List<Assertion> getAssertionsByKeyWord(String keyName, boolean isFullyMatched);
	
	AssertionGroup getAssertionGroupsByName(String groupName);

	Assertion getAssertionByUUID(String assertionUUID);
}
