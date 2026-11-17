package org.ihtsdo.rvf.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * RVF's JPA wiring, moved off {@link Config} so that it can be conditional.
 *
 * <p>{@code @EntityScan} and {@code @EnableJpaRepositories} are class-level
 * annotations, and {@link Config} is the application class, which cannot carry
 * an engine condition. On a configuration class of their own the condition is
 * evaluated before the repository registrar runs. Same packages, same
 * repositories as before.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMysqlEngine
@EntityScan("org.ihtsdo.rvf.core.data.model")
@EnableJpaRepositories("org.ihtsdo.rvf.core.data.repository")
public class MysqlPersistenceConfig {
}
