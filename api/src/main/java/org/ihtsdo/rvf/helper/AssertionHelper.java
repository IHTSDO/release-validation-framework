package org.ihtsdo.rvf.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.service.AssertionService;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssertionHelper {
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	public Map<String, Object> assertAssertions (final Collection<Assertion> assertions, final Long runId,  final String prospectiveReleaseVersion,
			final String previousReleaseVersion) {
		//TODO throw an exception that results in a malformed request response and remove runtime dependency on JUnit
		Assert.assertNotNull(runId);
		final Collection<TestRunItem> allTestRunItems = new ArrayList<>();
		final Map<String , Object> responseMap = new HashMap<>();
		int failedAssertionCount = 0;
		for (final Assertion assertion: assertions) {
			try
			{
				final List<TestRunItem> items = new ArrayList<>(assertionExecutionService.executeAssertion(assertion, runId,
						prospectiveReleaseVersion, previousReleaseVersion));
				for (final TestRunItem item : items) {
					if(item.isFailure()){
						failedAssertionCount++;
					}
					allTestRunItems.add(item);
				}

			}
			catch (final MissingEntityException e) {
				failedAssertionCount++;
			}
		}

		responseMap.put("assertions", allTestRunItems);
		responseMap.put("assertionsRun", allTestRunItems.size());
		responseMap.put("assertionsFailed", failedAssertionCount);

		return responseMap;
	}
}
