package org.ihtsdo.rvf.core.data.model;

public enum SeverityLevel {
	WARN("WARNING"), 
	INFOR("INFOR"),
	ERROR("ERROR");

	private String severity;

	SeverityLevel(String severity) {
		this.severity = severity;
	}

	public boolean equalsName(String otherName) {
		return otherName != null && severity.equals(otherName);
	}

	@Override
	public String toString() {
	   return this.severity;
	}
}
