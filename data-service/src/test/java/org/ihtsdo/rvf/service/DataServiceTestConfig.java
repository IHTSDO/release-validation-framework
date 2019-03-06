package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.DataServiceConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan("org.ihtsdo.rvf")
@EnableAutoConfiguration
public class DataServiceTestConfig extends DataServiceConfig {

}
