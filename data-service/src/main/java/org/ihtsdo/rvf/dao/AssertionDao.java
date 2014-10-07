package org.ihtsdo.rvf.dao;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;

import java.util.List;

public interface AssertionDao extends EntityDao<Assertion> {

	List<Assertion> findAll();

    List<AssertionTest> getAssertionTests(Long assertionId, Long releaseCenterId);

    List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<Test> getTests(Long assertionid, Long releaseCenterId);
}
