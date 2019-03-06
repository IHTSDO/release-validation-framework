package org.ihtsdo.rvf.execution.service.test.harness;

import org.ihtsdo.rvf.execution.service.ExecutionServiceConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("org.ihtsdo.rvf")
@TestConfiguration
public class ExecutionServiceEndToEndTestConfig extends ExecutionServiceConfig {
}
