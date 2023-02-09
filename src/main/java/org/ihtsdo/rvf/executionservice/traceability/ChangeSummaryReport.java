package org.ihtsdo.rvf.executionservice.traceability;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangeSummaryReport {

	private Map<ComponentType, Set<String>> componentChanges;
	private List<Activity> changesNotAtTaskLevel;
	private Map<String, String> componentToConceptIdMap;

	public Map<ComponentType, Set<String>> getComponentChanges() {
		return componentChanges;
	}

	public List<Activity> getChangesNotAtTaskLevel() {
		return changesNotAtTaskLevel;
	}

	public Map<String, String> getComponentToConceptIdMap() {
		return componentToConceptIdMap;
	}
}
