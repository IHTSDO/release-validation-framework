package org.ihtsdo.rvf.executionservice.test.harness;

import org.ihtsdo.rvf.executionservice.ExecutionServiceConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("org.ihtsdo.rvf")
@TestConfiguration
public class ExecutionServiceEndToEndTestConfig extends ExecutionServiceConfig {
}
