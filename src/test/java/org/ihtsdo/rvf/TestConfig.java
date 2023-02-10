package org.ihtsdo.rvf;

import org.ihtsdo.rvf.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.*;
import org.springframework.context.annotation.PropertySource;

@PropertySource("application.properties")
@PropertySource("application-test.properties")
@TestConfiguration
@SpringBootApplication(
        exclude = {ContextCredentialsAutoConfiguration.class,
                ContextInstanceDataAutoConfiguration.class,
                ContextRegionProviderAutoConfiguration.class,
                ContextResourceLoaderAutoConfiguration.class,
                ContextStackAutoConfiguration.class})
public class TestConfig extends Config {
}
