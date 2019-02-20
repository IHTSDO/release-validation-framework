package org.ihtsdo.rvf;
import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

import javax.jms.ConnectionFactory;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ihtsdo.rvf.autoscaling.AutoScalingManager;
import org.ihtsdo.rvf.messaging.ValidationMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
@Configuration
@PropertySources({
	@PropertySource(value = "classpath:api-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/api.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
public class ApiConfig {	
	
	private Logger logger = LoggerFactory.getLogger(ApiConfig.class);
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
