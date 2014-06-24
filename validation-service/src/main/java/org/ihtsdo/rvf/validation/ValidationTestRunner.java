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

    public TestReport execute(String configurationFile, ResponseType type, ResourceManager resourceManager) {

        ColumnPatternConfiguration configuration = loadConfiguration(configurationFile);
        ValidationLog validationLog = resourceProviderFactory.getValidationLog(ColumnPatternTester.class);
        // factory to create this bases on the enumeration or responseType
        TestReport report = new TestReport(new CsvResultFormatter());

        ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, configuration, resourceManager, report);
        columnPatternTest.runTests();

        return report;
    }

    private ColumnPatternConfiguration loadConfiguration(String filename) {

        try {
            URL f = getClass().getResource(filename);
            File file = new File(f.toURI());
            InputStream configurationStream = new FileInputStream(file);

            JAXBContext jaxbContext = JAXBContext.newInstance(ColumnPatternConfiguration.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<ColumnPatternConfiguration> configurationJAXBElement = unmarshaller.unmarshal(new StreamSource(configurationStream), ColumnPatternConfiguration.class);
            return configurationJAXBElement.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to find the xml file", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to load the xml configuration file", e);
        }
    }
}
