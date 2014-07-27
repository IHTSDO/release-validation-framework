package org.ihtsdo.rvf.validation;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 *
 */
public class CsvResultFormatter implements ResultFormatter {

    @Override
    public String formatResults(List<TestRunItem> failures, List<TestRunItem> testRuns) {
        StringBuilder output = new StringBuilder();

        output.append(headers).append(String.format("%n"));
        for (TestRunItem ti : testRuns) {
            //  output pass/fail, id,
            output.append(formatRow(ti));
        }
        return output.toString();
    }

    public String formatRow(TestRunItem ti) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"%n",
                ti.getFailureMessage(),
                ti.getExecutionId(),
                ti.getStartDate(), ti.getFileName(), ti.getFilePath(), ti.getColumnName(), ti.getTestType(),
                ti.getTestPattern(), escape(ti.getActualExpectedValue()));
    }

    private String escape(String actualExpectedValue) {
        if (actualExpectedValue.contains(",")) {
            return StringUtils.replace(actualExpectedValue, ",", " ");
        }
        return actualExpectedValue;
    }

    public String getHeaders() {
        return headers;
    }

    // no spaces between the commas please as this breaks the , quote escaping
    private static final String headers = "Result,Line-Column,Execution Start,File Name,File Path,Column Name,Test Type,Test Pattern,Failure Details";
}
