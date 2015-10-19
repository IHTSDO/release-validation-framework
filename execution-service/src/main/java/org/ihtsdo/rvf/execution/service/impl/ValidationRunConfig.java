package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ValidationRunConfig {
	private String testFileName;
	private Long runId;
	private transient MultipartFile file;
	private transient File prospectiveFile;
	private String prospectiveFilePath;
	private boolean writeSucceses;
	private transient MultipartFile manifestFile;
	private List<String> groupsList;
	private String prevIntReleaseVersion;
	private String previousExtVersion;
	private String extensionDependency;
	private String storageLocation;
	private String url;
	private Integer failureExportMax;
	private boolean firstTimeRelease;
	private String manifestFileFullPath;
	
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
	public String getPrevIntReleaseVersion() {
		return prevIntReleaseVersion;
	}
	public ValidationRunConfig addPrevIntReleaseVersion(final String prevIntReleaseVersion) {
		this.prevIntReleaseVersion = prevIntReleaseVersion;
		return this;
	}
	public String getPreviousExtVersion() {
		return previousExtVersion;
	}
	public ValidationRunConfig addPreviousExtVersion(final String previousExtVersion) {
		this.previousExtVersion = previousExtVersion;
		return this;
	}
	public String getExtensionDependencyVersion() {
		return extensionDependency;
	}
	public ValidationRunConfig addExtensionDependencyVersion(final String extensionDependency) {
		this.extensionDependency = extensionDependency;
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
	public File getProspectiveFile() {
		return prospectiveFile;
	}
	public void setProspectiveFile(final File prospectiveFile) {
		this.prospectiveFile = prospectiveFile;
		this.prospectiveFilePath = prospectiveFile.getAbsolutePath();
	}
	
	public String getProspectiveFilePath() {
		return prospectiveFilePath;
	}
	public void setProspectiveFilePath(String prospectiveFilePath) {
		this.prospectiveFilePath = prospectiveFilePath;
	}
	public void setTestFileName(final String filename) {
		testFileName = filename;
		
	}
	
	public String getTestFileName() {
		return testFileName;
	}
	
	public boolean getFirstTimeRelease() {
		return firstTimeRelease;
	}
	public void setFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
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
	public void setPrevIntReleaseVersion(String prevIntReleaseVersion) {
		this.prevIntReleaseVersion = prevIntReleaseVersion;
	}
	public void setPreviousExtVersion(String previousExtVersion) {
		this.previousExtVersion = previousExtVersion;
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
				+ ", writeSucceses=" + writeSucceses + ", groupsList="
				+ groupsList + ", prevIntReleaseVersion="
				+ prevIntReleaseVersion + ", previousExtVersion="
				+ previousExtVersion + ", extensionDependency="
				+ extensionDependency + ", runId=" + runId + ", url=" + url + "]";
	}
	public ValidationRunConfig addFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
		return this;
	}
	public String getManifestFileFullPath() {
		return this.manifestFileFullPath;
	}
	public void setManifestFileFullPath(String manifestFileFullPath) {
		this.manifestFileFullPath = manifestFileFullPath;
	}
	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}
}
