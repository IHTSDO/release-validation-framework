package org.ihtsdo.rvf.config;

import org.ihtsdo.rvf.App;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which beans each engine setting registers, read from the final bean
 * definitions of the real application. No database is needed: the application
 * is stopped once its bean definitions are complete and before any bean is
 * created, and the datasource URL points at an address nothing listens on.
 */
class ExecutionEngineContextTest {

	/** Beans that need the MySQL database, by bean name. */
	private static final String[] MYSQL_BEANS = {
			"dataSource", "mysqlPersistenceConfig", "entityManagerFactory", "transactionManager",
			"assertionRepository", "assertionGroupRepository",
			"assertionServiceImpl", "assertionExecutionService", "mysqlFailuresExtractor",
			"mysqlValidationService", "releaseDataManager", "resourceDataLoader", "rvfDynamicDataSource",
			"validationVersionLoader", "assertionGroupImporter", "assertionsDatabaseImporter",
			"rvfAssertionsDatabasePrimerService", "assertionHelper",
			"assertionAdministrationController", "assertionGroupAdministrationController", "releaseController"};

	/** Beans that do not depend on the engine. */
	private static final String[] ENGINE_NEUTRAL_BEANS = {
			"validationRunner", "releaseAcquisitionService", "validationMessageListener",
			"assertionController", "assertionGroupController", "assertionLookup",
			"testUploadFileController", "automatedTestController", "executionEngineAvailabilityCheck"};

	@ParameterizedTest
	@ValueSource(strings = {"MySQL", "", "postgres", " mysql "})
	void everyValueOtherThanDuckDbRegistersExactlyTheMysqlEngineBeans(String value) {
		Map<String, String> mysql = beanDefinitions(ExecutionEngine.MYSQL);

		assertEquals(mysql, beanDefinitions(value));
	}

	@Test
	void theMysqlEngineRegistersTheDatabaseBeans() {
		Map<String, String> mysql = beanDefinitions(ExecutionEngine.MYSQL);

		for (String bean : MYSQL_BEANS) {
			assertTrue(mysql.containsKey(bean), bean);
		}
		for (String bean : ENGINE_NEUTRAL_BEANS) {
			assertTrue(mysql.containsKey(bean), bean);
		}
		assertEquals("org.ihtsdo.rvf.core.service.MysqlValidationService",
				mysql.get("mysqlValidationService"));
	}

	@Test
	void anotherEngineRegistersNoDatabaseBeans() {
		Map<String, String> duckdb = beanDefinitions(ExecutionEngine.DUCKDB);

		for (String bean : MYSQL_BEANS) {
			assertFalse(duckdb.containsKey(bean), bean);
		}
		for (String bean : ENGINE_NEUTRAL_BEANS) {
			assertTrue(duckdb.containsKey(bean), bean);
		}
		for (String type : duckdb.values()) {
			assertFalse(type.contains("DataSource") && !type.contains("Properties"), type);
			assertFalse(type.contains("EntityManagerFactory"), type);
		}

		TreeSet<String> removed = new TreeSet<>(beanDefinitions(ExecutionEngine.MYSQL).keySet());
		removed.removeAll(duckdb.keySet());
		assertTrue(removed.containsAll(java.util.List.of(MYSQL_BEANS)), removed.toString());
	}

	@Test
	void selectingAnEngineThisBuildDoesNotHaveFailsAtStartupAndSaysWhy() {
		Throwable failure = assertThrows(Throwable.class, () -> run(ExecutionEngine.DUCKDB, null));

		Throwable cause = failure;
		while (!(cause instanceof IllegalStateException) && cause.getCause() != null) {
			cause = cause.getCause();
		}
		assertInstanceOf(IllegalStateException.class, cause, failure.toString());
		assertEquals("No SQL assertion engine is available for rvf.execution.engine=duckdb: this build has no "
				+ "SqlAssertionValidationService for it. Remove the property, or set it to mysql.", cause.getMessage());
	}

	/** Bean name to bean type, as registered for {@code engine}. */
	private static Map<String, String> beanDefinitions(String engine) {
		Map<String, String> beans = new TreeMap<>();
		assertThrows(SpringApplication.AbandonedRunException.class, () -> run(engine, beanFactory -> {
			for (String name : beanFactory.getBeanDefinitionNames()) {
				Class<?> type = beanFactory.getType(name, false);
				beans.put(name, type == null ? "?" : type.getName());
			}
		}));
		assertFalse(beans.isEmpty());
		return beans;
	}

	/**
	 * Runs the application, stopped before any bean is created if
	 * {@code inspect} is given: a bean factory post-processor registered on the
	 * context runs once every bean definition is registered, ahead of the
	 * application's own. Command-line arguments, so they override
	 * {@code application.properties}.
	 */
	private static void run(String engine, Consumer<ConfigurableListableBeanFactory> inspect) {
		SpringApplication application = new SpringApplication(App.class);
		if (inspect != null) {
			application.addInitializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
				inspect.accept(beanFactory);
				throw new SpringApplication.AbandonedRunException();
			}));
		}
		application.run("--" + ExecutionEngine.PROPERTY + "=" + engine,
				"--spring.datasource.url=jdbc:mysql://192.0.2.1:1/unreachable",
				"--spring.main.banner-mode=off");
	}
}
