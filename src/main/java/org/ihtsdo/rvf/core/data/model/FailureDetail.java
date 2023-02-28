package org.ihtsdo.rvf.core.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class FailureDetail implements Comparable<FailureDetail>{

	private  String conceptId;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String conceptFsn;
	private String detail;
	private String componentId;
	private String moduleId;
	private String fullComponent;

	private transient String tableName;

	public FailureDetail(){
	}

	public FailureDetail(String conceptId, String detail) {
		this.conceptId = conceptId;
		this.detail = detail;
	}

	public FailureDetail(String conceptId, String detail, String conceptFsn) {
		this(conceptId, detail);
		this.conceptFsn = conceptFsn;
	}

	public FailureDetail(String conceptId, String detail, String componentId, String tableName) {
		this(conceptId, detail);
		this.componentId = componentId;
		this.tableName = tableName;
	}

	public String getConceptId() {
		return conceptId;
	}
	
	public String getDetail() {
		return detail;
	}

	public FailureDetail setDetail(String detail) {
		this.detail = detail;
		return this;
	}

	@Override
	public String toString() {
		return "FailureDetail [conceptId=" + conceptId + ", detail=" + detail + ", componentId=" + componentId + ", tableName=" + tableName + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((detail == null) ? 0 : detail.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FailureDetail other = (FailureDetail) obj;
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
			return false;
		if (detail == null) {
            return other.detail == null;
		} else return detail.equals(other.detail);
    }

	@Override
	public int compareTo(FailureDetail other) {
		return ((Integer)hashCode()).compareTo(other.hashCode());
	}

	public String getConceptFsn() {
		return conceptFsn;
	}

	public FailureDetail setConceptFsn(String conceptFsn) {
		this.conceptFsn = conceptFsn;
		return this;
	}

	public FailureDetail setComponentId(String componentId) {
		this.componentId = componentId;
		return this;
	}

	public String getComponentId() {
		return componentId;
	}

	public FailureDetail setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public FailureDetail setFullComponent(String fullComponent) {
		this.fullComponent = fullComponent;
		return this;
	}

	public String getFullComponent() {
		return fullComponent;
	}

	public FailureDetail setModuleId(String moduleId) {
		this.moduleId = moduleId;
		return this;
	}

	public String getModuleId() {
		return moduleId;
	}
}
