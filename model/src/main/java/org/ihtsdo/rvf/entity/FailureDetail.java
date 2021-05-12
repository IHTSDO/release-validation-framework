package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class FailureDetail implements Comparable<FailureDetail>{

	String conceptId;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String conceptFsn;
	String detail;
	String componentId;

	@JsonIgnore
	String tableName;

	public FailureDetail(String conceptId, String detail) {
		this.conceptId = new String (conceptId);
		this.detail = detail;
	}

	public FailureDetail(String conceptId, String detail, String conceptFsn) {
		this.conceptId = conceptId;
		this.detail = detail;
		this.conceptFsn = conceptFsn;
	}

	public FailureDetail(String conceptId, String detail, String componentId, String tableName) {
		this.conceptId = new String (conceptId);
		this.detail = detail;
		this.componentId = componentId;
		this.tableName = tableName;
	}

	public String getConceptId() {
		return conceptId;
	}
	
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
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
			if (other.detail != null)
				return false;
		} else if (!detail.equals(other.detail))
			return false;
		return true;
	}

	@Override
	public int compareTo(FailureDetail other) {
		return ((Integer)hashCode()).compareTo((Integer)other.hashCode());
	}

	public String getConceptFsn() {
		return conceptFsn;
	}

	public void setConceptFsn(String conceptFsn) {
		this.conceptFsn = conceptFsn;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}
}
