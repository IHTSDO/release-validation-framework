package org.ihtsdo.rvf.dao;

import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.springframework.stereotype.Service;

@Service
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
    public AssertionTest getAssertionTests(final Long assertionId, final Long testId) {
        final List<AssertionTest> list = getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.test.id = :testId")
                .setParameter("assertionId", assertionId)
                .setParameter("testId", testId).list();

        return validateAndGetFirstEntry(list);
    }

    @Override
    public AssertionTest getAssertionTests(final UUID uuid, final Long testId) {
        final List<AssertionTest> list = getCurrentSession().createQuery("from AssertionTest as at where at.assertion.uuid = :assertionId and at.test.id = :testId")
                .setParameter("assertionId", uuid)
                .setParameter("testId", testId).list();

        return validateAndGetFirstEntry(list);
    }

    @Override
    public List<AssertionTest> getAssertionTests(final Long assertionId) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertionId).list();
    }

    @Override
    public List<AssertionTest> getAssertionTests(final UUID uuid) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.uuid = :assertionId")
                .setParameter("assertionId", uuid).list();
    }

    @Override
    public AssertionTest getAssertionTests(final Assertion assertion, final Test test) {
        final List<AssertionTest> list = getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId and at.test.id = :testId")
                .setParameter("assertionId", assertion.getId())
                .setParameter("testId", test.getId()).list();

        return validateAndGetFirstEntry(list);
    }

    @Override
    public List<AssertionTest> getAssertionTests(final Assertion assertion) {
        return getCurrentSession().createQuery("from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertion.getId()).list();
    }

    @Override
    public List<Test> getTests(final Assertion assertion) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion = :assertion")
                .setParameter("assertion", assertion).list();
    }

    @Override
    public List<Test> getTests(final Long assertionid) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion.id = :assertionId")
                .setParameter("assertionId", assertionid).list();
    }

    @Override
    public List<Test> getTests(final UUID uuid) {
        return getCurrentSession().createQuery("select at.test from AssertionTest as at where at.assertion.uuid = :assertionId")
                .setParameter("assertionId", uuid).list();
    }

    @Override
    public List<AssertionGroup> getGroupsForAssertion(final Assertion assertion) {
//        return getCurrentSession().createQuery("from AssertionGroup as g where g.assertions.id in :assertion")
//                .setParameter("assertion", assertion).list();
        return getCurrentSession().createQuery("select g from AssertionGroup g inner join g.assertions a where a.id = :assertionId")
                .setParameter("assertionId", assertion.getId()).list();
    }

    @Override
    public List<AssertionGroup> getGroupsForAssertion(final Long assertionId) {
//        return getCurrentSession().createQuery("from AssertionGroup as g where g.assertions.id in :assertionId")
//                .setParameter("assertionId", assertionId).list();
        return getCurrentSession().createQuery("select g from AssertionGroup g inner join g.assertions a where a.id = :assertionId")
                .setParameter("assertionId", assertionId).list();
    }

    @Override
    public List<Assertion> getAssertionsForGroup(final AssertionGroup group) {
        return getCurrentSession().createQuery("select g.assertions from AssertionGroup as g where g.id = :groupId")
                .setParameter("groupId", group.getId()).list();
    }

    @Override
    public List<Assertion> getAssertionsForGroup(final Long groupId) {
        return getCurrentSession().createQuery("select g.assertions from AssertionGroup as g where g.id = :groupId")
                .setParameter("groupId", groupId).list();
    }

    AssertionTest validateAndGetFirstEntry(final List<AssertionTest> list){
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

	@Override
	public List<Assertion> getAssertionsByKeywords(final String keyName) {
		return getCurrentSession()
				.createQuery("from Assertion assertion where assertion.keywords = :keyWord order by assertion.id")
				.setParameter("keyWord", keyName).list();
	}
}
