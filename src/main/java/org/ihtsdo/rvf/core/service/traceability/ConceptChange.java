package org.ihtsdo.rvf.core.service.traceability;

import java.util.Set;

public class ConceptChange {

	private String conceptId;

	private Set<ComponentChange> componentChanges;

	public String getConceptId() {
		return conceptId;
	}

	public Set<ComponentChange> getComponentChanges() {
		return componentChanges;
	}
}
