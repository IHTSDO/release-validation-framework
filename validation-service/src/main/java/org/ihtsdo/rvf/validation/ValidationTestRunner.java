package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

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
