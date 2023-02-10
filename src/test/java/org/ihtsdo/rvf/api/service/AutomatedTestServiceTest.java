package org.ihtsdo.rvf.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.entity.ValidationComparisonReport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AutomatedTestService.class})
public class AutomatedTestServiceTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();

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
        Assert.assertTrue(ValidationComparisonReport.Status.FAILED.equals(report.getStatus()));
        Assert.assertEquals(2, report.getNewAssertions().size());
        Assert.assertEquals(2, report.getRemovedAssertions().size());
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
        Assert.assertTrue(ValidationComparisonReport.Status.PASS.equals(report.getStatus()));
        Assert.assertEquals(null, report.getNewAssertions());
        Assert.assertEquals(null, report.getRemovedAssertions());
        Assert.assertEquals(null, report.getChangedAssertions());
    }
}
