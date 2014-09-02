package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.log.ValidationLogFactory;
import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.resource.ResourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

@Component
public class ValidationTestRunner {

    @Autowired
    private ValidationLogFactory validationLogFactory;
    
    public TestReportable execute(ResourceManager resourceManager, PrintWriter writer, boolean writeSuccesses,
                                  ManifestFile manifest) {

        // the information for the manifest testing
        StreamTestReport testReport = new StreamTestReport(new CsvMetadataResultFormatter(), writer, true);
        // run manifest tests
        runManifestTests(resourceManager, testReport, manifest, validationLogFactory.getValidationLog(ManifestPatternTester.class));
        testReport.addNewLine();
        
        // run column tests
        testReport.setFormatter(new CsvResultFormatter());
        testReport.setWriteSuccesses(writeSuccesses);
        ValidationLog validationLog = validationLogFactory.getValidationLog(ColumnPatternTester.class);
        runColumnTests(resourceManager, testReport, validationLog);

        String summary = testReport.writeSummary();
        validationLog.info(summary);
        
        return testReport;
    }
    
    public TestReportable execute(ResourceManager resourceManager, PrintWriter writer, boolean writeSuccesses) {
        
        StreamTestReport testReport = new StreamTestReport(new CsvResultFormatter(), writer, writeSuccesses);
        ValidationLog validationLog = validationLogFactory.getValidationLog(ColumnPatternTester.class);
        runColumnTests(resourceManager, testReport, validationLog);
        String summary = testReport.writeSummary();
        validationLog.info(summary);
        return testReport;
    }

    private void runManifestTests(ResourceManager resourceManager, TestReportable report,
                                  ManifestFile manifest, ValidationLog validationLog) {
        ManifestPatternTester manifestPatternTester = new ManifestPatternTester(validationLog, resourceManager, manifest, report);
        manifestPatternTester.runTests();
    }

    private void runColumnTests(ResourceManager resourceManager, TestReportable report, ValidationLog validationLog) {
        
        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, resourceManager, report);
        columnPatternTest.runTests();
    }
}
