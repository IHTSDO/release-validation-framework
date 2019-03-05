package org.ihtsdo.rvf.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.repository.AssertionGroupRepository;
import org.ihtsdo.rvf.repository.AssertionRepository;
import org.ihtsdo.rvf.repository.AssertionTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssertionServiceImpl implements AssertionService {

	@Autowired
	private AssertionRepository assertionRepo;
	
	@Autowired
	private AssertionGroupRepository assertionGroupRepo;
	
	@Autowired
	private AssertionTestRepository assertionTestRepo;


	@Override
	public void delete(final Assertion assertion) {
		// first get all associated AssertionTests and delete them
		for ( AssertionTest assertionTest : getAssertionTests(assertion)) {
			assertionTestRepo.delete(assertionTest);
		}
		assertionRepo.delete(assertion);
	}

	@Override
	public List<Assertion> findAll() {
		return assertionRepo.findAll();
	}

	@Override
	public Assertion find(final Long id) {
		return assertionRepo.findByAssertionId(id);
	}
	
	@Override
	public Assertion findAssertionByUUID(final UUID uuid) {
		return assertionRepo.findByUuid(uuid.toString());
	}

	@Override
	public List<AssertionTest> getAssertionTests(final Assertion assertion){
		return assertionTestRepo.findAssertionTestsByAssertion(assertion);
	}

	@Override
	public Assertion addTest( Assertion assertion, final Test test){
		AssertionTest assertionTest = new AssertionTest();
		assertionTest.setAssertion(assertion);
		assertionTest.setTest(test);
		assertionTestRepo.save(assertionTest);
		return assertion;
	}

	@Override
	public Assertion addTests(final Assertion assertion, final Collection<Test> tests){
		for ( Test test : tests) {
			addTest(assertion, test);
		}
		return assertion;
	}

	@Override
	public Assertion deleteTest(final Assertion assertion, final Test test){
		// get assertion tests for assertion
		AssertionTest assertionTest = assertionTestRepo.findByAssertionAndTest(assertion, test);
		// delete assertion test
		if (assertionTest != null) {
			assertionTestRepo.delete(assertionTest);
		}
		return assertion;
	}

	@Override
	public Assertion deleteTests(final Assertion assertion, Collection<Test> tests){
		//If no tests specified, delete all tests for this assertion
		if (tests == null) {
			tests = getTests(assertion);
		}
		
		for (final Test test : tests) {
			deleteTest(assertion, test);
		}

		return assertion;
	}

	@Override
	public Long count() {
		return assertionRepo.count();
	}
	
	@Override
	public AssertionGroup addAssertionToGroup(final Assertion assertion, final AssertionGroup group){
	
		List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
		if (!assertionGroups.contains(group)) {
			group.getAssertions().add(assertion);
			assertionRepo.save(assertion);
		}
		return assertionGroupRepo.save(group);
	}

	@Override
	public AssertionGroup removeAssertionFromGroup(final Assertion assertion, final AssertionGroup group){
		/*
			see if group already exists. We get groups for assertion, instead of assertions for group since getting
			assertions is likely to return a large number of entities. It is likely that a group might have a large
			number of assertions
		  */
		List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
		for (AssertionGroup grp : assertionGroups) {
			if (grp.getId().equals(group.getId())) {
				grp.removeAssertion(assertion);
				return assertionGroupRepo.save(grp);
			}
		}
		return group;
	}

	
	@Override
	public List<Assertion> getAssertionsByKeyWords(String keyWord, boolean exactMatched) {
		if (exactMatched) {
			return assertionRepo.findAssertionsByKeywords(keyWord);
		} else {
			List<Assertion> result = assertionRepo.findAll();
			List<Assertion> matchedResults = new ArrayList<>();
			for (Assertion assertion : result) {
				if (assertion.getKeywords().contains(keyWord)) {
					matchedResults.add(assertion);
				}
			}
			return matchedResults;
		}
	}

	@Override
	public AssertionGroup getAssertionGroupByName(final String groupName) {
		return assertionGroupRepo.findByName(groupName);
	}

	@Override
	public List<AssertionGroup> getAssertionGroupsByNames( final List<String> groupNames) {
		List<AssertionGroup> result = new ArrayList<>();
		for (final String groupName : groupNames) {
			AssertionGroup group = getAssertionGroupByName(groupName);
			if (group != null) {
				result.add(group);
			}
		}
		return result;
	}

	@Override
	public List<AssertionGroup> getAllAssertionGroups() {
		return assertionGroupRepo.findAll();
	}

	@Override
	public Assertion create(Assertion assertion) {
		return assertionRepo.save(assertion);
	}

	@Override
	public Assertion save(Assertion assertion) {
		return assertionRepo.save(assertion);
	}

	@Override
	public List<Test> getTests(Assertion assertion) {
		List<Test> results = new ArrayList<>();
		for (AssertionTest assertionTest : assertionTestRepo.findAssertionTestsByAssertion(assertion)) {
			results.add(assertionTest.getTest());
		}
		return results;
	}

	@Override
	public List<Test> getTestsByAssertionId(Long assertionId) {
		Assertion assertion = assertionRepo.getOne(assertionId);
		return getTests(assertion);
	}

	@Override
	public List<AssertionGroup> getGroupsForAssertion(Assertion assertion) {
		List<AssertionGroup> allGroups = assertionGroupRepo.findAll();
		List<AssertionGroup> matched = new ArrayList<>();
		for (AssertionGroup grp : allGroups) {
			if (grp.getAssertions().contains(assertion)) {
				matched.add(grp);
			}
		}
		return matched;
	}

	@Override
	public List<AssertionGroup> getGroupsForAssertion(Long assertionId) {
		Assertion assertion = assertionRepo.getOne(assertionId);
		return getGroupsForAssertion(assertion);
	}

	@Override
	public Assertion getAssertionByUuid(UUID uuid) {
		return assertionRepo.findByUuid(uuid.toString());
	}

	@Override
	public AssertionGroup createAssertionGroup(AssertionGroup group) {
		return assertionGroupRepo.save(group);
	}
}
