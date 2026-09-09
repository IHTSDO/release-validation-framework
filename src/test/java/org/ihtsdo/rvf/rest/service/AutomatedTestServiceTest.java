package org.ihtsdo.rvf.rest.service;

import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.data.model.ValidationComparisonReport;
import org.ihtsdo.rvf.core.service.AutomatedTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class AutomatedTestServiceTest extends IntegrationTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Autowired
    private AutomatedTestService automatedTestService;

    @Test
    public void compareReports_ShouldReturnExpectedResponse_WhenReportsAreDifferent() throws IOException {
        ValidationComparisonReport report = new ValidationComparisonReport();
        InputStream previousReportStream = getClass().getResourceAsStream("/previous_rvf_results.json");
        InputStream prospectiveReportStream = getClass().getResourceAsStream("/prospective_rvf_results.json");
        assertNotNull(previousReportStream);
        assertNotNull(prospectiveReportStream);
        final AutomatedTestService.HighLevelValidationReport leftHighLevelValidationReport = objectMapper.readValue(previousReportStream, AutomatedTestService.HighLevelValidationReport.class);
        final AutomatedTestService.HighLevelValidationReport rightHighLevelValidationReport = objectMapper.readValue(prospectiveReportStream, AutomatedTestService.HighLevelValidationReport.class);
        automatedTestService.compareReports(report, leftHighLevelValidationReport.getRvfValidationResult(), rightHighLevelValidationReport.getRvfValidationResult());
        assertEquals(ValidationComparisonReport.Status.FAILED, report.getStatus());
        assertEquals(2, report.getNewAssertions().size());
        assertEquals(2, report.getRemovedAssertions().size());
    }

    @Test
    public void compareReports_ShouldReturnExpectedResponse_WhenReportsAreIdentical() throws IOException {
        ValidationComparisonReport report = new ValidationComparisonReport();
        InputStream previousReportStream = getClass().getResourceAsStream("/previous_rvf_results.json");
        InputStream prospectiveReportStream = getClass().getResourceAsStream("/previous_rvf_results.json");
        assertNotNull(previousReportStream);
        assertNotNull(prospectiveReportStream);
        final AutomatedTestService.HighLevelValidationReport leftHighLevelValidationReport = objectMapper.readValue(previousReportStream, AutomatedTestService.HighLevelValidationReport.class);
        final AutomatedTestService.HighLevelValidationReport rightHighLevelValidationReport = objectMapper.readValue(prospectiveReportStream, AutomatedTestService.HighLevelValidationReport.class);
        automatedTestService.compareReports(report, leftHighLevelValidationReport.getRvfValidationResult(), rightHighLevelValidationReport.getRvfValidationResult());
        assertEquals(ValidationComparisonReport.Status.PASS, report.getStatus());
        assertNull(report.getNewAssertions());
        assertNull(report.getRemovedAssertions());
        assertNull(report.getChangedAssertions());
    }
}
