package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ValidationRunConfig {
	
	@JsonIgnore
	private MultipartFile file;
	
	private File prospectiveFile;
	
	private boolean writeSucceses;
	
	@JsonIgnore
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
}
