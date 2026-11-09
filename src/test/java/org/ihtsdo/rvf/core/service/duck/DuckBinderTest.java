package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The binder has to agree with two other implementations - MySqlQueryTransformer
 * and the publisher's own bind() - and disagreements are silent: a wrong value
 * still produces SQL that runs. So each case here pins one agreement.
 */
class DuckBinderTest {

	/** The sentinel table the publisher emits, in its order. */
	private static final Map<String, String> SENTINELS = Map.of(
			"<RUNID>", "424242424242424242",
			"<ASSERTIONUUID>", "rvfph_assertionuuid_",
			"<MODULEID>", "rvfph_moduleid_",
			"<MODULEIDS>", "rvfph_moduleids_",
			"<INCLUDED_MODULES>", "rvfph_included_modules_",
			"<VERSION>", "rvfph_version_",
			"<PROSPECTIVE>", "rvfph_prospective_",
			"<TEMP>", "rvfph_temp_",
			"<PREVIOUS>", "rvfph_previous_",
			"<DEPENDENCY>", "rvfph_dependency_");

	private static DuckBinder binder(String previous, String dependency) {
		return new DuckBinder(SENTINELS, new DuckBinder.Config(7L, "prospective",
				previous, dependency, "rvf_results.qa_result", null, List.of(), null));
	}

	/** As above, plus the zero-row schema an edition run can offer. */
	private static DuckBinder binderWithEmpty(String previous, String dependency) {
		return new DuckBinder(SENTINELS, new DuckBinder.Config(7L, "prospective",
				previous, dependency, "rvf_results.qa_result", null, List.of(), null, "empty"));
	}

	@Test
	void bindsTheRunsValuesIntoAStatement() {
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select 424242424242424242, "
						+ "'rvfph_assertionuuid_' from rvfph_prospective_.concept_s", "31");
		assertEquals("insert into rvf_results.qa_result select 7, '31' "
				+ "from prospective.concept_s", b.sql());
		assertFalse(b.isSkipped());
	}

	@Test
	void tempResolvesToTheProspectiveSchemaJustAsMySqlQueryTransformerDoes() {
		assertEquals("create table prospective.v_x as select 1",
				binder(null, null).bind("create table rvfph_temp_.v_x as select 1", "1").sql());
	}

	@Test
	void aStatementNeedingAnUnsuppliedReleaseIsSkippedNotFailed() {
		DuckBinder.Bound b = binder(null, null)
				.bind("select 1 from rvfph_previous_.concept_s", "1");
		assertTrue(b.isSkipped());
		assertEquals("<PREVIOUS>", b.skippedFor());
		assertNull(b.sql());
	}

	@Test
	void dependencyIsReportedSeparatelyFromPrevious() {
		// The distinction matters: an edition run (releaseAsAnEdition=true) supplies
		// a previous release but no dependency.
		DuckBinder.Bound b = binder("previous", null)
				.bind("select 1 from rvfph_previous_.concept_s "
						+ "join rvfph_dependency_.concept_s using (id)", "1");
		assertTrue(b.isSkipped());
		assertEquals("<DEPENDENCY>", b.skippedFor());
	}

	@Test
	void aSuppliedReleaseBinds() {
		assertEquals("select 1 from previous.concept_s",
				binder("previous", null).bind("select 1 from rvfph_previous_.concept_s", "1").sql());
	}

	@Test
	void moduleIdDefaultsToTheCoreModuleNotABlank() {
		// A blank here put '' into a BIGINT concept_id and killed the assertion
		// with "Could not convert string '' to INT64".
		assertEquals("select '900000000000207008'",
				binder(null, null).bind("select 'rvfph_moduleid_'", "1").sql());
	}

	@Test
	void versionDefaultsToNotSuppliedNotABlank() {
		assertEquals("select 'NOT_SUPPLIED'",
				binder(null, null).bind("select 'rvfph_version_'", "1").sql());
	}

	@Test
	void anExplicitModuleAndVersionWin() {
		DuckBinder b = new DuckBinder(SENTINELS, new DuckBinder.Config(1L, "p", null,
				null, "qa", "900000000000012004", List.of(), "20260831"));
		assertEquals("select '900000000000012004', '20260831'",
				b.bind("select 'rvfph_moduleid_', 'rvfph_version_'", "1").sql());
	}

	@Test
	void includedModulesIsNullWhenNoFilterIsConfigured() {
		// The assertions using it branch on `'NULL' = '<INCLUDED_MODULES>'`, so
		// NULL is the documented "no module filter" value - not an empty list.
		assertEquals("where 'NULL' = 'NULL'",
				binder(null, null).bind("where 'NULL' = 'rvfph_included_modules_'", "1").sql());
	}

	@Test
	void includedModulesJoinsOnCommas() {
		DuckBinder b = new DuckBinder(SENTINELS, new DuckBinder.Config(1L, "p", null,
				null, "qa", null, List.of("111", "222"), null));
		assertEquals("in (111,222)", b.bind("in (rvfph_included_modules_)", "1").sql());
		assertEquals("in (111,222)", b.bind("in (rvfph_moduleids_)", "1").sql());
	}

	@Test
	void moduleIdSentinelIsNotCorruptedByTheModuleIdsSentinel() {
		// Both sentinels end in "_" precisely because <MODULEID>'s would
		// otherwise be a prefix of <MODULEIDS>'s. Binding the first must not
		// leave the second half-substituted.
		DuckBinder b = new DuckBinder(SENTINELS, new DuckBinder.Config(1L, "p", null,
				null, "qa", "111", List.of("222"), null));
		assertEquals("one 111 many 222",
				b.bind("one rvfph_moduleid_ many rvfph_moduleids_", "1").sql());
	}

	@Test
	void qaResultIsRewrittenOnWordBoundariesOnly() {
		assertEquals("insert into rvf_results.qa_result select qa_resultant",
				binder(null, null).bind("insert into qa_result select qa_resultant", "1").sql());
	}

	@Test
	void anAbsentDependencyUsedOnlyAsAnAntiJoinRunsAgainstTheEmptySchema() {
		// The shape of all 16 release-type-snapshot-*-successive-states
		// statements. An empty relation and no relation give the SAME answer
		// here - the join adds nothing and the NULL test passes for every row -
		// so skipping it does not decline to answer, it answers a smaller
		// question and reports the count as if it were the whole one.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select d.id from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_dependency_.concept_s as e on d.id = e.id "
						+ "where e.id is null", "1");
		assertFalse(b.isSkipped(), "an anti-join against nothing is answerable");
		assertEquals("insert into rvf_results.qa_result select d.id from prospective.concept_s as d "
				+ "left join empty.concept_s as e on d.id = e.id where e.id is null", b.sql());
	}

	@Test
	void anAbsentDependencyUsedAsTheComparisonTargetIsStillSkipped() {
		// The shape of the 28 statements in
		// file-centric-snapshot-inactivated-component-module. Against an empty
		// schema every component would be reported, for an extension release
		// whose dependency was never supplied.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select a.id from rvfph_dependency_.concept_s as a "
						+ "where a.active = 1", "1");
		assertTrue(b.isSkipped());
		assertEquals("<DEPENDENCY>", b.skippedFor());
	}

	@Test
	void aHalfAntiJoinedDependencyIsSkippedRatherThanGuessed() {
		// One anti-join and one plain reference. The plain one needs real rows,
		// so the statement is not answerable - and a rule that recognised the
		// join it liked and ignored the rest would run it anyway.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select d.id from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_dependency_.concept_s as e on d.id = e.id "
						+ "where e.id is null and d.moduleid in "
						+ "(select moduleid from rvfph_dependency_.concept_s)", "1");
		assertTrue(b.isSkipped());
	}

	@Test
	void anAntiJoinedDependencyWithoutTheNullTestIsSkipped() {
		// A LEFT JOIN whose alias is never required to be NULL is an OUTER
		// LOOKUP, not an anti-join: the statement reads e's columns, and
		// against an empty schema they would all be NULL - a different answer,
		// quietly.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select d.id, e.moduleid from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_dependency_.concept_s as e on d.id = e.id", "1");
		assertTrue(b.isSkipped());
	}

	@Test
	void anAbsentDependencyUsedOnlyInNotExistsRunsAgainstTheEmptySchema() {
		// The shape of release-type-snapshot-inferred-relationship-successive-states and
		// release-type-delta-previous-snapshot-validation-inferred-relationship. Nothing
		// in an empty dependency matches, so NOT EXISTS holds for every row: MySQL's answer
		// against its empty dependency schema.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select a.id from rvfph_prospective_.relationship_s as a "
						+ "where a.active = '0' and not exists(select 1 from rvfph_dependency_.relationship_s as d "
						+ "where a.id = d.id and (a.active <> d.active or d.effectivetime < a.effectivetime))", "1");
		assertFalse(b.isSkipped(), "NOT EXISTS over nothing is answerable");
		assertEquals("insert into rvf_results.qa_result select a.id from prospective.relationship_s as a "
				+ "where a.active = '0' and not exists(select 1 from empty.relationship_s as d "
				+ "where a.id = d.id and (a.active <> d.active or d.effectivetime < a.effectivetime))", b.sql());
	}

	@Test
	void anAntiJoinAndANotExistsTogetherRun() {
		assertFalse(binderWithEmpty("previous", null).bind(
				"insert into qa_result select d.id from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_dependency_.concept_s as e on d.id = e.id "
						+ "where e.id is null and not exists (select * from rvfph_dependency_.description_s "
						+ "where conceptid = d.id)", "1").isSkipped());
	}

	@Test
	void aSubqueryThatReadsTheDependencysRowsIsStillSkipped() {
		String outer = "insert into qa_result select a.id from rvfph_prospective_.concept_s as a where ";
		Map<String, String> readsRows = Map.of(
				"EXISTS: true only if the dependency has the row",
				"exists (select 1 from rvfph_dependency_.concept_s as d where d.id = a.id)",
				"IN", "a.id in (select id from rvfph_dependency_.concept_s)",
				"NOT IN: an empty list makes it true, but the shape is not recognised",
				"not a.id in (select id from rvfph_dependency_.concept_s)",
				"a scalar subquery", "a.moduleid = (select moduleid from rvfph_dependency_.concept_s as d where d.id = a.id)",
				"NOT EXISTS over an ungrouped aggregate, one row even from an empty table",
				"not exists (select count(*) from rvfph_dependency_.concept_s as d where d.id = a.id)",
				"NOT EXISTS whose subquery also reads another table",
				"not exists (select 1 from rvfph_dependency_.concept_s as d join rvfph_prospective_.concept_s as p on p.id = d.id)",
				"NOT EXISTS whose subquery unions in other rows",
				"not exists (select id from rvfph_dependency_.concept_s as d where d.id = a.id "
						+ "union select id from rvfph_prospective_.concept_s as p where p.id = a.id)",
				"a negated NOT EXISTS", "not not exists (select 1 from rvfph_dependency_.concept_s as d where d.id = a.id)");
		readsRows.forEach((shape, where) -> {
			DuckBinder.Bound b = binderWithEmpty("previous", null).bind(outer + where, "1");
			assertTrue(b.isSkipped(), shape);
			assertEquals("<DEPENDENCY>", b.skippedFor(), shape);
		});
	}

	@Test
	void aNotExistsBesideAPlainJoinIsSkippedRatherThanGuessed() {
		// The NOT EXISTS alone would run; the join needs real rows.
		DuckBinder.Bound b = binderWithEmpty("previous", null).bind(
				"insert into qa_result select a.id from rvfph_prospective_.concept_s as a "
						+ "join rvfph_dependency_.concept_s as c on a.id = c.id "
						+ "where not exists (select 1 from rvfph_dependency_.description_s as d "
						+ "where d.conceptid = a.id)", "1");
		assertTrue(b.isSkipped());
		// So is one whose NOT EXISTS reads the dependency's rows deeper down.
		assertTrue(binderWithEmpty("previous", null).bind(
				"insert into qa_result select a.id from rvfph_prospective_.concept_s as a "
						+ "where not exists (select 1 from rvfph_dependency_.description_s as d "
						+ "where d.conceptid = a.id and d.moduleid in "
						+ "(select moduleid from rvfph_dependency_.concept_s))", "1").isSkipped());
	}

	@Test
	void anAbsentPreviousIsNeverStoodInFor() {
		// "This did not exist before" and "there is no before" are different
		// answers, and a first-time release is a real case the report already
		// handles by saying not-run.
		DuckBinder.Bound b = binderWithEmpty(null, "dependency").bind(
				"insert into qa_result select d.id from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_previous_.concept_s as e on d.id = e.id "
						+ "where e.id is null", "1");
		assertTrue(b.isSkipped());
		assertEquals("<PREVIOUS>", b.skippedFor());
	}

	@Test
	void withNoEmptySchemaOfferedTheOldSkipStands() {
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select d.id from rvfph_prospective_.concept_s as d "
						+ "left join rvfph_dependency_.concept_s as e on d.id = e.id "
						+ "where e.id is null", "1");
		assertTrue(b.isSkipped());
	}


	@Test
	void groupingOnTermIgnoresTrailingSpacesAsMySqlsCollationDoes() {
		// MySQL's collation is PAD SPACE: 'x ' and 'x  ' group together. DuckDB
		// compares exactly, so it found no duplicate where MySQL found one - on
		// an assertion that says active description terms are unique.
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select a.conceptid, 'Duplicate term=' || a.term "
						+ "from rvfph_prospective_.description_s as a where a.active = 1 "
						+ "group by a.conceptid, a.term having count(distinct a.id) > 1", "1");
		// Rewritten in BOTH places, because a GROUP BY key and the SELECT
		// expression over it have to agree or DuckDB rejects the statement.
		assertTrue(b.sql().contains("'Duplicate term=' || rtrim(a.term)"), b.sql());
		assertTrue(b.sql().contains("group by a.conceptid, rtrim(a.term)"), b.sql());
	}

	@Test
	void aTermJoinIsLeftAloneBecausePatternsCannotDoItSafely() {
		// MySQL's PAD SPACE applies here too, so this IS unreplicated exposure -
		// 21 statements in 19 assertions - and it is left that way on purpose.
		// Rewriting term comparisons made three assertions fail to EXECUTE:
		// unique-FSN, unique-fsn-case-insensitive-checking and
		// unique-preferred-terms each group by term inside a derived table and
		// then join the outer query on dup.term, so rewriting the inner
		// projection renames the column and the outer reference stops resolving.
		// Three assertions that do not run is a worse report than one false
		// negative. Doing it properly needs the parse tree, in the publisher.
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select a.id from rvfph_prospective_.description_s as a "
						+ "join rvfph_prospective_.description_s as b on a.term = b.term", "1");
		assertFalse(b.sql().contains("rtrim"), b.sql());
	}

	@Test
	void groupingInsideADerivedTableIsLeftAlone() {
		// The shape that broke: the inner projection would be renamed and
		// `dup.term` would no longer resolve.
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select e.id from rvfph_prospective_.description_s as e "
						+ "join (select a.term from rvfph_prospective_.description_s as a "
						+ "group by a.term having count(*) > 1) as dup on e.term = dup.term", "1");
		assertFalse(b.sql().contains("rtrim"), b.sql());
	}

	@Test
	void aStatementThatMerelyReportsATermIsLeftAlone() {
		// No grouping and no term join, so PAD SPACE cannot change the answer -
		// and rewriting it would change the reported text for nothing.
		DuckBinder.Bound b = binder("previous", null).bind(
				"insert into qa_result select a.id, 'term=' || a.term "
						+ "from rvfph_prospective_.description_s as a where a.active = 1", "1");
		assertTrue(b.sql().contains("'term=' || a.term"), b.sql());
		assertFalse(b.sql().contains("rtrim"), b.sql());
	}

	@Test
	void aStatementThatAlreadyTrimsIsNotTrimmedAgain() {
		// The corpus has two statements that trim maprule themselves. Wrapping
		// an existing trim would be pointless and could silence a check that
		// exists to find padding.
		String sql = "insert into qa_result select a.id from rvfph_prospective_.description_s as a "
				+ "join rvfph_prospective_.description_s as b on rtrim(a.term) = rtrim(b.term)";
		DuckBinder.Bound b = binder("previous", null).bind(sql, "1");
		assertFalse(b.sql().contains("rtrim(rtrim("), b.sql());
	}

}
