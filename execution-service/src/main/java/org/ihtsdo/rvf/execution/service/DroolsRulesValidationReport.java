package org.ihtsdo.rvf.execution.service;

import java.util.List;

import org.ihtsdo.rvf.entity.TestType;

public class DroolsRulesValidationReport {
	private TestType testType;
	private Long executionId;
	private long timeTakenInSeconds;
	private String reportUrl;
	private int totalTestsRun;
	private int totalSkips;
	private int totalFailures;
	private String ruleSetExecuted;
	private boolean isCompleted;
	private String message;

	private List<AssertionDroolRule> assertionsInvalidContent;

	public DroolsRulesValidationReport(TestType testType) {
		this.testType = testType;
	}

	public TestType getTestType() {
		return testType;
	}

	public void setTestType(TestType testType) {
		this.testType = testType;
	}

	public Long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(Long executionId) {
		this.executionId = executionId;
	}

	public long getTimeTakenInSeconds() {
		return timeTakenInSeconds;
	}

	public void setTimeTakenInSeconds(long timeTakenInSeconds) {
		this.timeTakenInSeconds = timeTakenInSeconds;
	}

	public String getReportUrl() {
		return reportUrl;
	}

	public void setReportUrl(String reportUrl) {
		this.reportUrl = reportUrl;
	}

	public int getTotalTestsRun() {
		return totalTestsRun;
	}

	public void setTotalTestsRun(int totalTestsRun) {
		this.totalTestsRun = totalTestsRun;
	}

	public int getTotalSkips() {
		return totalSkips;
	}

	public void setTotalSkips(int totalSkips) {
		this.totalSkips = totalSkips;
	}

	public int getTotalFailures() {
		return totalFailures;
	}

	public void setTotalFailures(int totalFailures) {
		this.totalFailures = totalFailures;
	}

	public List<AssertionDroolRule> getAssertionsInvalidContent() {
		return assertionsInvalidContent;
	}

	public void setAssertionsInvalidContent(List<AssertionDroolRule> assertionsInvalidContent) {
		this.assertionsInvalidContent = assertionsInvalidContent;
	}

	public String getRuleSetExecuted() {
		return ruleSetExecuted;
	}

	public void setRuleSetExecuted(String ruleSetExecuted) {
		this.ruleSetExecuted = ruleSetExecuted;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public void setCompleted(boolean completed) {
		isCompleted = completed;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
