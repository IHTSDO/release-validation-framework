package org.ihtsdo.rvf.service;

import org.hibernate.ObjectNotFoundException;
import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AssertionServiceImpl extends EntityServiceImpl<Assertion> implements AssertionService {

	@Autowired
	private AssertionDao assertionDao;
    @Autowired
    private EntityService entityService;

	@Autowired
	public AssertionServiceImpl(AssertionDao assertionDao) {
		super(assertionDao);
	}

	@Override
	public Assertion create(Map<String, String> properties) {
		Assertion assertion = new Assertion();
		setProperties(assertion, properties);
		assertionDao.save(assertion);
		return assertion;
	}

	@Override
	public Assertion update(Long id, Map<String, String> newValues) {
		Assertion assertion = find(id);
		setProperties(assertion, newValues);
		assertionDao.save(assertion);
		return assertion;
	}

    @Override
    public void delete(final Assertion assertion) {
        // first get all associated AssertionTests and delete them
        for(AssertionTest assertionTest : getAssertionTests(assertion))
        {
            entityService.delete(assertionTest);
        }

        // then delete assertion, but first merge assertion, in case it has been detached from session
        try {
            Object merged = assertionDao.getCurrentSession().merge(assertion);
            assertionDao.getCurrentSession().delete(merged);
        }
        catch (ObjectNotFoundException | org.hibernate.ObjectDeletedException e) {
            // disappeared already due to cascade
        }

    }

    @Override
	public List<Assertion> findAll() {
		return assertionDao.findAll();
	}

	@Override
	public Assertion find(Long id) {
        Assertion assertion = assertionDao.load(Assertion.class, id);
        if(assertion != null){
            return assertion;
        }
        else{
            throw new MissingEntityException(id);
        }
	}
	
	public Collection <Assertion> find(List<Long> ids) {
		Collection <Assertion>assertionsFound = new ArrayList<Assertion>();
        for (Long id : ids) {
        	assertionsFound.add(find(id));
        }
        return assertionsFound;
	}

	@Override
	public Assertion find(UUID uuid) {
        Assertion assertion = assertionDao.findByUuid(Assertion.class, uuid);
        if(assertion != null){
            return assertion;
        }
        else{
            throw new MissingEntityException(uuid);
        }
	}

	// todo use beanUtils/propertyUtils reflection for each of the properties
	private void setProperties(Assertion assertion, Map<String, String> properties) {
		assertion.setName(properties.get("name"));
		assertion.setDescription(properties.get("description"));
		assertion.setDocLink(properties.get("docLink"));
		assertion.setKeywords(properties.get("keywords"));
		assertion.setStatement(properties.get("statement"));
        if(properties.get("uuid") != null ){
            assertion.setUuid(UUID.fromString(properties.get("statement")));
        }
	}

    @Override
    public List<AssertionTest> getAssertionTests(Assertion assertion){
        return assertionDao.getAssertionTests(assertion);
    }

    @Override
    public List<AssertionTest> getAssertionTests(Long assertionId){
        return assertionDao.getAssertionTests(assertionId);
    }

    @Override
    public List<AssertionTest> getAssertionTests(UUID uuid){
        return assertionDao.getAssertionTests(uuid);
    }

    @Override
    public List<Test> getTests(Assertion assertion){
        return assertionDao.getTests(assertion);
    }

    @Override
    public List<Test> getTests(Long assertionId){
        return assertionDao.getTests(assertionId);
    }

    @Override
    public List<Test> getTests(UUID uuid){
        return assertionDao.getTests(uuid);
    }

    @Override
    public Assertion addTest(Assertion assertion, Test test){

        // see if matching assertion test already exists
        AssertionTest assertionTest = assertionDao.getAssertionTests(assertion, test);
        if(assertionTest == null)
        {
            assertionTest = new AssertionTest();
            // verify if assertion has been saved - otherwise save it
            if(assertion.getId() == null){
                assertionDao.save(assertion);
            }
            // verify if test has been saved - otherwise save first
            if(test.getId() == null){
                entityService.create(test);
            }
            assertionTest.setTest(test);
            assertionTest.setInactive(false);
            assertionTest.setAssertion(assertion);
            entityService.create(assertionTest);
        }
        else{
            assertionTest.setTest(test);
            assertionTest.setInactive(false);
            assertionTest.setAssertion(assertion);
            entityService.update(assertionTest);
        }

        return assertion;
    }

    @Override
    public Assertion addTests(Assertion assertion, Collection<Test> tests){
        for(Test test : tests)
        {
            addTest(assertion, test);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTest(Assertion assertion, Test test){
        // get assertion tests for assertion
        AssertionTest assertionTest = assertionDao.getAssertionTests(assertion, test);
        // delete assertion test
        if (assertionTest != null) {
            entityService.delete(assertionTest);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTests(Assertion assertion, Collection<Test> tests){
        for(Test test : tests)
        {
            deleteTest(assertion, test);
        }

        return assertion;
    }

    @Override
    public Long count() {
        return super.count(Assertion.class);
    }

    @Override
    public List<AssertionGroup> getGroupsForAssertion(Assertion assertion) {
        return assertionDao.getGroupsForAssertion(assertion);
    }

    @Override
    public List<AssertionGroup> getGroupsForAssertion(Long assertionId) {
        return assertionDao.getGroupsForAssertion(assertionId);
    }

    @Override
    public List<Assertion> getAssertionsForGroup(AssertionGroup group) {
        return assertionDao.getAssertionsForGroup(group);
    }

    @Override
    public List<Assertion> getAssertionsForGroup(Long groupId) {
        return assertionDao.getAssertionsForGroup(groupId);
    }

    @Override
    public AssertionGroup addAssertionToGroup(Assertion assertion, AssertionGroup group){
        /*
            see if group already exists. We get groups for assertion, instead of assertions for group since getting
            assertions is likely to return a large number of entites. It is likely that a group might have a large
            number of assertions
          */
        List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
        if(! assertionGroups.contains(group))
        {
            group.getAssertions().add(assertion);
            assertion.getGroups().add(group);
            assertionDao.update(assertion);
        }

        return (AssertionGroup) entityService.update(group);
    }

    @Override
    public AssertionGroup removeAssertionFromGroup(Assertion assertion, AssertionGroup group){
        /*
            see if group already exists. We get groups for assertion, instead of assertions for group since getting
            assertions is likely to return a large number of entites. It is likely that a group might have a large
            number of assertions
          */

        List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
        if(assertionGroups.contains(group))
        {
            group.getAssertions().remove(assertion);
            assertion.getGroups().remove(group);
            assertionDao.update(assertion);
        }

        return (AssertionGroup) entityService.update(group);
    }
}
