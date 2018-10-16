package org.ihtsdo.rvf;

import org.springframework.boot.SpringApplication;

import springfox.documentation.swagger2.annotations.EnableSwagger2;
@EnableSwagger2
public class App extends ApiConfig {
	
	public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
    }
}