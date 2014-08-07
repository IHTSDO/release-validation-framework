package org.ihtsdo.rvf.validation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StreamTestReport implements TestReportable {

    private final PrintWriter writer;
    private ResultFormatter formatter;
    private int numFailures = 0;
    private int numTestRuns = 0;
    private boolean writeSuccesses;
    private Map<String, TestRunItemCount> errorMap = new HashMap<>();

    public StreamTestReport(ResultFormatter formatter, OutputStream outputStream, boolean writeSuccesses) {
        this.formatter = formatter;
        writer = new PrintWriter(outputStream);
        writer.write(formatter.getHeaders());
        this.writeSuccesses = writeSuccesses;

    }

    public StreamTestReport(ResultFormatter formatter, PrintWriter writer, boolean writeSucceses) {
        this.formatter = formatter;
        this.writer = writer;
        this.writer.write(formatter.getHeaders());
        this.writeSuccesses = writeSucceses;
    }

    @Override
    public String getResult() {
        for (Map.Entry<String, TestRunItemCount> entry : errorMap.entrySet()) {
            TestRunItemCount value = entry.getValue();
            writer.write(formatter.formatRow(value.getItem(), value.getErrorCount()));
        }
        writeSummary();
        return writer.toString();
    }

    @Override
    public String writeSummary() {
        String summary = "\n\nNumber of tests run: " + getNumTestRuns() + "\n" + "Total number of failures: " 
                + getNumErrors() + "\n" + "Total number of successes: " + getNumSuccesses() + "\n";
        writer.write(summary);
        return summary;
    }

    @Override
    public void addNewLine() {
        writer.write("/n/n");
    }

    public void addError(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String actualValue, String expectedValue) {
        TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, true, actualValue, expectedValue);
        if (writeSuccesses) {
            String row = formatter.formatRow(item, 0);
            writer.write(row);
        } else {
            if (errorMap.containsKey(columnName)) {
                TestRunItemCount errorCounter = errorMap.get(columnName);
                errorCounter.addError();
            } else {
                errorMap.put(columnName, new TestRunItemCount(item));
            }
        }
        numFailures++;
        numTestRuns++;
    }

    public void addSuccess(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern) {
        if (writeSuccesses) {
            TestRunItem item = new TestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, false, null, null);
            String row = formatter.formatRow(item, 0);
            writer.write(row);
        }
        numTestRuns++;
    }

    public int getNumErrors() {
        return numFailures;
    }

    public int getNumberRecordedErrors() {
        return errorMap.size();
    }

    public int getNumSuccesses() {
        return numTestRuns - numFailures;
    }

    public int getNumTestRuns() {
        return numTestRuns;
    }
    
    public void setFormatter(ResultFormatter formatter) {
        this.formatter = formatter;
    }

    public void setWriteSuccesses(boolean writeSuccesses) {
        this.writeSuccesses = writeSuccesses;
    }
}
