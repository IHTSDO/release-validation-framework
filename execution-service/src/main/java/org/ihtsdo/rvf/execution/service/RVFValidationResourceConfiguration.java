package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.otf.dao.resources.ResourceConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="rvf.validation.storage")
@EnableAutoConfiguration
public class RVFValidationResourceConfiguration extends ResourceConfiguration {
	
}
