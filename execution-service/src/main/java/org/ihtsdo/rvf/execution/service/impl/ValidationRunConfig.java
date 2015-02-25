package org.ihtsdo.rvf.execution.service.impl;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ValidationRunConfig {
	
	private MultipartFile file;
	
	private boolean writeSucceses;
	
	private MultipartFile manifestFile;
	
	private List<String> groupsList;
	
	private String prevIntReleaseVersion;
	
	private String previousExtVersion;
	
	private String extensionBaseLine;
	
	private Long runId;
	
	private String storageLocation;
	
	private String url;
	
	public MultipartFile getFile() {
		return file;
	}
	public ValidationRunConfig addFile(MultipartFile file) {
		this.file = file;
		return this;
	}
	public boolean isWriteSucceses() {
		return writeSucceses;
	}
	public ValidationRunConfig addWriteSucceses(boolean writeSucceses) {
		this.writeSucceses = writeSucceses;
		return this;
	}
	public MultipartFile getManifestFile() {
		return manifestFile;
	}
	public ValidationRunConfig addManifestFile(MultipartFile manifestFile) {
		this.manifestFile = manifestFile;
		return this;
	}
	public List<String> getGroupsList() {
		return groupsList;
	}
	public ValidationRunConfig addGroupsList(List<String> groupsList) {
		this.groupsList = groupsList;
		return this;
	}
	public String getPrevIntReleaseVersion() {
		return prevIntReleaseVersion;
	}
	public ValidationRunConfig addPrevIntReleaseVersion(String prevIntReleaseVersion) {
		this.prevIntReleaseVersion = prevIntReleaseVersion;
		return this;
	}
	public String getPreviousExtVersion() {
		return previousExtVersion;
	}
	public ValidationRunConfig addPreviousExtVersion(String previousExtVersion) {
		this.previousExtVersion = previousExtVersion;
		return this;
	}
	public String getExtensionBaseLine() {
		return extensionBaseLine;
	}
	public ValidationRunConfig addExtensionBaseLine(String extensionBaseLine) {
		this.extensionBaseLine = extensionBaseLine;
		return this;
	}
	public Long getRunId() {
		return runId;
	}
	public ValidationRunConfig addRunId(Long runId) {
		this.runId = runId;
		return this;
	}
	public String getStorageLocation() {
		return storageLocation;
	}
	public ValidationRunConfig addStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
		return this;
	}
	public String getUrl() {
		return url;
	}
	public ValidationRunConfig addUrl(String url) {
		this.url = url;
		return this;
	}
}
