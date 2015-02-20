package org.ihtsdo.rvf.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

@Service
@Transactional
public class AssertionServiceImpl extends EntityServiceImpl<Assertion> implements AssertionService {

	@Autowired
	private AssertionDao assertionDao;
    @Autowired
    private EntityService entityService;

	@Autowired
	public AssertionServiceImpl(final AssertionDao assertionDao) {
		super(assertionDao);
	}

	@Override
	public Assertion create(final Map<String, String> properties) {
		final Assertion assertion = new Assertion();
		setProperties(assertion, properties);
		assertionDao.save(assertion);
		return assertion;
	}

	@Override
	public Assertion update(final Long id, final Map<String, String> newValues) {
		final Assertion assertion = find(id);
		setProperties(assertion, newValues);
		assertionDao.save(assertion);
		return assertion;
	}

    @Override
    public void delete(final Assertion assertion) {
        // first get all associated AssertionTests and delete them
        for(final AssertionTest assertionTest : getAssertionTests(assertion))
        {
            entityService.delete(assertionTest);
        }

        // then delete assertion, but first merge assertion, in case it has been detached from session
        try {
            final Object merged = assertionDao.getCurrentSession().merge(assertion);
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
	public Assertion find(final Long id) {
        final Assertion assertion = assertionDao.load(Assertion.class, id);
        if(assertion != null){
            return assertion;
        }
        else{
            throw new MissingEntityException(id);
        }
	}
	
	@Override
	public Collection <Assertion> find(final List<Long> ids) {
		final Collection <Assertion>assertionsFound = new ArrayList<Assertion>();
        for (final Long id : ids) {
        	assertionsFound.add(find(id));
        }
        return assertionsFound;
	}

	@Override
	public Assertion find(final UUID uuid) {
        final Assertion assertion = assertionDao.findByUuid(Assertion.class, uuid);
        if(assertion != null){
            return assertion;
        }
        else{
            throw new MissingEntityException(uuid);
        }
	}

	// todo use beanUtils/propertyUtils reflection for each of the properties
	private void setProperties(final Assertion assertion, final Map<String, String> properties) {
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
    public List<AssertionTest> getAssertionTests(final Assertion assertion){
        return assertionDao.getAssertionTests(assertion);
    }

    @Override
    public List<AssertionTest> getAssertionTests(final Long assertionId){
        return assertionDao.getAssertionTests(assertionId);
    }

    @Override
    public List<AssertionTest> getAssertionTests(final UUID uuid){
        return assertionDao.getAssertionTests(uuid);
    }

    @Override
    public List<Test> getTests(final Assertion assertion){
        return assertionDao.getTests(assertion);
    }

    @Override
    public List<Test> getTests(final Long assertionId){
        return assertionDao.getTests(assertionId);
    }

    @Override
    public List<Test> getTests(final UUID uuid){
        return assertionDao.getTests(uuid);
    }

    @Override
    public Assertion addTest(final Assertion assertion, final Test test){

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
    public Assertion addTests(final Assertion assertion, final Collection<Test> tests){
        for(final Test test : tests)
        {
            addTest(assertion, test);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTest(final Assertion assertion, final Test test){
        // get assertion tests for assertion
        final AssertionTest assertionTest = assertionDao.getAssertionTests(assertion, test);
        // delete assertion test
        if (assertionTest != null) {
            entityService.delete(assertionTest);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTests(final Assertion assertion, final Collection<Test> tests){
        for(final Test test : tests)
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
    public List<AssertionGroup> getGroupsForAssertion(final Assertion assertion) {
        return assertionDao.getGroupsForAssertion(assertion);
    }

    @Override
    public List<AssertionGroup> getGroupsForAssertion(final Long assertionId) {
        return assertionDao.getGroupsForAssertion(assertionId);
    }

    @Override
    public List<Assertion> getAssertionsForGroup(final AssertionGroup group) {
        return assertionDao.getAssertionsForGroup(group);
    }

    @Override
    public List<Assertion> getAssertionsForGroup(final Long groupId) {
        return assertionDao.getAssertionsForGroup(groupId);
    }

    @Override
    public AssertionGroup addAssertionToGroup(final Assertion assertion, final AssertionGroup group){
        /*
            see if group already exists. We get groups for assertion, instead of assertions for group since getting
            assertions is likely to return a large number of entites. It is likely that a group might have a large
            number of assertions
          */
        final List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
        if(! assertionGroups.contains(group))
        {
            group.getAssertions().add(assertion);
            assertion.getGroups().add(group);
            assertionDao.update(assertion);
        }

        return (AssertionGroup) entityService.update(group);
    }

    @Override
    public AssertionGroup removeAssertionFromGroup(final Assertion assertion, final AssertionGroup group){
        /*
            see if group already exists. We get groups for assertion, instead of assertions for group since getting
            assertions is likely to return a large number of entites. It is likely that a group might have a large
            number of assertions
          */

        final List<AssertionGroup> assertionGroups = getGroupsForAssertion(assertion);
        if(assertionGroups.contains(group))
        {
            group.getAssertions().remove(assertion);
            assertion.getGroups().remove(group);
            assertionDao.update(assertion);
        }

        return (AssertionGroup) entityService.update(group);
    }

	@Override
	public List<Assertion> getResourceAssertions() {
		return assertionDao.getAssertionsByKeywords("resource");
				
	}

	@Override
	public AssertionGroup getAssertionGroupByName(final String groupName) {
		return assertionDao.getAssertionGroupsByName(groupName);
	}

}
