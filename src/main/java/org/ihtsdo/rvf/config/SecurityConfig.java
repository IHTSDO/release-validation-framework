package org.ihtsdo.rvf.config;

import jakarta.servlet.http.HttpServletResponse;
import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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

		// Configure exception handling to prevent Basic Auth popup
		// Returns JSON response instead of triggering browser Basic Auth popup
		http.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, authException) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json;charset=UTF-8");
					String message = authException.getMessage() != null ? authException.getMessage().replace("\"", "\\\"") : "Authentication required";
					response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
				})
		);

		return http.build();
	}
}