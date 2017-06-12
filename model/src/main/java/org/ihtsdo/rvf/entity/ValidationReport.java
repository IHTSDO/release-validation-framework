package org.ihtsdo.rvf.entity;

import java.util.List;

public class ValidationReport {

	private TestType testType;
	private Long executionId;
	private Long timeTakenInSeconds;
	private String reportUrl;
	private Integer totalTestsRun;
	private Integer totalFailures;
	private Integer totalWarnings;
	private List<TestRunItem> assertionsFailed;
	private List<TestRunItem> assertionsWarning;
	private List<TestRunItem> assertionsPassed;

	public ValidationReport(final TestType testType) {
		this.testType = testType;
	}

	public void setExecutionId(final Long runId) {
		executionId = runId;
		
	}

	public void setTotalTestsRun(final int numTestRuns) {
		totalTestsRun = numTestRuns;
	}

	public void setTotalFailures(final int numErrors) {
		totalFailures = numErrors;
		
	}

	public void setReportUrl(final String url) {
		reportUrl = url;
	}

	public void setTimeTakenInSeconds(final long timeTaken) {
		timeTakenInSeconds = timeTaken;
	}

	public void setFailedAssertions(final List<TestRunItem> failedItems) {
		assertionsFailed = failedItems;
	}

	public void setPassedAssertions(final List<TestRunItem> passedItems) {
		assertionsPassed = passedItems;
	}

	public TestType getTestType() {
		return testType;
	}

	public void setTestType(final TestType testType) {
		this.testType = testType;
	}

	public List<TestRunItem> getAssertionsFailed() {
		return assertionsFailed;
	}

	public void setAssertionsFailed(final List<TestRunItem> assertionsFailed) {
		this.assertionsFailed = assertionsFailed;
	}

	public List<TestRunItem> getAssertionsPassed() {
		return assertionsPassed;
	}

	public void setAssertionsPassed(final List<TestRunItem> assertionsPassed) {
		this.assertionsPassed = assertionsPassed;
	}

	public Long getExecutionId() {
		return executionId;
	}

	public int getTotalTestsRun() {
		return totalTestsRun;
	}

	public int getTotalFailures() {
		return totalFailures;
	}

	public String getReportUrl() {
		return reportUrl;
	}

	public long getTimeTakenInSeconds() {
		return timeTakenInSeconds;
	}

	public int getTotalWarnings() {
		return totalWarnings;
	}

	public void setTotalWarnings(int totalWarnings) {
		this.totalWarnings = totalWarnings;
	}

	public List<TestRunItem> getAssertionsWarning() {
		return assertionsWarning;
	}

	public void setAssertionsWarning(List<TestRunItem> assertionsWarning) {
		this.assertionsWarning = assertionsWarning;
	}
	
}
