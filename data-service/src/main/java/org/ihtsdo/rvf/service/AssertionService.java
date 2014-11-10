package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AssertionService extends EntityService<Assertion> {

	Assertion create(Map<String, String> properties);

	Assertion update(Long id, Map<String, String> newValues);

	List<Assertion> findAll();

	Assertion find(Long id);

    List<AssertionTest> getAssertionTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<AssertionTest> getAssertionTests(Long assertionId, Long releaseCenterId);

    List<Test> getTests(Assertion assertion, ReleaseCenter releaseCenter);

    List<Test> getTests(Long assertionId, Long releaseCenterId);

    List<AssertionTest> getAssertionTests(Assertion assertion);

    List<AssertionTest> getAssertionTests(Long assertionId);

    List<Test> getTests(Assertion assertion);

    List<Test> getTests(Long assertionId);

    Assertion addTest(Assertion assertion, ReleaseCenter releaseCenter, Test test);

    Assertion addTest(Assertion assertion, Test test);

    Assertion addTests(Assertion assertion, ReleaseCenter releaseCenter, Collection<Test> tests);

    Assertion addTests(Assertion assertion, Collection<Test> tests);

    Assertion deleteTest(Assertion assertion, ReleaseCenter releaseCenter, Test test);

    Assertion deleteTest(Assertion assertion, Test test);

    Assertion deleteTests(Assertion assertion, ReleaseCenter releaseCenter, Collection<Test> tests);

    Assertion deleteTests(Assertion assertion, Collection<Test> tests);
}
