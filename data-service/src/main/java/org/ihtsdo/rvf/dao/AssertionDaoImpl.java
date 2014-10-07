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
    public List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId")
                .setParameter("assertionId", assertion.getId())
                .setParameter("releaseCenterId", releaseCenter.getId()).list();
    }

    @Override
    public List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion = :assertion and at.center = :releaseCenter")
                .setParameter("assertion", assertion)
                .setParameter("releaseCenter", releaseCenter).list();
    }

    @Override
    public List<Test> getTests(Long assertionid, Long releaseCenterId) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion.id = :assertionId and at.center.id = :releaseCenterId")
                .setParameter("assertionId", assertionid)
                .setParameter("releaseCenterId", releaseCenterId).list();
    }
}
