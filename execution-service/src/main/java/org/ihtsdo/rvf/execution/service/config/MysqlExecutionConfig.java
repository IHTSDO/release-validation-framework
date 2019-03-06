package org.ihtsdo.rvf.execution.service.config;

import java.util.List;

public class MysqlExecutionConfig {

	private String prospectiveVersion;
	private String previousVersion;
	private Long executionId;
	private List<String> groupNames;
	private int failureExportMax = 10;
	private boolean firstTimeRelease;
	private boolean extensionValidation;
	private boolean isReleaseValidation;
	private String extensionDependencyVersion;
	private String effectiveTime;
	private String dependencyEffectiveTime;

	public MysqlExecutionConfig(final Long runId) {
		this(runId,false);
	}

	public MysqlExecutionConfig(Long runId, boolean firstTimeRelease) {
		this.executionId = runId;
		this.firstTimeRelease = firstTimeRelease;
	}

	public void setProspectiveVersion(final String prospectiveVersion) {
		this.prospectiveVersion = prospectiveVersion;
	}

	public void setPreviousVersion(final String prevReleaseVersion) {
		this.previousVersion = prevReleaseVersion;
		
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

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public String getDependencyEffectiveTime() {
		return dependencyEffectiveTime;
	}

	public void setDependencyEffectiveTime(String dependencyEffectiveTime) {
		this.dependencyEffectiveTime = dependencyEffectiveTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExecutionConfig [");
		if (prospectiveVersion != null)
			builder.append("prospectiveVersion=").append(prospectiveVersion).append(", ");
		if (previousVersion != null)
			builder.append("previousVersion=").append(previousVersion).append(", ");
		if (executionId != null)
			builder.append("executionId=").append(executionId).append(", ");
		if (groupNames != null)
			builder.append("groupNames=").append(groupNames).append(", ");
		builder.append("failureExportMax=").append(failureExportMax).append(", firstTimeRelease=")
				.append(firstTimeRelease).append(", extensionValidation=").append(extensionValidation)
				.append(", isReleaseValidation=").append(isReleaseValidation).append(", ");
		if (extensionDependencyVersion != null)
			builder.append("extensionDependencyVersion=").append(extensionDependencyVersion).append(", ");
		if (effectiveTime != null)
			builder.append("effectiveTime=").append(effectiveTime).append(", ");
		if (dependencyEffectiveTime != null)
			builder.append("dependencyEffectiveTime=").append(dependencyEffectiveTime);
		builder.append("]");
		return builder.toString();
	}
}
