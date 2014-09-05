package org.ihtsdo.rvf.validation;

import java.util.List;

public class CsvMetadataResultFormatter implements ResultFormatter {

	// no spaces between the commas please as this breaks the , quote escaping
	private static final String headers = "Result\tRow-Column\tFile Name\tTest Type\tFailure Details";

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
		return String.format("%s\t%s\t%s\t%s\t%s\n",
				ti.getFailureMessage(),
				ti.getExecutionId(),
				ti.getFileName(), ti.getTestType(), ti.getActualExpectedValue());
	}

	public String getHeaders() {
		return headers;
	}

}
