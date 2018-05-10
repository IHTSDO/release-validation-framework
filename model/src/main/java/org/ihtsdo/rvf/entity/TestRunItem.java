package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class that records metrics about execution of an {@link org.ihtsdo.rvf.entity.Test}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunItem implements Comparable<TestRunItem>{

	private String testCategory;
	private TestType testType;
	private UUID assertionUuid;
	private String assertionText;
	private String severity;
	private String executionId;
	private Long queryInMilliSeconds;
	private Long failureCount;
	private String failureMessage;
	private List<FailureDetail> firstNInstances;
	private Long extractResultInMillis;
	private String jiraLink;

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

	public TestType getTestType() {
		return testType;
	}

	public void setTestType(TestType testType) {
		this.testType = testType;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	@Override
	public String toString() {
		return "TestRunItem{" +
				"assertionUuid=" + assertionUuid +  '\'' +
				"assertionText=" + assertionText +  '\'' +	
				"executionId=" + executionId + '\'' +
				"testCategory=" + testCategory + '\'' +
				"testType=" + testType + '\'' +
				"runTime=" + queryInMilliSeconds + '\'' +
				"failureCount=" + failureCount +
				'}';
	}

	public void setExecutionId(final String executionId) {
		this.executionId = executionId;
	}

	public void setTestCategory(final String testType) {
		this.testCategory = testType;
	}

	public Long getRunTime() {
		return queryInMilliSeconds;
	}

	public void setRunTime(final long runTime) {
		this.queryInMilliSeconds = runTime;
	}

	public void setFailureMessage(final String failureMessage) {
		this.failureMessage = failureMessage;
	}

	
	public List<FailureDetail> getFirstNInstances() {
		return firstNInstances;
	}

	public void setFirstNInstances(final List<FailureDetail> firstNInstances) {
		this.firstNInstances = firstNInstances;
	}

	public void addFirstNInstance(final FailureDetail failureInstance){
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

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public UUID getAssertionUuid() {
		return assertionUuid;
	}

	public void setAssertionUuid(final UUID assertionUuid) {
		this.assertionUuid = assertionUuid;
	}

	public Long getExtractResultInMillis() {
		return extractResultInMillis;
	}

	public void setExtractResultInMillis(Long extractResultInMillis) {
		this.extractResultInMillis = extractResultInMillis;
	}

	public String getJiraLink() {
		return jiraLink;
	}

	public void setJiraLink(String jiraLink) {
		this.jiraLink = jiraLink;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((assertionText == null) ? 0 : assertionText.hashCode());
		result = prime * result
				+ ((assertionUuid == null) ? 0 : assertionUuid.hashCode());
		result = prime * result
				+ ((executionId == null) ? 0 : executionId.hashCode());
		result = prime
				* result
				+ ((extractResultInMillis == null) ? 0 : extractResultInMillis
						.hashCode());
		result = prime * result
				+ ((failureCount == null) ? 0 : failureCount.hashCode());
		result = prime * result
				+ ((failureMessage == null) ? 0 : failureMessage.hashCode());
		result = prime * result
				+ ((firstNInstances == null) ? 0 : firstNInstances.hashCode());
		result = prime
				* result
				+ ((queryInMilliSeconds == null) ? 0 : queryInMilliSeconds
						.hashCode());
		result = prime * result
				+ ((testCategory == null) ? 0 : testCategory.hashCode());
		result = prime * result
				+ ((testType == null) ? 0 : testType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestRunItem other = (TestRunItem) obj;
		if (assertionText == null) {
			if (other.assertionText != null)
				return false;
		} else if (!assertionText.equals(other.assertionText))
			return false;
		if (assertionUuid == null) {
			if (other.assertionUuid != null)
				return false;
		} else if (!assertionUuid.equals(other.assertionUuid))
			return false;
		if (executionId == null) {
			if (other.executionId != null)
				return false;
		} else if (!executionId.equals(other.executionId))
			return false;
		if (extractResultInMillis == null) {
			if (other.extractResultInMillis != null)
				return false;
		} else if (!extractResultInMillis.equals(other.extractResultInMillis))
			return false;
		if (failureCount == null) {
			if (other.failureCount != null)
				return false;
		} else if (!failureCount.equals(other.failureCount))
			return false;
		if (failureMessage == null) {
			if (other.failureMessage != null)
				return false;
		} else if (!failureMessage.equals(other.failureMessage))
			return false;
		if (firstNInstances == null) {
			if (other.firstNInstances != null)
				return false;
		} else if (!firstNInstances.equals(other.firstNInstances))
			return false;
		if (queryInMilliSeconds == null) {
			if (other.queryInMilliSeconds != null)
				return false;
		} else if (!queryInMilliSeconds.equals(other.queryInMilliSeconds))
			return false;
		if (testCategory == null) {
			if (other.testCategory != null)
				return false;
		} else if (!testCategory.equals(other.testCategory))
			return false;
		if (testType == null) {
			if (other.testType != null)
				return false;
		} else if (!testType.equals(other.testType))
			return false;
		return true;
	}

	@Override
	public int compareTo(TestRunItem other) {
		if(other.assertionUuid == null || this.assertionUuid == null) {
			return 0;
		}
		return this.assertionUuid.compareTo(other.getAssertionUuid());
	}
	
}
