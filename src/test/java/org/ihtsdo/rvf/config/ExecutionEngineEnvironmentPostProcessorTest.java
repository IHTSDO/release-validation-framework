package org.ihtsdo.rvf.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The post-processor runs on every start, before any bean exists. With the
 * MySQL engine it must leave the environment exactly as it found it.
 */
class ExecutionEngineEnvironmentPostProcessorTest {

	private static final String EXCLUDE = "spring.autoconfigure.exclude";

	private final ExecutionEngineEnvironmentPostProcessor processor = new ExecutionEngineEnvironmentPostProcessor();

	@ParameterizedTest
	@ValueSource(strings = {"mysql", "MySQL", "", " ", "postgres", "duck"})
	void theMysqlEngineLeavesTheEnvironmentUntouched(String value) {
		MockEnvironment environment = new MockEnvironment().withProperty(ExecutionEngine.PROPERTY, value);

		processor.postProcessEnvironment(environment, null);

		assertFalse(environment.getPropertySources().contains(ExecutionEngineEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
		assertFalse(environment.containsProperty(EXCLUDE));
	}

	@Test
	void anAbsentPropertyIsTheMysqlEngine() {
		MockEnvironment environment = new MockEnvironment();

		processor.postProcessEnvironment(environment, null);

		assertFalse(environment.containsProperty(EXCLUDE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"duckdb", "DuckDB", "DUCKDB", " duckdb "})
	void anotherEngineExcludesBootsDatabaseAutoConfiguration(String value) {
		MockEnvironment environment = new MockEnvironment().withProperty(ExecutionEngine.PROPERTY, value);

		processor.postProcessEnvironment(environment, null);

		String excluded = environment.getProperty(EXCLUDE);
		assertNotNull(excluded, "no exclusions for '" + value + "'");
		assertTrue(excluded.contains(".DataSourceAutoConfiguration"), excluded);
		assertTrue(excluded.contains(".DataSourceTransactionManagerAutoConfiguration"), excluded);
		assertTrue(excluded.contains(".HibernateJpaAutoConfiguration"), excluded);
		assertTrue(excluded.contains(".DataJpaRepositoriesAutoConfiguration"), excluded);
	}

	@Test
	void anExclusionTheDeploymentAlreadyMadeIsKept() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(ExecutionEngine.PROPERTY, ExecutionEngine.DUCKDB)
				.withProperty(EXCLUDE, "com.example.SomethingAutoConfiguration");

		processor.postProcessEnvironment(environment, null);

		String excluded = environment.getProperty(EXCLUDE);
		assertTrue(excluded.contains("com.example.SomethingAutoConfiguration"), excluded);
		assertTrue(excluded.contains(".DataSourceAutoConfiguration"), excluded);
	}

	@Test
	void indexedExclusionsTheDeploymentAlreadyMadeAreKept() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(ExecutionEngine.PROPERTY, ExecutionEngine.DUCKDB)
				.withProperty(EXCLUDE + "[0]", "com.example.FirstAutoConfiguration")
				.withProperty(EXCLUDE + "[1]", "com.example.SecondAutoConfiguration");

		processor.postProcessEnvironment(environment, null);

		// Read the way Spring Boot reads it: bound, so the plain property this
		// adds has to carry the indexed entries it now takes precedence over.
		List<String> excluded = Binder.get(environment).bind(EXCLUDE, Bindable.listOf(String.class)).get();
		assertTrue(excluded.contains("com.example.FirstAutoConfiguration"), excluded.toString());
		assertTrue(excluded.contains("com.example.SecondAutoConfiguration"), excluded.toString());
		assertTrue(excluded.stream().anyMatch(name -> name.endsWith(".DataSourceAutoConfiguration")), excluded.toString());
	}
}
