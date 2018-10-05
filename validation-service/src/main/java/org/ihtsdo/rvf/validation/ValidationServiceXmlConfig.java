package org.ihtsdo.rvf.validation;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath:validationContext.xml"})
public class ValidationServiceXmlConfig {

}
