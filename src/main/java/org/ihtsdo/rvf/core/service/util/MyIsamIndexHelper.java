package org.ihtsdo.rvf.core.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Semaphore;

/**
 * Re-enables MyISAM keys after a bulk load or copy, and repairs the table when MySQL
 * leaves the key file unusable.
 * <p>
 * MyISAM rebuilds every index when keys are enabled. Doing that for many tables at once
 * overflows the shared key cache, and the rebuild then fails with "Incorrect key file".
 * MySQL frequently reports that failure as a warning rather than an exception, so a caller
 * that only catches {@link SQLException} carries on with a broken table and the error only
 * surfaces later when an assertion queries it. All callers share one semaphore so that file
 * loads and table copies running at the same time are throttled together.
 */
public final class MyIsamIndexHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(MyIsamIndexHelper.class);
	private static final int MAX_CONCURRENT_INDEX_REBUILDS = 2;
	private static final Semaphore INDEX_REBUILDS = new Semaphore(MAX_CONCURRENT_INDEX_REBUILDS, true);
	private static final String INCORRECT_KEY_FILE = "Incorrect key file";

	private MyIsamIndexHelper() {
	}

	/**
	 * Runs {@code ALTER TABLE <tableName> ENABLE KEYS} and repairs the table if the key file
	 * was left unusable, whether MySQL reported that as an exception or as a warning.
	 *
	 * @throws SQLException if enabling keys fails for another reason, or the repair does not succeed
	 * @throws InterruptedException if interrupted while waiting for a rebuild slot
	 */
	public static void enableKeys(Statement statement, String tableName) throws SQLException, InterruptedException {
		try {
			INDEX_REBUILDS.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw e;
		}
		try {
			statement.clearWarnings();
			statement.execute("ALTER TABLE " + tableName + " ENABLE KEYS");
			if (isIncorrectKeyFile(statement.getWarnings())) {
				LOGGER.warn("Key file for {} was left unusable after enabling keys. Repairing the table.", tableName);
				repairTable(statement, tableName);
			}
		} catch (SQLException e) {
			if (!isIncorrectKeyFile(e)) {
				throw e;
			}
			LOGGER.warn("Key file for {} could not be rebuilt ({}). Repairing the table.", tableName, e.getMessage());
			repairTable(statement, tableName);
		} finally {
			INDEX_REBUILDS.release();
		}
	}

	private static void repairTable(Statement statement, String tableName) throws SQLException {
		try (ResultSet result = statement.executeQuery("REPAIR TABLE " + tableName)) {
			String status = null;
			while (result.next()) {
				status = result.getString("Msg_text");
			}
			if (!"OK".equalsIgnoreCase(status)) {
				throw new SQLException("REPAIR TABLE " + tableName + " failed: " + status);
			}
		}
	}

	private static boolean isIncorrectKeyFile(SQLException exception) {
		while (exception != null) {
			String message = exception.getMessage();
			if (message != null && message.contains(INCORRECT_KEY_FILE)) {
				return true;
			}
			exception = exception.getNextException();
		}
		return false;
	}
}
