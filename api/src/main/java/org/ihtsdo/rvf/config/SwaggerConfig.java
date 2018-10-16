//package org.ihtsdo.rvf.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
//import com.mangofactory.swagger.models.dto.ApiInfo;
//import com.mangofactory.swagger.paths.RelativeSwaggerPathProvider;
//import com.mangofactory.swagger.plugin.EnableSwagger;
//import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
//
///**
// * Class to support configuration of swagger based API documentation
// */
//@Configuration
//@EnableSwagger
//public class SwaggerConfig {
//
//	private SpringSwaggerConfig swaggerConfig;
//	private RelativeSwaggerPathProvider pathProvider;
//
//	@Autowired
//	public void setSpringSwaggerConfig(SpringSwaggerConfig swaggerConfig) {
//
//		this.swaggerConfig = swaggerConfig;
//	}
//
//	@Bean
//	public SwaggerSpringMvcPlugin apiImplementation() {
//
//		return new SwaggerSpringMvcPlugin(this.swaggerConfig)
//				.apiInfo(apiInfo()).pathProvider(this.pathProvider);
//
//	}
//
//	private ApiInfo apiInfo() {
//
//		return new ApiInfo(
//				"RVF API Docs",
//				"This is a listing of available apis of SNOMED release validation framework service. For more technical details visit "
//						+ "<a href='https://github.com/IHTSDO/release-validation-framework' > SNOMED release validation framework Service </a> page @ github.com ",
//				"https://github.com/IHTSDO/release-validation-framework",
//				"info@ihtsdotools.org", "Apache License, Version 2.0",
//				"http://www.apache.org/licenses/LICENSE-2.0");
//	}
//
//	@Bean
//	public RelativeSwaggerPathProvider setPathProvider(
//			RelativeSwaggerPathProvider pathProvider) {
//
//		this.pathProvider = pathProvider;
//		this.pathProvider.setApiResourcePrefix("v1");
//
//		return this.pathProvider;
//
//	}
//
//}
