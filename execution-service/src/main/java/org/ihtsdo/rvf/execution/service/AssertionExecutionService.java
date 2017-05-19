package org.ihtsdo.rvf.execution.service;

import java.util.Collection;
import java.util.List;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;

/**
 * An interface specification for service that is capable of transforming {@link org.ihtsdo.rvf.entity.Assertion}
 * into the corresponding {@link org.ihtsdo.rvf.entity.Test} and collecting the execution results.
 */
public interface AssertionExecutionService {

	TestRunItem executeAssertionTest(AssertionTest assertionTest, ExecutionConfig config);

	Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions, ExecutionConfig config);

	Collection<TestRunItem> executeAssertion(Assertion assertion, ExecutionConfig config);

	Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, ExecutionConfig config);

	TestRunItem executeTest(Assertion assertion, Test test, ExecutionConfig config);

	Collection<TestRunItem> executeAssertionsConcurrently(List<Assertion> assertions, ExecutionConfig config);
	
	Collection<TestRunItem> executeECLValidationForMRCMRefset(ExecutionConfig config);

}
