package org.ihtsdo.rvf.validation;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
	@PropertySource(value = "classpath:validation-service-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/validation-service.properties", ignoreResourceNotFound=true)})
public class ValidationServiceConfig {
}
