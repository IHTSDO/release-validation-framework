package org.ihtsdo.rvf.validation.impl;

import org.ihtsdo.rvf.validation.ResultFormatter;
import org.ihtsdo.rvf.validation.TestRunItem;

import java.util.List;

public class CsvResultFormatter implements ResultFormatter {

	// no spaces between the commas please as this breaks the , quote escaping
	private static final String headers = "Result\tRow-Column\tFile Name\tFile Path\tColumn Name\tTest Type\tTest Pattern\tFailure Details\tNumber of occurences";

	@Override
	public String formatResults(List<TestRunItem> testRuns) {
		StringBuilder output = new StringBuilder();

		output.append(headers).append("\n");
		for (TestRunItem ti : testRuns) {
			//  output pass/fail, id,
			output.append(formatRow(ti, 0));
		}
		return output.toString();
	}

	public String formatRow(TestRunItem ti, Integer itemErrorCount) {
		return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d\n",
				ti.getFailureMessage(),
				ti.getExecutionId(),
				ti.getFileName(), ti.getFilePath(), ti.getColumnName(), ti.getTestType(),
				ti.getTestPattern(), ti.getActualExpectedValue(), itemErrorCount);
	}

	public String getHeaders() {
		return headers;
	}

}
