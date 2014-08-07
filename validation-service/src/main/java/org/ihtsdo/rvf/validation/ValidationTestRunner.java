package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.validation.resource.ResourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

@Component
public class ValidationTestRunner {

    @Autowired
    private ResourceProviderFactory resourceProviderFactory;
    
    public TestReportable execute(ResourceManager resourceManager, PrintWriter writer, boolean writeSuccesses,
                                  ManifestFile manifest) {

        // the information for the manifest testing
        StreamTestReport testReport = new StreamTestReport(new CsvMetadataResultFormatter(), writer, true);
        // run manifest tests
        runManifestTests(resourceManager, testReport, manifest);
        testReport.addNewLine();
        
        // run column tests
        testReport.setFormatter(new CsvResultFormatter());
        testReport.setWriteSuccesses(writeSuccesses);
        runColumnTests(resourceManager, testReport);
        return null;
    }
    
    public TestReportable execute(ResourceManager resourceManager, PrintWriter writer, boolean writeSuccesses) {
        
        StreamTestReport testReport = new StreamTestReport(new CsvResultFormatter(), writer, writeSuccesses);
        runColumnTests(resourceManager, testReport);
        return testReport;
    }

    private void runManifestTests(ResourceManager resourceManager, TestReportable report, 
                                  ManifestFile manifest) {
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ManifestPatternTester.class);
        ManifestPatternTester manifestPatternTester = new ManifestPatternTester(validationLog, resourceManager, manifest, report);
        manifestPatternTester.runTests();
    }

    private void runColumnTests(ResourceManager resourceManager, TestReportable report) {
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, resourceManager, report);
        columnPatternTest.runTests();
        String summary = report.writeSummary();
        validationLog.info(summary);
    }
}
