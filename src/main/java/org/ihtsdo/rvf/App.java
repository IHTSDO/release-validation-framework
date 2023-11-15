package org.ihtsdo.rvf;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.ihtsdo.rvf.core.messaging.ActiveMQConnectionFactoryPrefetchCustomizer;
import org.ihtsdo.rvf.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
public class App extends Config {
	@Autowired(required = false)
	private BuildProperties buildProperties;

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	@Bean
	// The default queue prefetch size is 1,000. That prevents autoscaling rvf
	public ActiveMQConnectionFactoryPrefetchCustomizer queuePrefetchCustomizer(@Value("${spring.activemq.queuePrefetch:1}") int queuePrefetch) {
		return new ActiveMQConnectionFactoryPrefetchCustomizer(queuePrefetch);
	}

	@Bean
	public GroupedOpenApi apiDocs() {
		return GroupedOpenApi.builder()
				.group("release-validation-framework")
				.packagesToScan("org.ihtsdo.rvf.rest")
				// Don't show the error or root endpoints in swagger
				.pathsToExclude("/error", "/")
				.build();
	}

	@Bean
	public GroupedOpenApi springActuatorApi() {
		return GroupedOpenApi.builder()
				.group("actuator")
				.packagesToScan("org.springframework.boot.actuate")
				.pathsToMatch("/actuator/**")
				.build();
	}

	@Bean
	public OpenAPI apiInfo() {
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		return new OpenAPI()
				.info(new Info()
						.title("SNOMED CT Release Validation Framework (RVF)")
						.description("A framework for testing the validity of SNOMED CT releases based on groups of quality assertions")
						.version(version)
						.contact(new Contact().name("SNOMED International").url("https://www.snomed.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("See more about Release Validation Framework in GitHub")
						.url("https://github.com/IHTSDO/release-validation-framework"));
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(App.class, args);
	}
}
