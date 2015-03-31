package org.ihtsdo.rvf.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class that records metrics about execution of an {@link org.ihtsdo.rvf.entity.Test}.
 */
public class TestRunItem {

	private String testCategory;
	private UUID assertionUuid;
	private String assertionText;
	private String executionId;
	private long runTimeInMilliSeconds;
	private Long failureCount;
	private String failureMessage;
	private List<String> firstNInstances;


	/**
	 * Empty constructor for IOC
	 */
	public TestRunItem() {
		failureCount = -1L;
	}

	public String getExecutionId() {
		return executionId;
	}

	public String getTestCategory() {
		return testCategory;
	}


	public String getFailureMessage() {
		return failureMessage;
	}

	@Override
	public String toString() {
		return "TestRunItem{" +
				"assertionUuid=" + assertionUuid + +  '\'' +
				"assertionText=" + assertionText +  '\'' +	
				"executionId=" + executionId + '\'' +
				"testCategory=" + testCategory + '\'' +
				"runTime=" + runTimeInMilliSeconds + '\'' +
				"failureCount=" + failureCount +
				'}';
	}

	public void setExecutionId(final String executionId) {
		this.executionId = executionId;
	}

	public void setTestCategory(final String testType) {
		this.testCategory = testType;
	}

	public long getRunTime() {
		return runTimeInMilliSeconds;
	}

	public void setRunTime(final long runTime) {
		this.runTimeInMilliSeconds = runTime;
	}

	public void setFailureMessage(final String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public List<String> getFirstNInstances() {
		return firstNInstances;
	}

	public void setFirstNInstances(final List<String> firstNInstances) {
		this.firstNInstances = firstNInstances;
	}

	public void addFirstNInstance(final String failureInstance){
		if (firstNInstances == null) {
			firstNInstances = new ArrayList<>();
		}
		firstNInstances.add(failureInstance);
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

	public UUID getAssertionUuid() {
		return assertionUuid;
	}

	public void setAssertionUuid(final UUID assertionUuid) {
		this.assertionUuid = assertionUuid;
	}
}
