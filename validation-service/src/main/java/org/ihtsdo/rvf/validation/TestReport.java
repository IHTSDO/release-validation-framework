package org.ihtsdo.rvf.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class TestReport implements Serializable {

    private List<TestRunItem> failures = new ArrayList<>();
    private List<TestRunItem> testRuns = new ArrayList<>();
    private ResultFormatter formatter;

    public TestReport(ResultFormatter formatter) {
        this.formatter = formatter;
    }

    public String getResult() {
        return formatter.formatResults(failures, testRuns);
    }

    public void addError(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String columnValue) {
        TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, true, columnValue);
        failures.add(item);
        testRuns.add(item);
    }

    public void addSuccess(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern) {
        TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, false, null);
        testRuns.add(item);
    }

    public int getErrorCount() {
        return failures.size();
    }

    public int getNumSuccesses() {
        return testRuns.size() - failures.size();
    }

    public int getNumTestRuns() {
        return testRuns.size();
    }

}
