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
	static AssertionService assertionService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	public Map<String, Object> assertAssertions (Collection<Assertion> assertions, Long runId,  String prospectiveReleaseVersion,
			String previousReleaseVersion) {
		Assert.assertNotNull(runId);
		Collection<TestRunItem> allTestRunItems = new ArrayList<>();
		Map<String , Object> responseMap = new HashMap<>();
		int failedAssertionCount = 0;
		for (Assertion assertion: assertions) {
			try
			{
				List<TestRunItem> items = new ArrayList<>(assertionExecutionService.executeAssertion(assertion, runId,
						prospectiveReleaseVersion, previousReleaseVersion));
				for (TestRunItem item : items) {
					if(item.isFailure()){
						failedAssertionCount++;
					}
					allTestRunItems.add(item);
				}

			}
			catch (MissingEntityException e) {
				failedAssertionCount++;
			}
		}

		responseMap.put("assertions", allTestRunItems);
		responseMap.put("assertionsRun", allTestRunItems.size());
		responseMap.put("assertionsFailed", failedAssertionCount);

		return responseMap;
	}
}
