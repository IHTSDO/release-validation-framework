package org.ihtsdo.rvf.validation;

import java.util.Date;

/**
 *
 */
public interface TestReportable {

    void addError(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String actualValue, String expectedValue);
    void addSuccess(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern);
    String getResult();
    int getNumErrors();
    int getNumSuccesses();
    int getNumTestRuns();
}
