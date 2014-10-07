package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;

import java.util.List;
import java.util.Map;

public interface AssertionService extends EntityService<Assertion> {

	Assertion create(String name, Map<String, String> properties);

	Assertion update(Long id, Map<String, String> newValues);

	List<Assertion> findAll();

	Assertion find(Long id);

    List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<AssertionTest> getAssertionTests(Long assertionId, Long releaseCenterId);

    List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<Test> getTests(Long assertionId, Long releaseCenterId);
}
