package org.ihtsdo.rvf.executionservice.traceability;

import java.util.Objects;

public class ComponentChange {

	private String componentId;

	private ChangeType changeType;

	private ComponentType componentType;

	private String componentSubType;

	private boolean effectiveTimeNull;

	public String getComponentId() {
		return componentId;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public String getComponentSubType() {
		return componentSubType;
	}

	public boolean isEffectiveTimeNull() {
		return effectiveTimeNull;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ComponentChange that = (ComponentChange) o;
		return effectiveTimeNull == that.effectiveTimeNull && componentId.equals(that.componentId) && changeType == that.changeType && componentType == that.componentType && Objects.equals(componentSubType, that.componentSubType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(componentId, changeType, componentType, componentSubType, effectiveTimeNull);
	}

	@Override
	public String toString() {
		return "ComponentChange{" +
				"componentId='" + componentId + '\'' +
				", changeType=" + changeType +
				", componentType=" + componentType +
				", componentSubType='" + componentSubType + '\'' +
				", effectiveTimeNull=" + effectiveTimeNull +
				'}';
	}
}
