package org.ihtsdo.rvf.core.service.config;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlExecutionConfig {

	private String prospectiveVersion;
	private String previousVersion;
	private String extensionDependencyVersion;
	private final Long executionId;
	private List<String> groupNames;
	private List<String> assertionExclusionList;
	private String defaultModuleId;
	private List<String> includedModules;
	private List<String> excludedRF2Files;
	private int failureExportMax = 10;
	private boolean firstTimeRelease;
	private boolean standAloneProduct;
	private boolean extensionValidation;
	private boolean isRf2DeltaOnly;
	private boolean releaseAsAnEdition;
	private String effectiveTime;
	private String previousEffectiveTime;
	private List<File> localReleaseFiles;
	private Map<String, String> currentDependencyToSchemeMap;
	private Map<String, String> currentDependencyToPreviousEffectiveTimeMap;

	public MysqlExecutionConfig(final Long runId) {
		this(runId,false);
	}

	public MysqlExecutionConfig(Long runId, boolean firstTimeRelease) {
		this.executionId = runId;
		this.firstTimeRelease = firstTimeRelease;
	}

	public void setStandAloneProduct(boolean standAloneProduct) {
		this.standAloneProduct = standAloneProduct;
	}

	public boolean isStandAloneProduct() {
		return standAloneProduct;
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

	public void setAssertionExclusionList(List<String> assertionExclusionList) {
		this.assertionExclusionList = assertionExclusionList;
	}

	public List<String> getAssertionExclusionList() {
		return assertionExclusionList;
	}

	public void setExcludedRF2Files(List<String> excludedRF2Files) {
		this.excludedRF2Files = excludedRF2Files;
	}

	public List<String> getExcludedRF2Files() {
		return excludedRF2Files;
	}

	public List<String> getIncludedModules() {
		return includedModules;
	}

	public void setDefaultModuleId(String defaultModuleId) {
		this.defaultModuleId = defaultModuleId;
	}

	public String getDefaultModuleId() {
		return defaultModuleId;
	}

	public void setIncludedModules(List<String> includedModules) {
		this.includedModules = includedModules;
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
	
	public void setRf2DeltaOnly(boolean isReleaseValidation) {
		this.isRf2DeltaOnly = isReleaseValidation;
	}

	public boolean isRf2DeltaOnly() {
		return this.isRf2DeltaOnly;
	}

	public void setReleaseAsAnEdition(boolean releaseAsAnEdition) {
		this.releaseAsAnEdition = releaseAsAnEdition;
	}

	public boolean isReleaseAsAnEdition() {
		return releaseAsAnEdition;
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

	public String getPreviousEffectiveTime() {
		return previousEffectiveTime;
	}

	public void setPreviousEffectiveTime(String previousEffectiveTime) {
		this.previousEffectiveTime = previousEffectiveTime;
	}

	public void setLocalReleaseFiles(List<File> localReleaseFiles) {
		this.localReleaseFiles = localReleaseFiles;
	}

	public List<File> getLocalReleaseFiles() {
		return localReleaseFiles;
	}

	public void addCurrentDependencyRelease(String releaseFilename, String scheme) {
		if (this.currentDependencyToSchemeMap == null) {
			this.currentDependencyToSchemeMap = new HashMap<>();
		}
		this.currentDependencyToSchemeMap.put(releaseFilename, scheme);
	}

	public Map<String, String> getCurrentDependencyToSchemeMap() {
		return currentDependencyToSchemeMap;
	}

	public void addCurrentDependencyToPreviousEffectiveTime(String releaseFilename, String effectiveTime) {
		if (this.currentDependencyToPreviousEffectiveTimeMap == null) {
			this.currentDependencyToPreviousEffectiveTimeMap = new HashMap<>();
		}
		this.currentDependencyToPreviousEffectiveTimeMap.put(releaseFilename, effectiveTime);
	}
	public Map<String, String> getCurrentDependencyToPreviousEffectiveTimeMap() {
		return currentDependencyToPreviousEffectiveTimeMap;
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
				.append(", isRf2DeltaOnly=").append(isRf2DeltaOnly).append(", ");
		if (includedModules != null)
			builder.append("includedModules=").append(includedModules).append(", ");
		if (extensionDependencyVersion != null)
			builder.append("extensionDependencyVersion=").append(extensionDependencyVersion).append(", ");
		if (effectiveTime != null)
			builder.append("effectiveTime=").append(effectiveTime).append(", ");
		builder.append("]");
		return builder.toString();
	}
}
