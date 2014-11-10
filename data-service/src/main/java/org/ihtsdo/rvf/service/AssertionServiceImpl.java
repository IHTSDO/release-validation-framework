package org.ihtsdo.rvf.service;

import org.hibernate.ObjectNotFoundException;
import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	// todo use beanUtils/propertyUtils reflection for each of the properties
	private void setProperties(Assertion assertion, Map<String, String> properties) {
		assertion.setName(properties.get("name"));
		assertion.setDescription(properties.get("description"));
		assertion.setDocLink(properties.get("docLink"));
		assertion.setKeywords(properties.get("keywords"));
		assertion.setStatement(properties.get("statement"));
	}

    @Override
    public List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter){
        return assertionDao.getAssertionTests(assertion, releaseCenter);
    }

    @Override
    public List<AssertionTest> getAssertionTests(Long assertionId, Long releaseCenterId){
        return assertionDao.getAssertionTests(assertionId, releaseCenterId);
    }

    @Override
    public List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter){
        return assertionDao.getTests(assertion, releaseCenter);
    }

    @Override
    public List<Test> getTests(Long assertionId, Long releaseCenterId){
        return assertionDao.getTests(assertionId, releaseCenterId);
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
    public List<Test> getTests(Assertion assertion){
        return assertionDao.getTests(assertion);
    }

    @Override
    public List<Test> getTests(Long assertionId){
        return assertionDao.getTests(assertionId);
    }

    @Override
    public Assertion addTest(Assertion assertion, ReleaseCenter releaseCenter, Test test){

        // see if matching assertion test already exists
        AssertionTest assertionTest = assertionDao.getAssertionTests(assertion, releaseCenter, test);
        if(assertionTest == null)
        {
            assertionTest = new AssertionTest();
            assertionTest.setTest(test);
            assertionTest.setInactive(false);
            assertionTest.setAssertion(assertion);
            assertionTest.setCenter(releaseCenter);
            entityService.create(assertionTest);
        }
        else{
            assertionTest.setTest(test);
            assertionTest.setInactive(false);
            assertionTest.setAssertion(assertion);
            assertionTest.setCenter(releaseCenter);
            entityService.update(assertionTest);
        }

        return assertion;
    }

    @Override
    public Assertion addTest(Assertion assertion, Test test){
        return addTest(assertion, entityService.getIhtsdo(), test);
    }

    @Override
    public Assertion addTests(Assertion assertion, ReleaseCenter releaseCenter, Collection<Test> tests){
        for(Test test : tests)
        {
            addTest(assertion, releaseCenter, test);
        }

        return assertion;
    }

    @Override
    public Assertion addTests(Assertion assertion, Collection<Test> tests){
        return addTests(assertion, entityService.getIhtsdo(), tests);
    }

    @Override
    public Assertion deleteTest(Assertion assertion, ReleaseCenter releaseCenter, Test test){

        // see if matching assertion test already exists
        AssertionTest assertionTest = assertionDao.getAssertionTests(assertion, releaseCenter, test);
        if(assertionTest != null){
            entityService.delete(assertionTest);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTest(Assertion assertion, Test test){
        return addTest(assertion, entityService.getIhtsdo(), test);
    }

    @Override
    public Assertion deleteTests(Assertion assertion, ReleaseCenter releaseCenter, Collection<Test> tests){
        for(Test test : tests)
        {
            deleteTest(assertion, releaseCenter, test);
        }

        return assertion;
    }

    @Override
    public Assertion deleteTests(Assertion assertion, Collection<Test> tests){
        return deleteTests(assertion, entityService.getIhtsdo(), tests);
    }
}
