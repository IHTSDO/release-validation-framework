package org.ihtsdo.rvf.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Switches off Spring Boot's database auto-configuration when an engine other
 * than MySQL is selected.
 *
 * <p>Removing RVF's own {@code dataSource} bean is not enough on its own.
 * {@code spring.datasource.url} is set in {@code application.properties} and
 * the MySQL driver is on the classpath, so {@link DataSourceAutoConfiguration}
 * would build a pool from those properties, {@link HibernateJpaAutoConfiguration}
 * would build an EntityManagerFactory on it, and
 * {@code spring.jpa.hibernate.ddl-auto} would run DDL against a database the
 * selected engine does not use.
 *
 * <p>An {@link EnvironmentPostProcessor} because the exclusion has to be
 * conditional, and {@code @SpringBootApplication(exclude = ...)} is not. It runs
 * after config data, so the property is honoured wherever it is set.
 *
 * <p>With the MySQL engine, which is the default, it does nothing: no property
 * source is added and the environment is returned untouched.
 */
public class ExecutionEngineEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PROPERTY_SOURCE_NAME = "rvf-execution-engine";

	private static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

	/**
	 * Referenced as classes rather than names, so that a Spring Boot upgrade
	 * that moves one of them fails to compile here instead of silently no
	 * longer excluding it.
	 */
	private static final List<String> DATABASE_AUTO_CONFIGURATION = List.of(
			DataSourceAutoConfiguration.class.getName(),
			DataSourceTransactionManagerAutoConfiguration.class.getName(),
			HibernateJpaAutoConfiguration.class.getName(),
			DataJpaRepositoriesAutoConfiguration.class.getName());

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!ExecutionEngine.isDuckDb(environment.getProperty(ExecutionEngine.PROPERTY))) {
			return;
		}
		Set<String> excludes = new LinkedHashSet<>(DATABASE_AUTO_CONFIGURATION);
		// Merged rather than overwritten, so a deployment's own exclusions survive.
		// Bound rather than read with getProperty, which would miss the indexed
		// form (spring.autoconfigure.exclude[0]=...); the property source added
		// below takes precedence over both forms, so it has to carry them.
		Binder.get(environment).bind(EXCLUDE_PROPERTY, Bindable.listOf(String.class))
				.orElse(List.of())
				.stream()
				.map(String::trim)
				.filter(StringUtils::hasText)
				.forEach(excludes::add);
		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME,
				Map.of(EXCLUDE_PROPERTY, String.join(",", excludes))));
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}
}
