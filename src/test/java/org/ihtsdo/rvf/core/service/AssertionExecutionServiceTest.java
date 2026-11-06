package org.ihtsdo.rvf.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code ENGINE = MyISAM} append is a string predicate, so it is tested as
 * one. The case that matters is a statement that already names an engine: two
 * ENGINE clauses is a syntax error, the statement fails, and the table it would
 * have created never exists.
 */
class AssertionExecutionServiceTest {

	@Test
	void aPlainCreateTableGetsTheEngine() {
		assertTrue(AssertionExecutionService.needsMyIsamEngine(
				"create table tmp_x(id varchar(36), key idx_id (id))"));
	}

	@Test
	void aStatementThatAlreadyNamesAnEngineIsLeftAlone() {
		// scripts/resource/res-table-edited-concept.sql, verbatim. It is
		// manifest-declared, so RVF runs it on every validation.
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"create table if not exists res_concepts_edited(conceptid varchar(36), "
						+ "key idx_conceptid (conceptid)) ENGINE=MEMORY"),
				"appending a second ENGINE clause is a syntax error, so the table "
						+ "would never be created and every assertion selecting from "
						+ "res_concepts_edited would fail");
	}

	@Test
	void engineIsMatchedWhateverItsCase() {
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"create table t(id int) engine=memory"));
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"create table t(id int) Engine = InnoDB"));
	}

	@Test
	void theLikeAndAsFormsAreStillExcluded() {
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"create table tmp_copy like concept_s"));
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"create table tmp_sel as select id from concept_s"));
	}

	@Test
	void aStatementThatIsNotACreateTableIsLeftAlone() {
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"insert into qa_result values (1)"));
		// "ENGINE" appearing in a column name must not be what decides it, but a
		// non-create statement is excluded by the first condition anyway.
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"select engine_type from t"));
	}
}
