package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.rvf.assertion._1_0.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

@Component
public class ValidationRunner {

    @Autowired
    private ResourceProviderFactory resourceProviderFactory;

    public Serializable execute() {
        return "";
    }

    private TestConfiguration loadConfiguration() {

        try {
            InputStream configurationStream = resourceProviderFactory.getConfigurationStream("column-pattern-configuration.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(TestConfiguration.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<TestConfiguration> configurationJAXBElement = unmarshaller.unmarshal(new StreamSource(configurationStream), TestConfiguration.class);
            return configurationJAXBElement.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to find the xml file", e);
        }
    }
}
