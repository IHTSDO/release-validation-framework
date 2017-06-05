package org.ihtsdo.rvf.validation;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StructuralTestRunItem {

	private final String executionId;
	private final Date testTime;
	private final String fileName;
	private final String filePath;
	private final String columnName;
	private final String testType;
	private final String testPattern;
	private final boolean failure;
	private String actualValue;
	private String expectedValue;
	private Long lineNr;

	public StructuralTestRunItem(String executionId, Date testTime, String fileName, String filePath, String columnName,
			String testType, String testPattern, boolean failure, String actualValue, String expectedValue) {
		this.executionId = executionId;
		this.testTime = testTime;
		this.fileName = fileName;
		this.filePath = filePath;
		this.columnName = columnName;
		this.testType = testType;
		this.testPattern = testPattern;
		this.failure = failure;
		this.actualValue = actualValue;
		this.expectedValue = expectedValue;
	}
	
	public StructuralTestRunItem(String executionId, Date testTime, String fileName, String filePath, String columnName,
			String testType, String testPattern, boolean failure, String actualValue, String expectedValue, Long lineNr) {
		this.executionId = executionId;
		this.testTime = testTime;
		this.fileName = fileName;
		this.filePath = filePath;
		this.columnName = columnName;
		this.testType = testType;
		this.testPattern = testPattern;
		this.failure = failure;
		this.actualValue = actualValue;
		this.expectedValue = expectedValue;
		this.lineNr = lineNr;
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

	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getColumnName() {
		return columnName;
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

	public String getActualValue() {
		return actualValue;
	}

	public void setActualValue(String actualValue) {
		this.actualValue = actualValue;
	}

	public String getFailureMessage() {
		return failure ? "Failed" : "Success";
	}

	public String getActualExpectedValue() {
		return failure ? "expected '" + expectedValue + "' but got '" + actualValue + "'" : "";
	}

	public String getExpectedValue() {
		return expectedValue;
	}

	public void setExpectedValue(String expectedValue) {
		this.expectedValue = expectedValue;
	}

	public Long getLineNr() {
		return lineNr;
	}

	public void setLineNr(Long lineNr) {
		this.lineNr = lineNr;
	}

	@Override
	public String toString() {
		return "TestRunItem{" +
				"executionId='" + executionId + '\'' +
				", columnName='" + columnName + '\'' +
				", testPattern='" + testPattern + '\'' +
				", failure=" + (failure ? "Fail" : "Pass") +
				", actualValue='" + actualValue + '\'' +
				", expectedValue='" + expectedValue + '\'' +
				'}';
	}

}
