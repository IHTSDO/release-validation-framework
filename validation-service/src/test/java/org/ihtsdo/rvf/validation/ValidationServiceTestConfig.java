package org.ihtsdo.rvf.validation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@ComponentScan("org.ihtsdo.rvf")
public class ValidationServiceTestConfig extends ValidationServiceConfig {

}
