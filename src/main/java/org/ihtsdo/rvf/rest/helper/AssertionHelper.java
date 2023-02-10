package org.ihtsdo.rvf.rest.helper;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.AssertionExecutionService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.util.*;

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
