package org.ihtsdo.rvf.core.service.structure.validation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamTestReport implements TestReportable {

	public static final String LINE_ENDING = "\r\n";

	private final PrintWriter writer;
	private ResultFormatter formatter;
	private final AtomicInteger numFailures = new AtomicInteger(0);
	private final AtomicInteger numTestRuns = new AtomicInteger(0);
	private boolean writeSuccesses;
	private final ConcurrentHashMap<String, TestRunItemCount> errorMap = new ConcurrentHashMap<>();
	private final List<StructuralTestRunItem> failedItems = Collections.synchronizedList(new ArrayList<>());

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
		numFailures.getAndIncrement();
		numTestRuns.getAndIncrement();
	}

	@Override
	public void addSuccess( String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern) {
		if (writeSuccesses) {
			StructuralTestRunItem item = new StructuralTestRunItem(executionId, testTime, fileName, filePath, columnName, testType, testPattern, false, null, null);
			String row = formatter.formatRow(item, 0);
			writer.write(row);
		}
		numTestRuns.getAndIncrement();
	}

	@Override
	public int getNumErrors() {
		return numFailures.get();
	}

	@Override
	public int getNumberRecordedErrors() {
		return errorMap.size();
	}

	@Override
	public int getNumSuccesses() {
		return numTestRuns.get() - numFailures.get();
	}

	@Override
	public int getNumTestRuns() {
		return numTestRuns.get();
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
