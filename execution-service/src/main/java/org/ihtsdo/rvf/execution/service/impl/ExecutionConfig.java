package org.ihtsdo.rvf.execution.service.impl;

import java.util.List;

public class ExecutionConfig {

	private String prospectiveVersion;
	private String previousVersion;
	private Long executionId;
	private List<String> groupNames;
	private int failureExportMax = 10;
	private boolean firstTimeRelease;
	private boolean extensionValidation;
	private boolean isReleaseValidation;
	private String extensionDependencyVersion;
	private String releaseDate;
	private boolean jiraIssueCreationFlag;
	private String productName;
	private String reportingStage;

	public ExecutionConfig(final Long runId) {
		this(runId,false);
	}

	public ExecutionConfig(Long runId, boolean firstTimeRelease) {
		executionId = runId;
		this.firstTimeRelease = firstTimeRelease;
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

	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}

	public void setFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
	}

	public void setExtensionValidation(boolean isExtension) {
		this.extensionValidation = isExtension;
	}
	
	public boolean isExtensionValidation() {
		return this.extensionValidation;
	}
	
	public void setReleaseValidation(boolean isReleaseValidation) {
		this.isReleaseValidation = isReleaseValidation;
	}

	public boolean isReleaseValidation() {
		return this.isReleaseValidation;
	}

	public String getExtensionDependencyVersion() {
		return extensionDependencyVersion;
	}

	public void setExtensionDependencyVersion(String extensionDependencyVersion) {
		this.extensionDependencyVersion = extensionDependencyVersion;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public boolean isJiraIssueCreationFlag() {
		return jiraIssueCreationFlag;
	}

	public void setJiraIssueCreationFlag(boolean jiraIssueCreationFlag) {
		this.jiraIssueCreationFlag = jiraIssueCreationFlag;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getReportingStage() {
		return reportingStage;
	}

	public void setReportingStage(String reportingStage) {
		this.reportingStage = reportingStage;
	}

	
	
}
