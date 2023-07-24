package org.ihtsdo.rvf.core.service.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;

import java.util.*;

public class ValidationStatusReport {
	private ValidationRunConfig validationConfig;
	private Map<String,String> reportSummary;
	private List<String> failureMessages;
	@SerializedName("TestResult")
	private ValidationReport resultReport;

	@JsonIgnore
	private Date startTime;

	@JsonIgnore
	private Date endTime;
	private int totalRF2FilesLoaded;

	@SerializedName("rf2Files")
	private List<String> rf2FilesLoaded;

	public ValidationStatusReport() {

	}

	public ValidationStatusReport(ValidationRunConfig validationConfig) {
		this.validationConfig = validationConfig;
		totalRF2FilesLoaded = -1;
		rf2FilesLoaded = new ArrayList<>();
		failureMessages = new ArrayList<>();
		reportSummary = new HashMap<>();
	}

	public ValidationReport getResultReport() {
		return resultReport;
	}

	@JsonProperty("TestResult")
	public void setResultReport(ValidationReport resultReport) {
		this.resultReport = resultReport;
	}

	public List<String> getFailureMessages() {
		return this.failureMessages;
	}

	public void addFailureMessage(String failureMessage) {
		this.failureMessages.add(failureMessage);
	}

	@JsonProperty
	public Date getStartTime() {
		return startTime;
	}

	@JsonIgnore
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@JsonProperty
	public Date getEndTime() {
		return endTime;
	}

	@JsonIgnore
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public ValidationRunConfig getValidationConfig() {
		return validationConfig;
	}

	public void setRF2Files(List<String> rf2FilesLoaded) {
		this.rf2FilesLoaded = rf2FilesLoaded;
	}

	public int getTotalRF2FilesLoaded() {
		if (totalRF2FilesLoaded == -1) {
			return rf2FilesLoaded.size();
		}
		return this.totalRF2FilesLoaded;
	}

	public void setTotalRF2FilesLoaded(int totalRF2FilesLoaded) {
		this.totalRF2FilesLoaded = totalRF2FilesLoaded;
	}

	public List<String> getRf2FilesLoaded() {
		return rf2FilesLoaded;
	}

	public void setRf2FilesLoaded(List<String> rf2FilesLoaded) {
		this.rf2FilesLoaded = rf2FilesLoaded;
	}

	public Map<String, String> getReportSummary() {
		return reportSummary;
	}

	public void setReportSummary(Map<String, String> reportSummary) {
		this.reportSummary = reportSummary;
	}
}
