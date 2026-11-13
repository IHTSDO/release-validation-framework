package org.ihtsdo.rvf.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code ENGINE = MyISAM} append is a string predicate, so it is tested as
 * one. The case that matters is a statement that already names an engine:
 * MySQL accepts a repeated ENGINE option and applies the last one, so the
 * append would silently replace the engine the script asked for.
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
				"appending a second ENGINE clause would override the script's "
						+ "ENGINE=MEMORY, because MySQL applies the last one");
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
	void aColumnNamedLikeEngineDoesNotCountAsAnEngineOption() {
		assertTrue(AssertionExecutionService.needsMyIsamEngine(
				"create table tmp_e(id int, engine_type varchar(10))"));
	}

	@Test
	void aStatementThatIsNotACreateTableIsLeftAlone() {
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"insert into qa_result values (1)"));
		assertFalse(AssertionExecutionService.needsMyIsamEngine(
				"select engine_type from t"));
	}
}
