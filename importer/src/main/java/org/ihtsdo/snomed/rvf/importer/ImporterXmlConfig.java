package org.ihtsdo.snomed.rvf.importer;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath:importerServiceContext.xml"})
@EnableAutoConfiguration
public class ImporterXmlConfig {

}
