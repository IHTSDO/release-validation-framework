package org.ihtsdo.rvf.validation.impl;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ihtsdo.rvf.validation.ResultFormatter;
import org.ihtsdo.rvf.validation.StructuralTestRunItem;
import org.ihtsdo.rvf.validation.TestReportable;

public class StreamTestReport implements TestReportable {

	public static final String LINE_ENDING = "\r\n";

	private final PrintWriter writer;
	private ResultFormatter formatter;
	private int numFailures = 0;
	private int numTestRuns = 0;
	private boolean writeSuccesses;
	private ConcurrentHashMap<String, TestRunItemCount> errorMap = new ConcurrentHashMap<>();
	private List<StructuralTestRunItem> failedItems = Collections.synchronizedList(new ArrayList<StructuralTestRunItem>());

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
		this.writer.write(LINE_ENDING);
		this.writeSuccesses = writeSucceses;
	}

	@Override
	public String getResult() {
		for (Map.Entry<String, TestRunItemCount> entry : errorMap.entrySet()) {
			TestRunItemCount value = entry.getValue();
			writer.write(formatter.formatRow(value.getItem(), value.getErrorCount()));
		}
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

	@Override
	public void addError( String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String actualValue, String expectedValue, Long lineNr) {
		StructuralTestRunItem item = new StructuralTestRunItem (executionId, testTime, fileName, filePath, columnName, testType, testPattern, true, actualValue, expectedValue, lineNr);
		failedItems.add(item);
		if (errorMap.containsKey(columnName)) {
			TestRunItemCount errorCounter = errorMap.get(columnName);
			errorCounter.addError();
		} else {
			errorMap.put(columnName, new TestRunItemCount(item));
		}
		numFailures++;
		numTestRuns++;
	}

	@Override
	public void addSuccess( String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern) {
		if (writeSuccesses) {
			StructuralTestRunItem item = new StructuralTestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, false, null, null);
			String row = formatter.formatRow(item, 0);
			writer.write(row);
		}
		numTestRuns++;
	}

	@Override
	public int getNumErrors() {
		return numFailures;
	}

	@Override
	public int getNumberRecordedErrors() {
		return errorMap.size();
	}

	@Override
	public int getNumSuccesses() {
		return numTestRuns - numFailures;
	}

	@Override
	public int getNumTestRuns() {
		return numTestRuns;
	}

	public void setFormatter(ResultFormatter formatter) {
		this.formatter = formatter;
	}

	public void setWriteSuccesses(boolean writeSuccesses) {
		this.writeSuccesses = writeSuccesses;
	}

	@Override
	public List<StructuralTestRunItem> getFailedItems() {
		return failedItems;
	}

}
