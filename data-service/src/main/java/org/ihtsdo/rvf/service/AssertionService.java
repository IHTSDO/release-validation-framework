package org.ihtsdo.rvf.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;

public interface AssertionService extends EntityService<Assertion> {

	Assertion create(Map<String, String> properties);

	Assertion update(Long id, Map<String, String> newValues);

	@Override
	void delete(Assertion assertion);

	List<Assertion> findAll();

	Assertion find(Long id);
	
	Collection<Assertion> find (List<Long> ids);

	Assertion find(UUID uuid);

	List<AssertionTest> getAssertionTests(Assertion assertion);

	List<AssertionTest> getAssertionTests(Long assertionId);

	List<AssertionTest> getAssertionTests(UUID uuid);

	List<Test> getTests(Assertion assertion);

	List<Test> getTests(Long assertionId);

	List<Test> getTests(UUID uuid);

	Assertion addTest(Assertion assertion, Test test);

	Assertion addTests(Assertion assertion, Collection<Test> tests);

	Assertion deleteTest(Assertion assertion, Test test);

	Assertion deleteTests(Assertion assertion, Collection<Test> tests);

	Long count();

	List<AssertionGroup> getGroupsForAssertion(Assertion assertion);

	List<AssertionGroup> getGroupsForAssertion(Long assertionId);

	List<Assertion> getAssertionsForGroup(AssertionGroup group);

	List<Assertion> getAssertionsForGroup(Long groupId);

	AssertionGroup addAssertionToGroup(Assertion assertion, AssertionGroup group);

	AssertionGroup removeAssertionFromGroup(Assertion assertion, AssertionGroup group);

	List<Assertion> getResourceAssertions();
	AssertionGroup getAssertionGroupByName(String groupName);
}
