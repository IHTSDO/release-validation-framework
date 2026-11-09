package org.ihtsdo.rvf.config;

import org.ihtsdo.rvf.App;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.SqlAssertionValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;

import java.util.List;
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

	/** Beans that only the DuckDB engine registers, by bean name. */
	private static final String[] DUCKDB_BEANS = {
			"duckStoreLocator", "duckAssertionService", "duckDbValidationService", "assertionPackController"};

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
		for (String bean : DUCKDB_BEANS) {
			assertFalse(mysql.containsKey(bean), bean);
		}
		for (String type : mysql.values()) {
			assertFalse(type.startsWith("org.ihtsdo.rvf.core.service.duck."), type);
		}
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
		assertTrue(removed.containsAll(List.of(MYSQL_BEANS)), removed.toString());
	}

	@Test
	void theDuckDbEngineRegistersItsOwnBeansInsteadOfTheMysqlOnes() {
		Map<String, String> duckdb = beanDefinitions(ExecutionEngine.DUCKDB);

		for (String bean : DUCKDB_BEANS) {
			assertTrue(duckdb.containsKey(bean), bean);
		}
		assertEquals("org.ihtsdo.rvf.core.service.duck.DuckStoreLocator", duckdb.get("duckStoreLocator"));
		assertEquals("org.ihtsdo.rvf.core.service.duck.DuckAssertionService", duckdb.get("duckAssertionService"));
		assertEquals("org.ihtsdo.rvf.core.service.duck.DuckDbValidationService", duckdb.get("duckDbValidationService"));
		assertEquals("org.ihtsdo.rvf.rest.controller.AssertionPackController", duckdb.get("assertionPackController"));

		// One of each, so ValidationRunner and the controllers inject unambiguously.
		assertEquals(List.of("duckDbValidationService"),
				implementations(duckdb, SqlAssertionValidationService.class));
		assertEquals(List.of("duckAssertionService"), implementations(duckdb, AssertionService.class));
	}

	@ParameterizedTest
	@ValueSource(strings = {" DuckDB ", "DUCKDB"})
	void duckDbIsSelectedIgnoringCaseAndSurroundingWhitespace(String value) {
		assertEquals(beanDefinitions(ExecutionEngine.DUCKDB), beanDefinitions(value));
	}

	/** The names of the beans whose type is {@code type} or a subtype of it. */
	private static List<String> implementations(Map<String, String> beans, Class<?> type) {
		return beans.entrySet().stream().filter(bean -> {
			try {
				return type.isAssignableFrom(Class.forName(bean.getValue(), false, type.getClassLoader()));
			} catch (ClassNotFoundException | LinkageError e) {
				return false;
			}
		}).map(Map.Entry::getKey).toList();
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
	 * Runs the application, stopped before any bean is created: a bean factory
	 * post-processor registered on the context runs once every bean definition
	 * is registered, ahead of the application's own. Command-line arguments, so
	 * they override {@code application.properties}.
	 */
	private static void run(String engine, Consumer<ConfigurableListableBeanFactory> inspect) {
		SpringApplication application = new SpringApplication(App.class);
		application.addInitializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
			inspect.accept(beanFactory);
			throw new SpringApplication.AbandonedRunException();
		}));
		application.run("--" + ExecutionEngine.PROPERTY + "=" + engine,
				"--spring.datasource.url=jdbc:mysql://192.0.2.1:1/unreachable",
				"--spring.main.banner-mode=off");
	}
}
