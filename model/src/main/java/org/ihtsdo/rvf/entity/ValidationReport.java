package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by NamLe on 5/29/2017.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReport {
    private Long executionId;
    private TestType testType;
    private long timeTakenInSeconds;
    private String reportUrl;
    private int totalTestsRun;
    private int totalSkips;
    private int totalWarnings;
    private int totalFailures;
    private List<TestRunItem> assertionsSkipped;
    private List<TestRunItem> assertionsWarning;
    private List<TestRunItem> assertionsFailed;
    private List<TestRunItem> assertionsPassed;

    public ValidationReport() {
        testType = null;
        assertionsSkipped = new ArrayList<>();
        assertionsWarning = new ArrayList<>();
        assertionsFailed = new ArrayList<>();
        assertionsPassed = new ArrayList<>();
        totalTestsRun = 0;
        totalSkips = 0;
        totalWarnings = 0;
        totalFailures = 0;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public TestType getTestType() {
        return testType;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public long getTimeTakenInSeconds() {
        return timeTakenInSeconds;
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

    public int getTotalFailures() {
        return totalFailures;
    }

    public int getTotalWarnings() {
        return totalWarnings;
    }

    public List<TestRunItem> getAssertionsSkipped() {
        return assertionsSkipped;
    }

    public List<TestRunItem> getAssertionsFailed() {
        return assertionsFailed;
    }

    public List<TestRunItem> getAssertionsPassed() {
        return assertionsPassed;
    }

    public List<TestRunItem> getAssertionsWarning() {
        return assertionsWarning;
    }

    public void addSkippedAssertions(List<TestRunItem> skippedItems){
        if(!CollectionUtils.isEmpty(skippedItems)) {
            assertionsSkipped.addAll(skippedItems);
            int noOfItems = skippedItems.size();
            totalSkips += noOfItems;
            totalTestsRun += noOfItems;
        }
    }

    public void addWarningAssertions(List<TestRunItem> warningItems){
        if(!CollectionUtils.isEmpty(warningItems)) {
            assertionsWarning.addAll(warningItems);
            int noOfItems = warningItems.size();
            totalWarnings += noOfItems;
            totalTestsRun += noOfItems;
        }
    }

    public void addFailedAssertions(List<TestRunItem> failedItems){
        if(!CollectionUtils.isEmpty(failedItems)) {
            assertionsFailed.addAll(failedItems);
            int noOfItems = failedItems.size();
            totalFailures += noOfItems;
            totalTestsRun += noOfItems;
        }
    }

    public void addPassedAssertions(List<TestRunItem> passedItems){
        if(!CollectionUtils.isEmpty(passedItems)) {
            assertionsPassed.addAll(passedItems);
            int noOfItems = passedItems.size();
            totalTestsRun += noOfItems;
        }
    }

    public void addTimeTaken(long seconds){
        timeTakenInSeconds += seconds;
    }

    public void sortAssertionLists() {
        Collections.sort(assertionsSkipped);
        Collections.sort(assertionsFailed);
        Collections.sort(assertionsWarning);
        Collections.sort(assertionsPassed);
    }
}
