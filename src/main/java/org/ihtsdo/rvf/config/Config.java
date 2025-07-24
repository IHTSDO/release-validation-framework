package org.ihtsdo.rvf.config;

import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;
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

	@Bean
	public ModuleStorageCoordinator moduleStorageCoordinator(@Autowired ModuleStorageResourceConfig moduleStorageResourceConfig, @Autowired ResourceLoader cloudResourceLoader, @Value("${module.storage.environment.shortname}") final String envShortname) {
		ResourceManager resourceManager = new ResourceManager(moduleStorageResourceConfig, cloudResourceLoader);
		return switch (envShortname) {
			case "prod" -> ModuleStorageCoordinator.initProd(resourceManager);
			case "uat" -> ModuleStorageCoordinator.initUat(resourceManager);
			default -> ModuleStorageCoordinator.initDev(resourceManager);
		};
	}
}
