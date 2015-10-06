package org.ihtsdo.rvf.entity;

public class FailureDetail {

	Long conceptId;
	String detail;
	
	public FailureDetail(long conceptId, String detail) {
		if (conceptId > 0) {
			this.conceptId = new Long (conceptId);
		}
		this.detail = detail;
	}
	public Long getConceptId() {
		return conceptId;
	}
	public void setConceptId(Long conceptId) {
		this.conceptId = conceptId;
	}
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
}
