package org.ihtsdo.rvf.entity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ihtsdo.rvf.helper.Configuration;

/**
 * A class that records metrics about execution of an {@link org.ihtsdo.rvf.entity.Test}.
 */
public class TestRunItem {

	private Long assertionId;
	private String assertionText;
	private String executionId;
	private Date testTime;
	private Configuration configuration;
	private String testType;
	private String testPattern;
	private boolean failure = true;
	private long runTime;
	private String failureMessage;
	private List<String> firstNInstances = new ArrayList<>();
	private Long failureCount;

	/**
	 * Empty constructor for IOC
	 */
	public TestRunItem() {

	}

	public TestRunItem(final Assertion assertion, final String executionId, final Date testTime, final Configuration configuration,
					   final String testType, final String testPattern, final boolean failure, final long runTime, final String failureMessage) {
		assertionId = assertion.getId();
		this.assertionText = assertion.getName();
		this.executionId = executionId;
		this.testTime = testTime;
		this.configuration = configuration;
		this.testType = testType;
		this.testPattern = testPattern;
		this.failure = failure;
		this.runTime = runTime;
		this.failureMessage = failureMessage;
	}

	public boolean isFailure() {
		return failure;
	}

	private String check(final String separator, final String filePath) {
		return filePath != null ? (separator + filePath) : "";
	}

	public String getExecutionId() {
		return executionId;
	}

	public Date getTestTime() {
		return testTime;
	}

	public String getTestType() {
		return testType;
	}

	public String getTestPattern() {
		return testPattern;
	}

	public String getStartDate() {
		return new SimpleDateFormat().format(testTime);
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	@Override
	public String toString() {
		return "TestRunItem{" +
				"assertionId=" + assertionId + +  '\'' +
				"assertionText=" + assertionText +  '\'' +	
				"executionId='" + executionId + '\'' +
				", testType='" + testType + '\'' +
				", testPattern='" + testPattern + '\'' +
				", failure=" + (failure ? "Fail" : "Pass") +
				", testTime='" + testTime + '\'' +
				", runTime='" + runTime + '\'' +
				'}';
	}

	public void setExecutionId(final String executionId) {
		this.executionId = executionId;
	}

	public void setTestTime(final Date testTime) {
		this.testTime = testTime;
	}

	public void setTestType(final String testType) {
		this.testType = testType;
	}

	public void setTestPattern(final String testPattern) {
		this.testPattern = testPattern;
	}

	public void setFailure(final boolean failure) {
		this.failure = failure;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(final long runTime) {
		this.runTime = runTime;
	}

	public void setFailureMessage(final String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final Configuration configuration) {
		this.configuration = configuration;
	}

	public List<String> getFirstNInstances() {
		return firstNInstances;
	}

	public void setFirstNInstances(final List<String> firstNInstances) {
		this.firstNInstances = firstNInstances;
	}

	public void addFirstNInstance(final String string){
		getFirstNInstances().add(string);
	}

	public Long getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(final Long failureCount) {
		this.failureCount = failureCount;
	}

	public String getAssertionText() {
		return assertionText;
	}

	public void setAssertionText(final String assertionText) {
		this.assertionText = assertionText;
	}
}
