package org.ihtsdo.rvf.execution.service.config;

import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="rvf.release.mysql.binary.storage")
@EnableAutoConfiguration
public class ValidationMysqlBinaryStorageConfig extends ResourceConfiguration {

}
