package org.ihtsdo.rvf;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

import org.ihtsdo.rvf.config.ActiveMQConnectionFactoryPrefetchCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableJms
public class App extends ApiConfig {
	
	// Swagger Config
	@Bean
	public Docket swagger() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(not(regex("/error")))
				.build();
	}
	
	@Bean
	// The default queue prefetch size is 1,000. That prevents auto scaling rvf
	public ActiveMQConnectionFactoryPrefetchCustomizer queuePrefetchCustomizer(@Value("${spring.activemq.queuePrefetch:1}") int queuePrefetch) {
		return new ActiveMQConnectionFactoryPrefetchCustomizer(queuePrefetch);
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(App.class, args);
	}
}