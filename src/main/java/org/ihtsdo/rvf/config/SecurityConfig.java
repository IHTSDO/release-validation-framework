package org.ihtsdo.rvf.config;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.httpBasic(withDefaults());
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new RequestHeaderAuthenticationDecorator(), AuthorizationFilter.class);
        http.authorizeHttpRequests((authorize) -> authorize.requestMatchers("/swagger-ui.html",
						"/version",
						"/swagger-ui/**",
						"/v3/api-docs/**")
                .permitAll()
                .anyRequest().authenticated()
        );
		return http.build();
	}
}