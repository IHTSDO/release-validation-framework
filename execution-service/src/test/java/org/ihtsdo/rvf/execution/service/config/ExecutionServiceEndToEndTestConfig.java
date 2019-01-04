package org.ihtsdo.rvf.execution.service.config;

import org.ihtsdo.rvf.execution.service.ExecutionServiceConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@ComponentScan("org.ihtsdo.rvf")
public class ExecutionServiceEndToEndTestConfig extends ExecutionServiceConfig{
}
