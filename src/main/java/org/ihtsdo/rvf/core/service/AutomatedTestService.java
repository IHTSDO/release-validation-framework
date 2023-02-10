package org.ihtsdo.rvf.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AutomatedTestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatedTestService.class);

    private final int pollPeriod = 60 * 1000; // 1 minute

    @Value("${rvf.report.max.poll.period:3600000}")
    private int maxPollPeriod; // default 1 hour

    private final RestTemplate rvfRestTemplate = new RestTemplate();

    private final Map<String, ValidationComparisonReport> pendingCompareReports = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<ValidationComparisonReport> buildComparisonBlockingQueue = new LinkedBlockingQueue<>();

    private ExecutorService comparisonExecutorService = Executors.newFixedThreadPool(5);

    public String compareReportGivenUrls(String previousReportUrl, String prospectiveReportUrl) {
        String compareId = UUID.randomUUID().toString();

        ValidationComparisonReport report = new ValidationComparisonReport();
        report.setCompareId(compareId);
        report.setLeftReportUrl(previousReportUrl);
        report.setRightReportUrl(prospectiveReportUrl);
        report.setStatus(ValidationComparisonReport.Status.RUNNING);
        report.setStartDate(new Date());
        try {
            buildComparisonBlockingQueue.put(report);
            pendingCompareReports.put(compareId, report);
            processReportComparisonJobs();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return compareId;
    }

    public ValidationComparisonReport getCompareReport(String compareId) {
        if (pendingCompareReports.containsKey(compareId)) {
            synchronized (pendingCompareReports) {
                if (pendingCompareReports.get(compareId).getStatus().equals(ValidationComparisonReport.Status.RUNNING)) {
                    return pendingCompareReports.get(compareId);
                } else {
                    return pendingCompareReports.remove(compareId);
                }
            }
        }
        return null;
    }

    protected void processReportComparisonJobs() {
        comparisonExecutorService.submit(() -> {
            ValidationComparisonReport report = null;
            try {
                report = buildComparisonBlockingQueue.take();
                ValidationStatusReport leftValidationReport = getValidationStatusReport(report.getLeftReportUrl());
                ValidationStatusReport rightValidationReport = getValidationStatusReport(report.getRightReportUrl());
                compareReports(report, leftValidationReport, rightValidationReport);
                pendingCompareReports.put(report.getCompareId(), report);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                if (report != null) {
                    report.setStatus(ValidationComparisonReport.Status.FAILED_TO_COMPARE);
                    report.setMessage(String.format("Failed to compare. Error message: %s", e.getMessage()));
                    pendingCompareReports.put(report.getCompareId(), report);
                }
            }
        });
    }

    public void compareReports(ValidationComparisonReport report, ValidationStatusReport leftValidationReport, ValidationStatusReport rightValidationReport) {
        ValidationReport leftResultReport = leftValidationReport.getResultReport();
        ValidationReport rightResultReport = rightValidationReport.getResultReport();
        report.setStatus(ValidationComparisonReport.Status.PASS);
        ValidationComparisonItem item = new ValidationComparisonItem();
        item.setTestName("Total Test Run");
        item.setExpected(leftResultReport.getTotalTestsRun());
        item.setActual(rightResultReport.getTotalTestsRun());
        if (leftResultReport.getTotalTestsRun() != rightResultReport.getTotalTestsRun()) {
            item.setStatus(ValidationComparisonReport.Status.FAILED.toString());
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        }
        report.addComparisonItem(item);

        item = new ValidationComparisonItem();
        item.setTestName("Total Failures");
        item.setExpected(leftResultReport.getTotalFailures());
        item.setActual(rightResultReport.getTotalFailures());
        if (leftResultReport.getTotalFailures() != rightResultReport.getTotalFailures()) {
            item.setStatus(ValidationComparisonReport.Status.FAILED.toString());
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        }
        report.addComparisonItem(item);

        item = new ValidationComparisonItem();
        item.setTestName("Total Warnings");
        item.setExpected(leftResultReport.getTotalWarnings());
        item.setActual(rightResultReport.getTotalWarnings());
        if (leftResultReport.getTotalWarnings() != rightResultReport.getTotalWarnings()) {
            item.setStatus(ValidationComparisonReport.Status.FAILED.toString());
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        }
        report.addComparisonItem(item);

        List<TestRunItem> leftAllTestRunItems = new ArrayList <>();
        leftAllTestRunItems.addAll(leftResultReport.getAssertionsFailed());
        leftAllTestRunItems.addAll(leftResultReport.getAssertionsPassed());
        leftAllTestRunItems.addAll(leftResultReport.getAssertionsSkipped());
        leftAllTestRunItems.addAll(leftResultReport.getAssertionsWarning());
        Map<String, TestRunItem> leftTestRunItemToUUIDMap = leftAllTestRunItems.stream()
                .filter(i -> !TestType.MRCM.equals(i.getTestType()))
                .collect(Collectors.toMap(i -> i.getAssertionUuid().toString(), Function.identity()));

        List<TestRunItem> rightAllTestRunItems = new ArrayList <>();
        rightAllTestRunItems.addAll(rightResultReport.getAssertionsFailed());
        rightAllTestRunItems.addAll(rightResultReport.getAssertionsPassed());
        rightAllTestRunItems.addAll(rightResultReport.getAssertionsSkipped());
        rightAllTestRunItems.addAll(rightResultReport.getAssertionsWarning());
        Map<String, TestRunItem> rightTestRunItemToUUIDMap = rightAllTestRunItems.stream()
                .filter(i -> !TestType.MRCM.equals(i.getTestType()))
                .collect(Collectors.toMap(i -> i.getAssertionUuid().toString(), Function.identity()));

        Collection removedIDs = CollectionUtils.subtract(leftTestRunItemToUUIDMap.keySet(), rightTestRunItemToUUIDMap.keySet());
        Collection addedIDs = CollectionUtils.subtract(rightTestRunItemToUUIDMap.keySet(), leftTestRunItemToUUIDMap.keySet());
        Collection unChangedIDs = CollectionUtils.intersection(rightTestRunItemToUUIDMap.keySet(), leftTestRunItemToUUIDMap.keySet());
        removedIDs.forEach(id -> {
            report.addRemovedAssertion(leftTestRunItemToUUIDMap.get(id));
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        });
        addedIDs.forEach(id -> {
            report.addNewAssertion(rightTestRunItemToUUIDMap.get(id));
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        });
        unChangedIDs.forEach(id -> {
            if (leftTestRunItemToUUIDMap.get(id).getFailureCount().longValue() != rightTestRunItemToUUIDMap.get(id).getFailureCount().longValue()) {
                ValidationComparisonItem changedItem = new ValidationComparisonItem();
                changedItem.setStatus(null);
                changedItem.setExpected(leftTestRunItemToUUIDMap.get(id));
                changedItem.setActual(rightTestRunItemToUUIDMap.get(id));
                report.addChangedAssertion(changedItem);
                if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                    report.setStatus(ValidationComparisonReport.Status.FAILED);
                }
            }
        });

        Map<String, TestRunItem> leftMRCMTestRunItemToUUIDMap = leftAllTestRunItems.stream()
                .filter(i -> TestType.MRCM.equals(i.getTestType()))
                .collect(Collectors.toMap(i -> i.getAssertionUuid().toString() + ":" + i.getAssertionText(), Function.identity()));
        Map<String, TestRunItem> rightMRCMTestRunItemToUUIDMap = rightAllTestRunItems.stream()
                .filter(i -> TestType.MRCM.equals(i.getTestType()))
                .collect(Collectors.toMap(i -> i.getAssertionUuid().toString() + ":" + i.getAssertionText(), Function.identity()));
        Collection removedMRMCIDs = CollectionUtils.subtract(leftMRCMTestRunItemToUUIDMap.keySet(), rightMRCMTestRunItemToUUIDMap.keySet());
        Collection addedMRMCIDs = CollectionUtils.subtract(rightMRCMTestRunItemToUUIDMap.keySet(), leftMRCMTestRunItemToUUIDMap.keySet());
        Collection unChangedMRMCIDs = CollectionUtils.intersection(leftMRCMTestRunItemToUUIDMap.keySet(), rightMRCMTestRunItemToUUIDMap.keySet());
        removedMRMCIDs.forEach(id -> {
            report.addRemovedAssertion(leftMRCMTestRunItemToUUIDMap.get(id));
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        });
        addedMRMCIDs.forEach(id -> {
            report.addNewAssertion(rightMRCMTestRunItemToUUIDMap.get(id));
            if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                report.setStatus(ValidationComparisonReport.Status.FAILED);
            }
        });
        unChangedMRMCIDs.forEach(id -> {
            if (leftMRCMTestRunItemToUUIDMap.get(id).getFailureCount().longValue() != rightMRCMTestRunItemToUUIDMap.get(id).getFailureCount().longValue()) {
                ValidationComparisonItem changedItem = new ValidationComparisonItem();
                changedItem.setStatus(null);
                changedItem.setExpected(leftMRCMTestRunItemToUUIDMap.get(id));
                changedItem.setActual(rightMRCMTestRunItemToUUIDMap.get(id));
                report.addChangedAssertion(changedItem);
                if (!ValidationComparisonReport.Status.FAILED.equals(report.getStatus())) {
                    report.setStatus(ValidationComparisonReport.Status.FAILED);
                }
            }
        });
    }

    private ValidationStatusReport getValidationStatusReport(final String url) throws InterruptedException, BusinessServiceException, IOException {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
        int count = 0;
        while (true) {
            Thread.sleep(pollPeriod);
            count += pollPeriod;

            String validationReportString = rvfRestTemplate.getForObject(url, String.class);
            if (!StringUtils.isEmpty(validationReportString)) {
                final HighLevelValidationReport highLevelValidationReport = objectMapper.readValue(validationReportString, HighLevelValidationReport.class);
                if (highLevelValidationReport != null && "COMPLETE".equals(highLevelValidationReport.getStatus())) {
                    return highLevelValidationReport.getRvfValidationResult();
                }
            }

            if (count > maxPollPeriod) {
                throw new BusinessServiceException(String.format("RVF report %s did not complete within the allotted time (%s minutes).", url, maxPollPeriod / (60 * 1000)));
            }
        }
    }


    public static final class HighLevelValidationReport {
        private String status;
        private ValidationStatusReport rvfValidationResult;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public ValidationStatusReport getRvfValidationResult() {
            return rvfValidationResult;
        }

        public void setRvfValidationResult(ValidationStatusReport rvfValidationResult) {
            this.rvfValidationResult = rvfValidationResult;
        }
    }
}
