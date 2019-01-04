package org.ihtsdo.rvf.execution.service.config;

import org.ihtsdo.rvf.execution.service.ExecutionServiceConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type=FilterType.REGEX, pattern="org\\.ihtsdo\\.rvf\\.importer\\..*"))
public class ExecutionServiceTestConfig extends ExecutionServiceConfig{
}
