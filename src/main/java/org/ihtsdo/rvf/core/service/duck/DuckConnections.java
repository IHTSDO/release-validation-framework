package org.ihtsdo.rvf.core.service.duck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * How the engine configures every DuckDB connection it opens.
 *
 * <p>Assertion statements, including those of assertion packs, execute on these connections.
 * {@link #lockDown} restricts them to the files a run needs, so a statement cannot read or
 * write anything else on the server, fetch over the network or install an extension.
 */
final class DuckConnections {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckConnections.class);

	private DuckConnections() {
	}

	/**
	 * The CPUs and memory DuckDB may use.
	 *
	 * <p>DuckDB sizes its thread pool from the host's core count and ignores CPU affinity and
	 * container quotas; the JVM's {@code availableProcessors()} observes both. A container
	 * limited to 2 CPUs on a 64-core node would otherwise run 64 threads inside a 2-CPU quota.
	 *
	 * @param threads     {@code rvf.duck.threads}; 0 or less uses the CPUs available to the JVM
	 * @param memoryLimit {@code rvf.duck.memory.limit}, with a unit; blank keeps DuckDB's default
	 */
	record Limits(int threads, String memoryLimit) {

		Limits {
			memoryLimit = requireUnit(memoryLimit);
		}

		void apply(Connection connection) throws SQLException {
			int effectiveThreads = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET threads = " + effectiveThreads);
				if (!memoryLimit.isEmpty()) {
					statement.execute("SET memory_limit = '" + memoryLimit + "'");
				}
			}
			LOGGER.info("DuckDB resource limits: threads={}{}", effectiveThreads,
					memoryLimit.isEmpty() ? "" : ", memory_limit=" + memoryLimit);
		}

		/**
		 * Rejects a memory limit without a unit at startup. DuckDB requires one, and would
		 * otherwise refuse it only when a run opens its connection, after the release is unpacked.
		 */
		private static String requireUnit(String memoryLimit) {
			if (memoryLimit == null || memoryLimit.isBlank()) {
				return "";
			}
			String trimmed = memoryLimit.trim();
			if (!trimmed.matches("(?i)\\d+(\\.\\d+)?\\s*(k|m|g|t)(i?b)?")) {
				throw new IllegalArgumentException("rvf.duck.memory.limit must carry a unit, e.g. 4GB or 4GiB - got '"
						+ memoryLimit + "'. DuckDB accepts KB/MB/GB/TB (1000^i) and KiB/MiB/GiB/TiB (1024^i).");
			}
			return trimmed;
		}
	}

	/**
	 * Confines a connection to {@code allowedDirectories} and freezes its configuration.
	 *
	 * <p>File access ({@code read_csv}, {@code read_text}, {@code COPY}, {@code ATTACH}) is
	 * refused outside those directories, as are network reads and installing or auto-loading
	 * an extension. The configuration is then locked, so a statement cannot undo any of it.
	 * The search path can still be set, which is how a run moves between schemas; every other
	 * setting must be applied before this.
	 */
	static void lockDown(Connection connection, Collection<Path> allowedDirectories) throws SQLException {
		String allowed = allowedDirectories.stream()
				.map(dir -> "'" + dir.toAbsolutePath().normalize().toString().replace("'", "''") + "'")
				.collect(Collectors.joining(", "));
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET allowed_directories = [" + allowed + "]");
			statement.execute("SET enable_external_access = false");
			statement.execute("SET autoinstall_known_extensions = false");
			statement.execute("SET autoload_known_extensions = false");
			statement.execute("SET lock_configuration = true");
		}
	}

	/**
	 * Opens an in-memory database, so a DuckDB native library that cannot be loaded stops the
	 * application at startup instead of failing every validation.
	 *
	 * <p>The DuckDB JDBC driver extracts its native library to {@code java.io.tmpdir} and loads
	 * it from there; it has no setting for another directory. On a host whose temporary
	 * directory is mounted {@code noexec} the load fails.
	 *
	 * @throws IllegalStateException if the library cannot be loaded or a database opened
	 */
	static void requireNativeLibrary() {
		try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
				Statement statement = connection.createStatement()) {
			statement.execute("SELECT 1");
		} catch (SQLException | LinkageError | RuntimeException e) {
			throw new IllegalStateException("The DuckDB engine cannot open a database: " + e
					+ ". The DuckDB JDBC driver extracts its native library to java.io.tmpdir ("
					+ System.getProperty("java.io.tmpdir") + ") and loads it from there; if that "
					+ "directory is mounted noexec, start the JVM with -Djava.io.tmpdir pointing "
					+ "to a writable directory that allows execution (see docs/duckdb-engine.md).", e);
		}
	}
}
