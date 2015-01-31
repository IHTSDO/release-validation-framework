package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;

import java.util.Collection;

/**
 * An interface specification for service that is capable of transforming {@link org.ihtsdo.rvf.entity.Assertion}
 * into the corresponding {@link org.ihtsdo.rvf.entity.Test} and collecting the execution results.
 */
public interface AssertionExecutionService {

    TestRunItem executeAssertionTest(AssertionTest assertionTest, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion);

    Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion);

    Collection<TestRunItem> executeAssertion(Assertion assertion, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion);

    Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion);

    //Collection<TestRunItem> executeTests(Collection<Test> tests, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion);

	TestRunItem executeTest(Assertion assertion, Test test, Long executionId,
			String prospectiveReleaseVersion, String previousReleaseVersion);

}
