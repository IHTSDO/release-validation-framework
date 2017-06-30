package org.ihtsdo.rvf.entity;

import org.ihtsdo.drools.response.InvalidContent;

import java.util.List;

/**
 * Created by TinLe on 6/27/2017.
 */
public class DroolsRulesValidationReport {
    private TestType testType;
    private Long executionId;
    private long timeTakenInSeconds;
    private String reportUrl;
    private int totalTestsRun;
    private int totalSkips;
    private int totalFailures;
    private String ruleSetExecuted;

    private List<AssertionDroolRule> assertionsInvalidContent;

    public DroolsRulesValidationReport(TestType testType) {
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

    public List<AssertionDroolRule> getAssertionsInvalidContent() {
        return assertionsInvalidContent;
    }

    public void setAssertionsInvalidContent(List<AssertionDroolRule> assertionsInvalidContent) {
        this.assertionsInvalidContent = assertionsInvalidContent;
    }

    public String getRuleSetExecuted() {
        return ruleSetExecuted;
    }

    public void setRuleSetExecuted(String ruleSetExecuted) {
        this.ruleSetExecuted = ruleSetExecuted;
    }
}
