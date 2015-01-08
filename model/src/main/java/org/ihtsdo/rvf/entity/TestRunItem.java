package org.ihtsdo.rvf.entity;

import org.ihtsdo.rvf.helper.Configuration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class that records metrics about execution of an {@link org.ihtsdo.rvf.entity.Test}.
 */
public class TestRunItem {

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

	public TestRunItem(Assertion assertion, String executionId, Date testTime, Configuration configuration,
					   String testType, String testPattern, boolean failure, long runTime, String failureMessage) {
		this.assertionText = assertion.toString();
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

	private String check(String separator, String filePath) {
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
				"executionId='" + executionId + '\'' +
				", testType='" + testType + '\'' +
				", testPattern='" + testPattern + '\'' +
				", failure=" + (failure ? "Fail" : "Pass") +
				", testTime='" + testTime + '\'' +
				", runTime='" + runTime + '\'' +
				'}';
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public void setTestTime(Date testTime) {
		this.testTime = testTime;
	}

	public void setTestType(String testType) {
		this.testType = testType;
	}

	public void setTestPattern(String testPattern) {
		this.testPattern = testPattern;
	}

	public void setFailure(boolean failure) {
		this.failure = failure;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public List<String> getFirstNInstances() {
		return firstNInstances;
	}

	public void setFirstNInstances(List<String> firstNInstances) {
		this.firstNInstances = firstNInstances;
	}

	public void addFirstNInstance(String string){
		getFirstNInstances().add(string);
	}

	public Long getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(Long failureCount) {
		this.failureCount = failureCount;
	}

	public String getAssertionText() {
		return assertionText;
	}

	public void setAssertionText(String assertionText) {
		this.assertionText = assertionText;
	}
}
