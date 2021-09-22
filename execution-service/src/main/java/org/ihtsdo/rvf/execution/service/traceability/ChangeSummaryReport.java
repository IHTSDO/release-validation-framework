package org.ihtsdo.rvf.execution.service.traceability;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangeSummaryReport {

	private Map<ComponentType, Set<String>> componentChanges;
	private List<Activity> changesNotAtTaskLevel;

	public Map<ComponentType, Set<String>> getComponentChanges() {
		return componentChanges;
	}

	public List<Activity> getChangesNotAtTaskLevel() {
		return changesNotAtTaskLevel;
	}
}
