package org.ihtsdo.rvf.validation;

import javax.swing.text.DateFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
*
*/
public class TestRunItem {

    public TestRunItem(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, boolean failure, String columnValue) {
        this.executionId = executionId;
        this.testTime = testTime;
        this.fileName = fileName;
        this.filePath = filePath;
        this.columnName = columnName;
        this.testType = testType;
        this.testPattern = testPattern;
        this.failure = failure;
        this.columnValue = columnValue;
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

    public String getColumnValue() {
        return columnValue;
    }

    private final String executionId;
    private final Date testTime;
    private final String fileName;
    private final String filePath;
    private final String columnName;
    private final String testType;
    private final String testPattern;
    private final boolean failure;
    private String columnValue;
}
