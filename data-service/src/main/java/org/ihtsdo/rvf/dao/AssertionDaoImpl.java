package org.ihtsdo.rvf.dao;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;

import java.util.List;

public class AssertionDaoImpl extends EntityDaoImpl<Assertion> implements AssertionDao {

	public AssertionDaoImpl() {
		super(Assertion.class);
	}

	@Override
	public List<Assertion> findAll() {
		return getCurrentSession()
				.createQuery("from Assertion assertion order by assertion.name")
				.list();
	}

    @Override
    public List<AssertionTest> getAssertionTests(Long assertionId, Long releaseCenterId) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId")
                .setParameter("assertionId", assertionId)
                .setParameter("releaseCenterId", releaseCenterId).list();
    }

    @Override
    public AssertionTest getAssertionTests(Long assertionId, Long releaseCenterId, Long testId) {
        List<AssertionTest> list = getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId and at.test.id = :testId")
                .setParameter("assertionId", assertionId)
                .setParameter("testId", testId)
                .setParameter("releaseCenterId", releaseCenterId).list();

        return validateAndGetFirstEntry(list);
    }

    @Override
    public List<AssertionTest> getAssertionTests(Long assertionId) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertionId).list();
    }

    @Override
    public List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId")
                .setParameter("assertionId", assertion.getId())
                .setParameter("releaseCenterId", releaseCenter.getId()).list();
    }

    @Override
    public AssertionTest getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter, Test test) {
        List<AssertionTest> list = getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId and at.test.id = :testId")
                .setParameter("assertionId", assertion.getId())
                .setParameter("testId", test.getId())
                .setParameter("releaseCenterId", releaseCenter.getId()).list();

        return validateAndGetFirstEntry(list);
    }

    @Override
    public List<AssertionTest> getAssertionTests(Assertion assertion) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertion.getId()).list();
    }

    @Override
    public List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion = :assertion and at.center = :releaseCenter")
                .setParameter("assertion", assertion)
                .setParameter("releaseCenter", releaseCenter).list();
    }

    @Override
    public List<Test> getTests(Assertion assertion) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion = :assertion")
                .setParameter("assertion", assertion).list();
    }

    @Override
    public List<Test> getTests(Long assertionid, Long releaseCenterId) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId")
                .setParameter("assertionId", assertionid)
                .setParameter("releaseCenterId", releaseCenterId).list();
    }

    @Override
    public List<Test> getTests(Long assertionid) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertionid).list();
    }

    AssertionTest validateAndGetFirstEntry(List<AssertionTest> list){
        if(list.size() == 1){
            return list.get(0);
        }
        else if(list.size() > 1){
            System.out.println("Found more than one matching AssertionTest objects. This is possibly wrong...");
            return list.get(0);
        }
        else{
            return null;
        }
    }
}
