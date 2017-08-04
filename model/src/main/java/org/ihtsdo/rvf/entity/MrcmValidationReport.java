package org.ihtsdo.rvf.entity;

import java.util.List;

/**
 * Created by NamLe on 5/29/2017.
 */
public class MrcmValidationReport {
    private TestType testType;
    private Long executionId;
    private long timeTakenInSeconds;
    private String reportUrl;
    private int totalTestsRun;
    private int totalSkips;
    private int totalFailures;
    private List<TestRunItem> assertionsSkipped;
    private List<TestRunItem> assertionsFailed;
    private List<TestRunItem> assertionsPassed;

    public MrcmValidationReport(TestType testType) {
        this.testType = testType;
    }

    public TestType getTestType() {
        return testType;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public long getTimeTakenInSeconds() {
        return timeTakenInSeconds;
    }

    public void setTimeTakenInSeconds(long timeTakenInSeconds) {
        this.timeTakenInSeconds = timeTakenInSeconds;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public int getTotalTestsRun() {
        return totalTestsRun;
    }

    public void setTotalTestsRun(int totalTestsRun) {
        this.totalTestsRun = totalTestsRun;
    }

    public int getTotalSkips() {
        return totalSkips;
    }

    public void setTotalSkips(int totalSkips) {
        this.totalSkips = totalSkips;
    }

    public int getTotalFailures() {
        return totalFailures;
    }

    public void setTotalFailures(int totalFailures) {
        this.totalFailures = totalFailures;
    }

    public List<TestRunItem> getAssertionsSkipped() {
        return assertionsSkipped;
    }

    public void setAssertionsSkipped(List<TestRunItem> assertionsSkipped) {
        this.assertionsSkipped = assertionsSkipped;
    }

    public List<TestRunItem> getAssertionsFailed() {
        return assertionsFailed;
    }

    public void setAssertionsFailed(List<TestRunItem> assertionsFailed) {
        this.assertionsFailed = assertionsFailed;
    }

    public List<TestRunItem> getAssertionsPassed() {
        return assertionsPassed;
    }

    public void setAssertionsPassed(List<TestRunItem> assertionsPassed) {
        this.assertionsPassed = assertionsPassed;
    }
}
