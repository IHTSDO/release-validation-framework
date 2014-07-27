package org.ihtsdo.rvf.validation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

/**
 *
 */
public class StreamTestReport implements TestReportable {

    private final PrintWriter writer;
    private ResultFormatter formatter;
    private int numFailures = 0;
    private int numTestRuns = 0;
    private boolean writeSuccesses = true;

    public StreamTestReport(ResultFormatter formatter, OutputStream outputStream, boolean writeSuccesses) {
        this.formatter = formatter;
        writer = new PrintWriter(outputStream);
        writer.write(formatter.getHeaders());
        this.writeSuccesses = writeSuccesses;
    }

    public StreamTestReport(ResultFormatter formatter, OutputStream outputStream) {
        this.formatter = formatter;
        writer = new PrintWriter(outputStream);
        writer.write(formatter.getHeaders());
    }

    @Override
    public String getResult() {
        return writer.toString();
    }

    public void addError(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String actualValue, String expectedValue) {
        TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, true, actualValue, expectedValue);
        writer.write(formatter.formatRow(item));
        numFailures++;
        numTestRuns++;
    }

    public void addSuccess(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern) {
        if (writeSuccesses) {
            TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, false, null, null);
            writer.write(formatter.formatRow(item));
        }
        numTestRuns++;
    }

    public int getNumErrors() {
        return numFailures;
    }

    public int getNumSuccesses() {
        return numTestRuns - numFailures;
    }

    public int getNumTestRuns() {
        return numTestRuns;
    }

}
