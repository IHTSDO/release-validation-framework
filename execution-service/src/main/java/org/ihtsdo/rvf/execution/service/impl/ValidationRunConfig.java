package org.ihtsdo.rvf.execution.service.impl;

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
	private String prevIntReleaseVersion;
	private String previousExtVersion;
	private String extensionDependency;
	private String storageLocation;
	private String url;
	private Integer failureExportMax;
	private String manifestFileFullPath;
	private String s3ExecutionBucketName;
	private String prospectiveFileFullPath;
	private boolean isProspectiveFilesInS3;
	private transient File localProspectiveFile;
	private transient File localManifestFile;
	private String s3PublishBucketName;
	private boolean isRf2DeltaOnly;
	private boolean jiraIssueCreationFlag;
	private String productName;
	private String reportingStage;
	
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
	public ValidationRunConfig addExtensionDependencyVersion(final String extensionDependency) {
		this.extensionDependency = extensionDependency;
		return this;
	}
	public ValidationRunConfig addFailureExportMax(final Integer exportMax) {
		this.failureExportMax = exportMax;
		return this;
	}
	
	public ValidationRunConfig addJiraIssueCreationFlag(final boolean jiraIssueCreationFlag) {
		this.jiraIssueCreationFlag = jiraIssueCreationFlag;
		return this;
	}

	public ValidationRunConfig addProductName(final String productName) {
		this.productName = productName;
		return this;
	}

	public ValidationRunConfig addReportingStage(final String reportingStage) {
		this.reportingStage = reportingStage;
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
	
	public String getManifestFileFullPath() {
		return this.manifestFileFullPath;
	}
	public void setManifestFileFullPath(String manifestFileFullPath) {
		this.manifestFileFullPath = manifestFileFullPath;
	}
	public boolean isFirstTimeRelease() {
		if (prevIntReleaseVersion == null && previousExtVersion == null) {
			return true;
		}
        return prevIntReleaseVersion != null && prevIntReleaseVersion.trim().isEmpty()
                && previousExtVersion != null && previousExtVersion.trim().isEmpty();

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
	public void setS3ExecutionBucketName(String bucketName) {
		s3ExecutionBucketName = bucketName;
	}
	
	public void setProspectiveFileFullPath(String targetFilePath) {
		this.prospectiveFileFullPath = targetFilePath;
	}
	public String getS3ExecutionBucketName() {
		return s3ExecutionBucketName;
	}
	public String getProspectiveFileFullPath() {
		return this.prospectiveFileFullPath;
	}
	
	public boolean isProspectiveFilesInS3() {
		return isProspectiveFilesInS3;
	}
	public void setProspectiveFilesInS3(boolean isProspectiveFilesInS3) {
		this.isProspectiveFilesInS3 = isProspectiveFilesInS3;
	}
	public void setLocalProspectiveFile(File localProspectiveFile) {
		this.localProspectiveFile = localProspectiveFile;
	}
	public File getLocalProspectiveFile() {
		return this.localProspectiveFile;
	}
	public void setLocalManifestFile(File manifestLocalFile) {
		this.localManifestFile = manifestLocalFile;
	}
	public File getLocalManifestFile() {
		return this.localManifestFile;
	}
	
	public ValidationRunConfig addProspectiveFilesInS3(boolean isFileInS3) {
		this.isProspectiveFilesInS3 = isFileInS3;
		return this;
	}

	public ValidationRunConfig addRF2DeltaOnly(boolean isRf2DeltaOnly) {
		this.isRf2DeltaOnly = isRf2DeltaOnly;
		return this;
	}
	public String getS3PublishBucketName() {
		return s3PublishBucketName;
	}
	public void setS3PublishBucketName(String s3PublishBucketName) {
		this.s3PublishBucketName = s3PublishBucketName;
	}
	
	public boolean isRf2DeltaOnly() {
		return isRf2DeltaOnly;
	}
	public void setRf2DeltaOnly(boolean isRf2DeltaOnly) {
		this.isRf2DeltaOnly = isRf2DeltaOnly;
	}
	public String getExtensionDependency() {
		return extensionDependency;
	}
	public void setExtensionDependency(String extensionDependency) {
		this.extensionDependency = extensionDependency;
	}
}
