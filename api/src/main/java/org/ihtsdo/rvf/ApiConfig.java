package org.ihtsdo.rvf;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication
@Configuration
@PropertySources({
	@PropertySource(value = "classpath:api-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/api.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
public class ApiConfig {

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
							"/webjars/springfox-swagger-ui/**")
				.permitAll();
			http.csrf().disable();
		}
	}
}
