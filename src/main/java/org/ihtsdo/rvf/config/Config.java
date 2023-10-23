package org.ihtsdo.rvf.config;

import org.ihtsdo.otf.jms.MessagingHelper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@EnableConfigurationProperties
@EntityScan("org.ihtsdo.rvf.core.data.model")
@EnableJpaRepositories("org.ihtsdo.rvf.core.data.repository")
@EnableTransactionManagement
public abstract class Config extends DataResourceConfig {

	@Bean
	public MessagingHelper messagingHelper() {
		return new MessagingHelper();
	}
}
