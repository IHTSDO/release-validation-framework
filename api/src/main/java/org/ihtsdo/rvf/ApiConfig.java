package org.ihtsdo.rvf;
import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
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
	
	//TODO change these parameters to spring.activemq.* 
	@Value("${orchestration.jms.url}") 
	private String brokerUrl;
	
	@Value("${orchestration.jms.username}") 
	private String userName;
	 
	@Value("${orchestration.jms.password}")
	private String password;
	
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
	
		//JMS config
		@Bean
		public ActiveMQConnectionFactory connectionFactory() {
		    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		    connectionFactory.setBrokerURL(brokerUrl);
		    connectionFactory.setPassword(userName);
		    connectionFactory.setUserName(password);
		    return connectionFactory;
		}

		@Bean
		public JmsTemplate jmsTemplate(){
		    JmsTemplate template = new JmsTemplate();
		    template.setConnectionFactory(connectionFactory());
		    return template;
		}

		@Bean
		public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
		    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		    factory.setConnectionFactory(connectionFactory());
		    factory.setConcurrency("1-1");
		    return factory;
		}
		
		@Bean
		public MessageConverter jacksonJmsMessageConverter() {
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
			converter.setTargetType(MessageType.TEXT);
			converter.setTypeIdPropertyName("_type");
			return converter;
		}
}
