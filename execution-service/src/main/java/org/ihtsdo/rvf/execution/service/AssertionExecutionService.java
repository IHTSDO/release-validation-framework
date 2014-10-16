package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;

import java.util.Collection;

/**
 * An interface specification for service that is capable of transforming {@link org.ihtsdo.rvf.entity.Assertion}
 * into the corresponding {@link org.ihtsdo.rvf.entity.Test} and collecting the execution results.
 */
public interface AssertionExecutionService {

    TestRunItem executeAssertionTest(AssertionTest assertionTest, long executionId);

    Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions, long executionId);

    Collection<TestRunItem> executeAssertion(Assertion assertion, ReleaseCenter releaseCenter, long executionId);

    Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, ReleaseCenter releaseCenter, long executionId);

    TestRunItem executeTest(Test test, ReleaseCenter releaseCenter, long executionId);

    Collection<TestRunItem> executeTests(Collection<Test> tests, ReleaseCenter releaseCenter, long executionId);
}
