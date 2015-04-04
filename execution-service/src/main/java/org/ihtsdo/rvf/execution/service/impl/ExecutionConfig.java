package org.ihtsdo.rvf.execution.service.impl;

import java.util.List;

public class ExecutionConfig {

	private String prospectiveVersion;
	private String previousVersion;
	private Long executionId;
	private List<String> groupNames;
	private int failureExportMax = -1;

	public ExecutionConfig(final Long runId) {
		executionId = runId;
	}

	public void setProspectiveVersion(final String prospectiveVersion) {
		this.prospectiveVersion = prospectiveVersion;
	}

	public void setPreviousVersion(final String prevReleaseVersion) {
		this.previousVersion = prevReleaseVersion;
		
	}

	public void setExecutionId(final Long runId) {
		executionId = runId;
	}

	public void setGroupNames(final List<String> groupsList) {
		groupNames = groupsList;
	}

	public List<String> getGroupNames() {
		return groupNames;
	}

	public Long getExecutionId() {
		return executionId;
	}

	public String getProspectiveVersion() {
		return prospectiveVersion;
	}

	public String getPreviousVersion() {
		return previousVersion;
	}

	public int getFailureExportMax() {
		return failureExportMax;
	}

	public void setFailureExportMax(final int max) {
		failureExportMax = max;
	}
}
