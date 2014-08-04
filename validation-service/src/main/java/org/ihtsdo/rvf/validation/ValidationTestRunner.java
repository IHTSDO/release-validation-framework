package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.PrintWriter;

@Component
public class ValidationTestRunner {



    @Autowired
    private ResourceProviderFactory resourceProviderFactory;

    public TestReportable execute(ResponseType csv, ResourceManager resourceManager, PrintWriter writer, boolean writeSuccesses) {
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        StreamTestReport testReport = new StreamTestReport(new CsvResultFormatter(), writer, writeSuccesses);

        runTests(resourceManager, validationLog, testReport);

        String summary = testReport.writeSummary();
        validationLog.info(summary);

        return testReport;
    }

    private void runTests(ResourceManager resourceManager, ValidationLog validationLog, TestReportable report) {
        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, resourceManager, report);
        columnPatternTest.runTests();
    }
}
