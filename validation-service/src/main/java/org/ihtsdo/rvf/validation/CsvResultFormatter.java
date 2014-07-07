package org.ihtsdo.rvf.validation;

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
            // todo color code
            output.append(String.format("%s, %s, %s, %s, %s, %s, %s, %s, %d, %s%n",
					ti.isFailure() ? "fail" : "pass",
					ti.getExecutionId(),
                    ti.getStartDate(), ti.getFileName(), ti.getFilePath(), ti.getColumnName(), ti.getTestType(),
                    ti.getTestPattern(), failures.size(), "need to get failure examples"));
        }
        return output.toString();
    }

    private static final String headers = "testResult, lineNumber-columnNumber, testExecutionStartDate, fileName, filePath, columnName, testType, testPattern, failureCount, failed Value";
}
