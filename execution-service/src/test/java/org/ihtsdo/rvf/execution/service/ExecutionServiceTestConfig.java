package org.ihtsdo.rvf.execution.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

@ComponentScan(basePackages = "org.ihtsdo.rvf",
				excludeFilters = @Filter(type = FilterType.REGEX,
				pattern = {"org.ihtsdo.rvf.importer.*", "org.ihtsdo.rvf.execution.service.test.harness.*"}))
@TestConfiguration
public class ExecutionServiceTestConfig {
}
