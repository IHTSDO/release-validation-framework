package org.ihtsdo.rvf.execution.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"runId", "releaseFileS3Path", "manifestFileS3Path", "previousRelease", 
	"dependencyRelease", "storageLocation", "mysqlValidationRequest", "droolsRulesValidationRequest"})
public class ValidationRequest {
	private Long runId;
	private String releaseFileS3Path;
	private String manifestFileS3Path;
	private String previousRelease;
	private String dependencyRelease;
	private String storageLocation;
	private RVFMysqlValidationRequest mysqlValidationRequest;
	private DroolsRulesValidationRequest droolsRulesValidationRequest;
	
	public DroolsRulesValidationRequest getDroolsRulesValidationRequest() {
		return this.droolsRulesValidationRequest;
	}

	public String getReleaseFileS3Path() {
		return this.releaseFileS3Path;
	}

	public String getManifestFileS3Path() {
		return this.manifestFileS3Path;
	}

	public String getPreviousRelease() {
		return this.previousRelease;
	}

	public String getDependencyRelease() {
		return this.dependencyRelease;
	}

	public Long getRunId() {
		return this.runId;
	}

	public String getStorageLocation() {
		return this.storageLocation;
	}

	public RVFMysqlValidationRequest getMysqlValidationRequest() {
		return mysqlValidationRequest;
	}

	public void setMysqlValidationRequest(RVFMysqlValidationRequest mysqlValidationRequest) {
		this.mysqlValidationRequest = mysqlValidationRequest;
	}

	public void setDroolsRulesValidationRequest(DroolsRulesValidationRequest droolsRulesValidationRequest) {
		this.droolsRulesValidationRequest = droolsRulesValidationRequest;
	}

	public void setReleaseFileS3Path(String releaseFileS3Path) {
		this.releaseFileS3Path = releaseFileS3Path;
	}

	public void setManifestFileS3Path(String manifestFileS3Path) {
		this.manifestFileS3Path = manifestFileS3Path;
	}

	public void setPreviousRelease(String previousRelease) {
		this.previousRelease = previousRelease;
	}

	public void setDependencyRelease(String dependencyRelease) {
		this.dependencyRelease = dependencyRelease;
	}

	public void setRunId(Long runId) {
		this.runId = runId;
	}

	public void setStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
	}
}
