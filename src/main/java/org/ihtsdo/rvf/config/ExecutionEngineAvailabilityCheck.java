package org.ihtsdo.rvf.config;

import org.ihtsdo.rvf.core.service.SqlAssertionValidationService;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start when the selected engine has no implementation in this build.
 *
 * <p>Runs once the bean definitions are final and before any bean is created.
 * Without it, selecting an engine that is not present fails later on whichever
 * unsatisfied dependency Spring happens to reach first, which names a bean
 * rather than the setting that caused it.
 */
@Component
public class ExecutionEngineAvailabilityCheck implements BeanFactoryPostProcessor, EnvironmentAware {

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		if (beanFactory.getBeanNamesForType(SqlAssertionValidationService.class, true, false).length == 0) {
			throw new IllegalStateException("No SQL assertion engine is available for "
					+ ExecutionEngine.PROPERTY + "=" + environment.getProperty(ExecutionEngine.PROPERTY)
					+ ": this build has no " + SqlAssertionValidationService.class.getSimpleName()
					+ " for it. Remove the property, or set it to " + ExecutionEngine.MYSQL + ".");
		}
	}
}
