package org.ihtsdo.rvf.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.core.data.model.ValidationComparisonReport;
import org.ihtsdo.rvf.core.service.AutomatedTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AutomatedTestService.class})
public class AutomatedTestServiceTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();

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
