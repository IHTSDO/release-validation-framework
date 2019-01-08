package org.ihtsdo.rvf.execution.service.test.harness;

import org.ihtsdo.rvf.execution.service.ExecutionServiceConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.ihtsdo.rvf")
public class ExecutionServiceEndToEndTestConfig extends ExecutionServiceConfig {
}
