package org.ihtsdo.rvf;

import org.springframework.boot.SpringApplication;
import org.springframework.jms.annotation.EnableJms;

import springfox.documentation.swagger2.annotations.EnableSwagger2;
@EnableSwagger2
@EnableJms
public class App extends ApiConfig {
	
	public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
    }
}