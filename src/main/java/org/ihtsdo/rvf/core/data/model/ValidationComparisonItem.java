package org.ihtsdo.rvf.core.data.model;

public class ValidationComparisonItem {

    private String testName;

    private String status;

    private Object expected;

    private Object actual;

    public ValidationComparisonItem() {
        this.status = ValidationComparisonReport.Status.PASS.toString();
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getExpected() {
        return expected;
    }

    public void setExpected(Object expected) {
        this.expected = expected;
    }

    public Object getActual() {
        return actual;
    }

    public void setActual(Object actual) {
        this.actual = actual;
    }
}
