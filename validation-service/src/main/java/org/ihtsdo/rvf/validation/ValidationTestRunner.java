package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidationTestRunner {

    @Autowired
    private ResourceProviderFactory resourceProviderFactory;

    @Autowired
    private ConfigurationFactory configurationFactory;

    public TestReport execute(ResponseType type, ResourceManager resourceManager) {

        //ColumnPatternConfiguration configuration = loadConfiguration(configurationFile);
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        // factory to create this bases on the enumeration or responseType
        TestReport report = new TestReport(new CsvResultFormatter());

        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, configurationFactory, resourceManager, report);
        columnPatternTest.runTests();

        return report;
    }


}
