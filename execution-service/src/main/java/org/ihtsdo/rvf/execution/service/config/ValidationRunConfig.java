package org.ihtsdo.rvf.execution.service.config;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ValidationRunConfig {
	private String testFileName;
	private Long runId;
	private transient MultipartFile file;
	private boolean writeSucceses;
	private transient MultipartFile manifestFile;
	private List<String> groupsList;
	private String previousRelease;
	private String dependencyRelease;
	private String storageLocation;
	private String url;
	private Integer failureExportMax;
	private String manifestFileFullPath;
	private String prospectiveFileFullPath;
	private boolean isProspectiveFileInS3;
	private transient File localProspectiveFile;
	private transient File localManifestFile;
	private boolean isRf2DeltaOnly;
	private boolean enableDrools;
	private String effectiveTime;
	private boolean releaseAsAnEdition;
	private String includedModules;
	private List<String> droolsRulesGroupList;
	private String bucketName;
	
	public MultipartFile getFile() {
		return file;
	}
	public ValidationRunConfig addFile(final MultipartFile file) {
		this.file = file;
		return this;
	}
	public boolean isWriteSucceses() {
		return writeSucceses;
	}
	public ValidationRunConfig addWriteSucceses(final boolean writeSucceses) {
		this.writeSucceses = writeSucceses;
		return this;
	}
	public MultipartFile getManifestFile() {
		return manifestFile;
	}
	public ValidationRunConfig addManifestFile(final MultipartFile manifestFile) {
		this.manifestFile = manifestFile;
		return this;
	}
	public List<String> getGroupsList() {
		return groupsList;
	}
	public ValidationRunConfig addGroupsList(final List<String> groupsList) {
		this.groupsList = groupsList;
		return this;
	}
	public ValidationRunConfig addDroolsRulesGroupList(final List<String> droolsRulesGroupList) {
		this.droolsRulesGroupList = droolsRulesGroupList;
		return this;
	}
	
	public ValidationRunConfig addFailureExportMax(final Integer exportMax) {
		this.failureExportMax = exportMax;
		return this;
	}
	public Integer getFailureExportMax() {
		return failureExportMax;
	}
	public void setFailureExportMax(final Integer failureExportMax) {
		this.failureExportMax = failureExportMax;
	}
	public Long getRunId() {
		return runId;
	}
	public ValidationRunConfig addRunId(final Long runId) {
		this.runId = runId;
		return this;
	}
	public String getStorageLocation() {
		return storageLocation;
	}
	public ValidationRunConfig addStorageLocation(final String storageLocation) {
		this.storageLocation = storageLocation;
		return this;
	}
	public String getUrl() {
		return url;
	}
	public ValidationRunConfig addUrl(final String url) {
		this.url = url;
		return this;
	}
	
	public void setTestFileName(final String filename) {
		testFileName = filename;
		
	}
	
	public String getTestFileName() {
		return testFileName;
	}
	
	public void setRunId(Long runId) {
		this.runId = runId;
	}
	public void setWriteSucceses(boolean writeSucceses) {
		this.writeSucceses = writeSucceses;
	}
	public void setGroupsList(List<String> groupsList) {
		this.groupsList = groupsList;
	}

	public void setStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	@Override
	public String toString() {
		return "ValidationRunConfig [testFileName=" + testFileName
				+ ", writeSucceses=" + writeSucceses
				+ ", groupsList=" + groupsList
				+ ", previousRelease=" + previousRelease
				+ ", dependencyRelease=" + dependencyRelease
				+ ", drools=" + enableDrools
				+ ", runId=" + runId 
				+ ", url=" + url + "]";
	}
	
	public String getManifestFileFullPath() {
		return this.manifestFileFullPath;
	}
	public void setManifestFileFullPath(String manifestFileFullPath) {
		this.manifestFileFullPath = manifestFileFullPath;
	}
	
	public boolean isFirstTimeRelease() {
		return previousRelease == null ? true : false;
	}
	
	public ValidationRunConfig addProspectiveFileFullPath(String s3File) {
		this.prospectiveFileFullPath = s3File;
		String[] parts = s3File.split("/");
		testFileName =  parts[parts.length-1];
		return this;
	}
	
	public  ValidationRunConfig addManifestFileFullPath(String manifestFileFullPath) {
		this.manifestFileFullPath = manifestFileFullPath;
		return this;
	}
	
	public String getProspectiveFileFullPath() {
		return this.prospectiveFileFullPath;
	}
	
	public boolean isProspectiveFileInS3() {
		return isProspectiveFileInS3;
	}
	public void setProspectiveFilesInS3(boolean isProspectiveFilesInS3) {
		this.isProspectiveFileInS3 = isProspectiveFilesInS3;
	}
	
	public ValidationRunConfig addProspectiveFilesInS3(boolean isFileInS3) {
		this.isProspectiveFileInS3 = isFileInS3;
		return this;
	}

	public ValidationRunConfig addRF2DeltaOnly(boolean isRf2DeltaOnly) {
		this.isRf2DeltaOnly = isRf2DeltaOnly;
		return this;
	}
	
	public boolean isRf2DeltaOnly() {
		return isRf2DeltaOnly;
	}
	public void setRf2DeltaOnly(boolean isRf2DeltaOnly) {
		this.isRf2DeltaOnly = isRf2DeltaOnly;
	}
	public String getExtensionDependency() {
		return dependencyRelease;
	}
	public void setExtensionDependency(String extensionDependency) {
		this.dependencyRelease = extensionDependency;
	}

	public boolean isEnableDrools() {
		return enableDrools;
	}

	public ValidationRunConfig setEnableDrools(boolean enableDrools) {
		this.enableDrools = enableDrools;
		return this;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public ValidationRunConfig setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
		return this;
	}


	public boolean isReleaseAsAnEdition() {
		return releaseAsAnEdition;
	}

	public ValidationRunConfig setReleaseAsAnEdition(boolean releaseAsAnEdition) {
		this.releaseAsAnEdition = releaseAsAnEdition;
		return this;
	}

	public String getIncludedModules() {
		return includedModules;
	}

	public ValidationRunConfig setIncludedModules(String includedModules) {
		this.includedModules = includedModules;
		return this;
	}

	public List<String> getDroolsRulesGroupList() {
		return droolsRulesGroupList;
	}

	public void setDroolsRulesGroupList(List<String> droolsRulesGroupList) {
		this.droolsRulesGroupList = droolsRulesGroupList;
	}
	
	public ValidationRunConfig addPreviousRelease(String previousRelease) {
		if (previousRelease != null && !previousRelease.isEmpty()) {
			this.previousRelease = previousRelease;
		}
		return this;
	}
	
	public ValidationRunConfig addDependencyRelease(String dependencyRelease) {
		this.dependencyRelease = dependencyRelease;
		return this;
	}
	public void setPreviousRelease(String previousRelease) {
		this.previousRelease = previousRelease;
	}
	
	public void setProspectiveFileFullPath(String prospectiveFileFullPath) {
		this.prospectiveFileFullPath = prospectiveFileFullPath;
	}
	
	public String getPreviousRelease() {
		return this.previousRelease;
	}
	
	public File getLocalProspectiveFile() {
		return this.localProspectiveFile;
	}
	public void setLocalProspectiveFile(File prospectiveFile) {
		this.localProspectiveFile = prospectiveFile;
	}
	public File getLocalManifestFile() {
		return localManifestFile;
	}
	public void setLocalManifestFile(File localManifestFile) {
		this.localManifestFile = localManifestFile;
	}
	
	public ValidationRunConfig addBucketName(String bucketName) {
		this.bucketName = bucketName;
		return this;
	}
	
	public String getBucketName() {
		return this.bucketName;
	}
	
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
		
	}
}
