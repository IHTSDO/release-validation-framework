package org.ihtsdo.rvf.execution.service.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A class that records metrics about execution of an {@link org.ihtsdo.rvf.entity.Test}.
 */
public class TestRunItem {

    private String executionId;
    private Date testTime;
    private String fileName;
    private String filePath;
    private String columnName;
    private String testType;
    private String testPattern;
    private boolean failure = true;
    private String actualValue;
    private String expectedValue;
    private long runTime;

    /**
     * Empty constructor for IOC
     */
    public TestRunItem() {

    }

    public TestRunItem(String executionId, Date testTime, String fileName, String filePath, String columnName,
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

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setTestTime(Date testTime) {
        this.testTime = testTime;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
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
}
