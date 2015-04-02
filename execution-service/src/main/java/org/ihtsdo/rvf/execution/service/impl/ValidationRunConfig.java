package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ValidationRunConfig {
	private String testFileName;
	private Long runId;
	private transient MultipartFile file;
	private transient File prospectiveFile;
	private boolean writeSucceses;
	private transient MultipartFile manifestFile;
	private List<String> groupsList;
	private String prevIntReleaseVersion;
	private String previousExtVersion;
	private String extensionBaseLine;
	private String storageLocation;
	private String url;
	private Integer failureExportMax;
	
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
	public String getExtensionBaseLine() {
		return extensionBaseLine;
	}
	public ValidationRunConfig addExtensionBaseLine(final String extensionBaseLine) {
		this.extensionBaseLine = extensionBaseLine;
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
	}

	public void setTestFileName(final String filename) {
		testFileName = filename;
		
	}
	
	public String getTestFileName() {
		return testFileName;
	}
	
	@Override
	public String toString() {
		return "ValidationRunConfig [testFileName=" + testFileName
				+ ", writeSucceses=" + writeSucceses + ", groupsList="
				+ groupsList + ", prevIntReleaseVersion="
				+ prevIntReleaseVersion + ", previousExtVersion="
				+ previousExtVersion + ", extensionBaseLine="
				+ extensionBaseLine + ", runId=" + runId + ", url=" + url + "]";
	}
}
