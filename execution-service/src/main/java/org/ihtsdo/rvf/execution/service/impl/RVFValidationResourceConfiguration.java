package org.ihtsdo.rvf.execution.service.impl;

import org.ihtsdo.otf.dao.resources.ResourceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("rvf.validation.storagee")
public class RVFValidationResourceConfiguration extends ResourceConfiguration{

}
