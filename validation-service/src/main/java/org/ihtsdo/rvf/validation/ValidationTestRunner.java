package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

@Component
public class ValidationTestRunner {



    @Autowired
    private ResourceProviderFactory resourceProviderFactory;

    public TestReportable execute(ResponseType type, ResourceManager resourceManager) {

        //ColumnPatternConfiguration configuration = loadConfiguration(configurationFile);
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        // factory to create this bases on the enumeration or responseType
        TestReport report = new TestReport(new CsvResultFormatter());

        runTests(resourceManager, validationLog, report);

        return report;
    }

    public TestReportable execute(ResponseType csv, ResourceManager resourceManager, OutputStream outputStream) {
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        StreamTestReport testReport = new StreamTestReport(new CsvResultFormatter(), outputStream);

        runTests(resourceManager, validationLog, testReport);

        return testReport;
    }

    private void runTests(ResourceManager resourceManager, ValidationLog validationLog, TestReportable report) {
        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, resourceManager, report);
        columnPatternTest.runTests();
    }
}
