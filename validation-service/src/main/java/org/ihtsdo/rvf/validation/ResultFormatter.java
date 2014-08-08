package org.ihtsdo.rvf.validation;

import java.util.List;

/**
 *
 */
public interface ResultFormatter {

    String formatResults(List<TestRunItem> results);
    String formatRow(TestRunItem testRunItem, Integer itemErrorCount);
    String getHeaders();
}
