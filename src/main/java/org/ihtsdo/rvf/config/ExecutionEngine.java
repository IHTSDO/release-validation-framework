package org.ihtsdo.rvf.config;

/**
 * Which engine executes the SQL assertions of a validation, selected by
 * {@value #PROPERTY}.
 *
 * <p>{@value #MYSQL} is the default, and it is what any value other than
 * {@value #DUCKDB} selects: an absent property, a blank one, or a typo all keep
 * the MySQL beans. A misspelt engine name must not be able to start the service
 * with its database half missing.
 *
 * <p>Every check of the property, in a condition or anywhere else, goes through
 * {@link #isDuckDb(String)} so that no two parts of the application can disagree
 * about which engine is running.
 */
public final class ExecutionEngine {

	/** Property that selects the engine. */
	public static final String PROPERTY = "rvf.execution.engine";

	/** MySQL: the default. */
	public static final String MYSQL = "mysql";

	/**
	 * DuckDB: executes the SQL assertions from a precompiled assertion store against
	 * the release files, with no relational database. Its beans carry
	 * {@link ConditionalOnDuckDbEngine}.
	 */
	public static final String DUCKDB = "duckdb";

	/**
	 * Whether a property value selects the DuckDB engine: {@value #DUCKDB},
	 * ignoring case and surrounding whitespace. Everything else is MySQL.
	 */
	public static boolean isDuckDb(String value) {
		return value != null && DUCKDB.equalsIgnoreCase(value.trim());
	}

	private ExecutionEngine() {
	}
}
