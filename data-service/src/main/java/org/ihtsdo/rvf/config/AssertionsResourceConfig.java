package org.ihtsdo.rvf.config;

import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="rvf.assertion.resource")
@EnableAutoConfiguration
public class AssertionsResourceConfig extends ResourceConfiguration {

}
