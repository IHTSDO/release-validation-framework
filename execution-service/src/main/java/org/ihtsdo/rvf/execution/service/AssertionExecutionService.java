package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;

import java.util.Collection;

/**
 * An interface specification for service that is capable of transforming {@link org.ihtsdo.rvf.entity.Assertion}
 * into the corresponding {@link org.ihtsdo.rvf.entity.Test} and collecting the execution results.
 */
public interface AssertionExecutionService {

    TestRunItem executeAssertionTest(AssertionTest assertionTest, Long executionId);

    Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions, Long executionId);

    Collection<TestRunItem> executeAssertion(Assertion assertion, Long executionId);

    Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, Long executionId);

    TestRunItem executeTest(Test test, Long executionId);

    Collection<TestRunItem> executeTests(Collection<Test> tests, Long executionId);

    void setSchemaName(String schemaName);
}
