package org.ihtsdo.rvf.execution.service;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = "org.ihtsdo.rvf",
			   excludeFilters = @Filter(type = FilterType.REGEX, pattern = {"org.ihtsdo.rvf.importer.*", 
			   		"org.ihtsdo.rvf.execution.service.test.harness.*"}))
public class ExecutionServiceTestConfig {
}
