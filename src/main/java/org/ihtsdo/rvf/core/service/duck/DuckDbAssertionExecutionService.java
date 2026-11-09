package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes SQL assertions from a {@link DuckStore} on one DuckDB connection.
 *
 * <p>The DuckDB counterpart of {@code AssertionExecutionService}, and smaller, because the
 * per-run work that class does has moved to publish time: the statements arrive transpiled
 * and split, and every stored-procedure {@code CALL} has been unrolled into plain SQL over
 * the DDL's fixed table set. This binds sentinels and executes. It produces a
 * {@link TestRunItem} per assertion with the fields the MySQL path sets, so the two
 * engines' reports compare record for record.
 *
 * <p>Not a Spring bean: it is constructed per run against that run's connection.
 */
public class DuckDbAssertionExecutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckDbAssertionExecutionService.class);

	private final DuckStore store;
	private final DuckBinder binder;
	private final Connection connection;

	public DuckDbAssertionExecutionService(DuckStore store, DuckBinder binder, Connection connection) {
		this.store = store;
		this.binder = binder;
		this.connection = connection;
	}

	/**
	 * Schema setup did not complete, so no assertion result from this connection can be trusted.
	 *
	 * <p>Thrown rather than returned: a missing macro does not change the findings of the
	 * assertions that do not call it, so a run that carried on past a setup failure would
	 * look normal.
	 */
	public static class SetupFailedException extends RuntimeException {

		private final transient List<String> failures;

		SetupFailedException(List<String> failures) {
			super("DuckDB schema setup failed on " + failures.size()
					+ " statement(s), so no assertion result from this run is "
					+ "trustworthy. First: " + failures.get(0));
			this.failures = List.copyOf(failures);
		}

		public List<String> getFailures() {
			return failures;
		}
	}

	/**
	 * Applies the store's pre-requisite statements, then its ports, to the current schema.
	 *
	 * <p>Pre-requisites first: a port may read a table a pre-requisite builds. Every
	 * statement is attempted and every failure reported; the publisher emits no setup
	 * statement it expects to fail, so any failure is fatal.
	 *
	 * @return the number of statements applied
	 * @throws SetupFailedException if any setup statement fails
	 */
	public int prepareSchema() {
		List<String> failures = new ArrayList<>();
		int applied = 0;
		List<String> setup = new ArrayList<>(store.prerequisiteStatements());
		setup.addAll(store.ports());
		for (String raw : setup) {
			DuckBinder.Bound bound = binder.bind(raw, "prereq");
			if (bound.isSkipped()) {
				continue;
			}
			try (Statement st = connection.createStatement()) {
				st.execute(bound.sql());
				applied++;
			} catch (SQLException e) {
				failures.add(abbreviate(raw) + " -> " + e.getMessage());
			}
		}
		if (!failures.isEmpty()) {
			failures.forEach(f -> LOGGER.error("DuckDB setup statement failed: {}", f));
			throw new SetupFailedException(failures);
		}
		LOGGER.info("DuckDB schema prepared: {} statements applied", applied);
		return applied;
	}

	/**
	 * Runs the given assertions, in order, one at a time.
	 *
	 * <p>Serial: DuckDB parallelises within a query across all its threads, so running
	 * assertions concurrently mostly makes them contend, and makes per-assertion timings
	 * meaningless.
	 */
	public List<TestRunItem> execute(List<Assertion> assertions) {
		Map<String, DuckStore.StoredAssertion> stored = store.assertions();
		List<TestRunItem> results = new ArrayList<>();
		for (Assertion assertion : assertions) {
			results.add(executeOne(assertion, stored.get(String.valueOf(assertion.getUuid()))));
		}
		return results;
	}

	private TestRunItem executeOne(Assertion assertion, DuckStore.StoredAssertion stored) {
		long start = System.currentTimeMillis();
		TestRunItem item = new TestRunItem();
		item.setTestCategory(assertion.getKeywords());
		item.setAssertionText(assertion.getAssertionText());
		item.setAssertionUuid(assertion.getUuid());
		item.setSeverity(assertion.getSeverity());

		if (stored == null) {
			// Assertions are selected from the same store, so this is reachable only for an
			// assertion object that did not come from it.
			item.setFailureMessage("No precompiled statements in the DuckDB store for assertion "
					+ assertion.getUuid());
			item.setRunTime(System.currentTimeMillis() - start);
			return item;
		}

		// The UUID, where the MySQL path binds the numeric assertion id, which exists only as
		// a row in the assertion tables. Every use of <ASSERTIONUUID> in the corpus is a
		// quoted string, and qa_result.assertion_id is VARCHAR here.
		String assertionId = assertion.getUuid().toString();
		List<String> skippedFor = new ArrayList<>();
		// Only a statement that writes to qa_result can report a finding, so an assertion
		// whose qa_result writers were all skipped performed no check, however many of its
		// other statements ran.
		boolean findingsPossible = false;
		for (String raw : stored.statements()) {
			DuckBinder.Bound bound = binder.bind(raw, assertionId);
			if (bound.isSkipped()) {
				skippedFor.add(bound.skippedFor());
				continue;
			}
			if (DuckBinder.QA_RESULT.matcher(raw).find()) {
				findingsPossible = true;
			}
			try (Statement st = connection.createStatement()) {
				st.execute(bound.sql());
			} catch (SQLException e) {
				// A statement that reads what a skipped statement would have built fails on a
				// missing table; name the skipped release, which is the actual cause.
				String cause = skippedFor.isEmpty() ? ""
						: " after skipping " + skippedFor.size() + " statement(s) that require "
								+ String.join(", ", skippedFor.stream().distinct().toList())
								+ ", which was not supplied - a skipped statement builds what "
								+ "this one reads";
				item.setFailureMessage("Error executing DuckDB statement" + cause + ": " + e.getMessage());
				item.setRunTime(System.currentTimeMillis() - start);
				return item;
			}
		}
		if (!skippedFor.isEmpty()) {
			String requires = String.join(", ", skippedFor.stream().distinct().toList());
			if (findingsPossible) {
				// Some findings were possible, so the count stands; it is not a failure, and
				// not the whole check either.
				item.setNote("Partly run: " + skippedFor.size() + " of " + stored.statements().size()
						+ " statement(s) skipped because they require " + requires
						+ ", which was not supplied to this validation. The failure count covers "
						+ "only the statements that ran.");
			} else {
				item.setFailureMessage("Not run: requires " + requires + ", which was not supplied to this validation");
			}
		}
		item.setRunTime(System.currentTimeMillis() - start);
		return item;
	}

	private static String abbreviate(String sql) {
		String flat = sql.replace('\n', ' ').replace('\t', ' ').trim();
		return flat.length() <= 70 ? flat : flat.substring(0, 70) + "...";
	}
}
