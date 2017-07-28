package org.ihtsdo.rvf.entity;


public class FailureDetail implements Comparable<FailureDetail>{

	String conceptId;
	String detail;
	String errorDescription;
	
	public FailureDetail(String conceptId, String detail, String errorDescription) {
		this.conceptId = new String (conceptId);
		this.detail = detail;
		this.errorDescription = errorDescription;

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
		return "FailureDetail [conceptId=" + conceptId + ", detail=" + detail
				+ ", Error Description="+ errorDescription + "]";
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
}
