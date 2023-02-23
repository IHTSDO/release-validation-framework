package org.ihtsdo.rvf.core.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ValidationComparisonReport {
    public enum Status {
        RUNNING, PASS, FAILED, FAILED_TO_COMPARE
    }

    private String compareId;

    private Status status;

    private String message;

    private Date startDate;

    private String leftReportUrl;

    private String rightReportUrl;

    List<ValidationComparisonItem> comparisonItems;

    List<TestRunItem> newAssertions;

    List<TestRunItem> removedAssertions;

    List<ValidationComparisonItem> changedAssertions;

    public String getCompareId() {
        return compareId;
    }

    public void setCompareId(String compareId) {
        this.compareId = compareId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getLeftReportUrl() {
        return leftReportUrl;
    }

    public void setLeftReportUrl(String leftReportUrl) {
        this.leftReportUrl = leftReportUrl;
    }

    public String getRightReportUrl() {
        return rightReportUrl;
    }

    public void setRightReportUrl(String rightReportUrl) {
        this.rightReportUrl = rightReportUrl;
    }

    public List<ValidationComparisonItem> getComparisonItems() {
        return comparisonItems;
    }

    public void setComparisonItems(List<ValidationComparisonItem> comparisonItems) {
        this.comparisonItems = comparisonItems;
    }

    public List <TestRunItem> getNewAssertions() {
        return newAssertions;
    }

    public void setNewAssertions(List <TestRunItem> newAssertions) {
        this.newAssertions = newAssertions;
    }

    public List <TestRunItem> getRemovedAssertions() {
        return removedAssertions;
    }

    public void setRemovedAssertions(List <TestRunItem> removedAssertions) {
        this.removedAssertions = removedAssertions;
    }

    public List <ValidationComparisonItem> getChangedAssertions() {
        return changedAssertions;
    }

    public void setChangedAssertions(List <ValidationComparisonItem> changedAssertions) {
        this.changedAssertions = changedAssertions;
    }

    public void addComparisonItem(ValidationComparisonItem item) {
        if (comparisonItems == null) {
            this.comparisonItems = new ArrayList<>();
        }
        this.comparisonItems.add(item);
    }

    public void addRemovedAssertion(TestRunItem item) {
        if (removedAssertions == null) {
            this.removedAssertions = new ArrayList<>();
        }
        this.removedAssertions.add(item);
    }

    public void addNewAssertion(TestRunItem item) {
        if (newAssertions == null) {
            this.newAssertions = new ArrayList<>();
        }
        this.newAssertions.add(item);
    }

    public void addChangedAssertion(ValidationComparisonItem item) {
        if (changedAssertions == null) {
            this.changedAssertions = new ArrayList<>();
        }
        this.changedAssertions.add(item);
    }
}
