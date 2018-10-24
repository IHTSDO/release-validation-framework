package org.ihtsdo.rvf;
import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@SpringBootApplication
@Configuration
@PropertySources({
	@PropertySource(value = "api-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/api.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
//@ComponentScan(basePackages = {"org.ihtsdo.snomed.rvf"})
public class ApiConfig {
	// Swagger Config
		@Bean
		public Docket api() {
			return new Docket(DocumentationType.SWAGGER_2)
					.select()
					.apis(RequestHandlerSelectors.any())
					.paths(not(regex("/error")))
					.build();
		}

		// Security
		@Configuration
		@EnableWebSecurity
		@Order(1)
		public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
			protected void configure(HttpSecurity http) throws Exception {
				http.authorizeRequests()
						.antMatchers("/swagger-ui.html",
								"/swagger-resources/**",
								"/v2/api-docs",
								"/v2/**",
								"/webjars/springfox-swagger-ui/**").permitAll();
//						.anyRequest().authenticated()
//						.and().httpBasic();
				http.csrf().disable();
//				http.addFilterAfter(new RequestHeaderAuthenticationDecorator(), BasicAuthenticationFilter.class);
			}

		}

}

