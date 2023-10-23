package org.ihtsdo.rvf;

import org.ihtsdo.rvf.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;

@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@PropertySource(value = "classpath:application-test.properties", encoding = "UTF-8")
@TestConfiguration
@SpringBootApplication
public class TestConfig extends Config {
}
