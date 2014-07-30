package org.ihtsdo.rvf.validation;

import java.util.List;

/**
 *
 */
public interface ResultFormatter {

    String formatResults(List<TestRunItem> failures, List<TestRunItem> testRuns);
    String formatRow(TestRunItem testRunItem);
    String getHeaders();
}
