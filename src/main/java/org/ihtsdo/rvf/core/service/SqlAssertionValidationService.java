package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;

/**
 * The SQL-assertion pass of a validation, whichever engine executes it.
 *
 * <p>{@link ValidationRunner} orchestrates five passes: structural, SQL
 * assertions, Drools, MRCM and traceability. Four of them work off the RF2
 * files and need no database. The SQL-assertion pass is the only one whose
 * implementation depends on where the release data is held, so it is the only
 * one behind an interface. Exactly one implementation is registered per engine:
 * {@link MysqlValidationService}, or
 * {@link org.ihtsdo.rvf.core.service.duck.DuckDbValidationService}.
 *
 * <p>Implementations acquire their own inputs from the run configuration,
 * because an engine decides for itself how to read the package (MySQL loads it
 * into a schema; DuckDB materialises the unpacked files into a per-run database).
 */
public interface SqlAssertionValidationService {

	/**
	 * Runs the SQL assertions and returns {@code statusReport}, filled in.
	 *
	 * <p>Declares {@code throws Exception} so that an implementation keeps its
	 * own checked exceptions. {@link ValidationRunner} submits this as a
	 * {@code Callable}, so a thrown exception surfaces from
	 * {@code Future.get()}.
	 *
	 * @param statusReport carries an empty {@code ValidationReport} that the
	 *                     implementation fills in rather than replaces; the
	 *                     runner merges it with the other passes' reports
	 */
	ValidationStatusReport runRF2Validations(ValidationRunConfig validationConfig,
			ValidationStatusReport statusReport) throws Exception;
}
