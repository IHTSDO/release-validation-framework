package org.ihtsdo.rvf.dataservice;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan("org.ihtsdo.rvf.dataservice")
@EnableAutoConfiguration
public class DataServiceTestConfig extends DatabaseServiceConfig {

}
