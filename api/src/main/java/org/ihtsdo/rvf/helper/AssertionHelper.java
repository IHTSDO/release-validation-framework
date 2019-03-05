package org.ihtsdo.rvf.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssertionHelper {
	@Autowired
	private AssertionExecutionService assertionExecutionService;

	public Map<String, Object> assertAssertions(
			final Collection<Assertion> assertions, final MysqlExecutionConfig config) {
		final Collection<TestRunItem> allTestRunItems = new ArrayList<>();
		final Map<String, Object> responseMap = new LinkedHashMap<>();
		int failedAssertionCount = 0;
		for (final Assertion assertion : assertions) {
			try {
				final List<TestRunItem> items = new ArrayList<>(
						assertionExecutionService.executeAssertion(assertion, config));
				for (final TestRunItem item : items) {
					if (item.getFailureCount() != 0) {
						failedAssertionCount++;
					}
					item.setExecutionId(config.getExecutionId().toString());
					allTestRunItems.add(item);
				}
			} catch (final EntityNotFoundException e) {
				failedAssertionCount++;
			}
		}
		responseMap.put("assertionsRun", allTestRunItems.size());
		responseMap.put("assertionsFailed", failedAssertionCount);
		responseMap.put("assertions", allTestRunItems);

		return responseMap;
	}
}
