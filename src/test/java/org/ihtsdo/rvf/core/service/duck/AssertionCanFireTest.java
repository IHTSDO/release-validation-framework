package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertions whose SQL cannot produce a finding for any release, so they always pass.
 *
 * <p>The shapes detected:
 * <pre>
 *   NOT EXISTS(SELECT f(x) = 'y')                 -- no FROM: always one row
 *   NOT EXISTS(SELECT COUNT(1) FROM t)            -- ungrouped aggregate: one row even if t is empty
 *   NOT p(id, 1) AND p(id, 1)                     -- a predicate required and forbidden together
 *   x = NULL                                      -- never true
 * </pre>
 * The store is the compiled form of what runs, so it is where such an assertion can be
 * caught mechanically. The bundled store has none; a corpus change that adds one fails here
 * with the assertion named.
 */
class AssertionCanFireTest {

	/** {@code NOT EXISTS ( SELECT}; the subquery's extent is found by matching parentheses. */
	private static final Pattern NOT_EXISTS_SELECT =
			Pattern.compile("NOT\\s+EXISTS\\s*\\(\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern FROM = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

	/** Anything that can reduce a {@code FROM}-less SELECT to zero rows. */
	private static final Pattern FILTER =
			Pattern.compile("\\bWHERE\\b|\\bHAVING\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern AGGREGATE =
			Pattern.compile("\\b(COUNT|SUM|MAX|MIN|AVG)\\s*\\(", Pattern.CASE_INSENSITIVE);

	private static final Pattern GROUPED =
			Pattern.compile("\\bGROUP\\s+BY\\b|\\bHAVING\\b", Pattern.CASE_INSENSITIVE);

	/** A comparison to NULL is NULL, never true. */
	private static final Pattern NULL_COMPARISON =
			Pattern.compile("([\\w.]+)\\s*(?:=|<>|!=)\\s*\\(?\\s*NULL\\s*\\)?", Pattern.CASE_INSENSITIVE);

	/**
	 * One of the corpus's predicate functions, required and forbidden as adjacent conjuncts.
	 *
	 * <p>Only adjacent: the same predicate legitimately appears on both sides of an OR or in
	 * separate subqueries, and two calls joined by nothing but {@code AND} span neither.
	 */
	private static final Pattern ADJACENT_CONTRADICTION = Pattern.compile(
			"(NOT\\s+)?(\\b(?:is\\w+_cr(?:_refset)?|get_cr_\\w+)\\s*\\([^()]*\\))"
					+ "\\s+AND\\s+"
					+ "(NOT\\s+)?(\\b(?:is\\w+_cr(?:_refset)?|get_cr_\\w+)\\s*\\([^()]*\\))",
			Pattern.CASE_INSENSITIVE);

	@Test
	void noBundledAssertionIsStructurallyUnableToFire() throws Exception {
		DuckStore store = bundledStore();
		assertTrue(store.assertions().size() > 300, "the bundled store is the full corpus");
		List<String> dead = new ArrayList<>();
		for (DuckStore.StoredAssertion assertion : store.assertions().values()) {
			String reason = whyItCannotFire(String.join(" ", assertion.statements()));
			if (reason != null) {
				dead.add(assertion.file() + " - " + reason);
			}
		}
		assertTrue(dead.isEmpty(),
				() -> dead.size() + " assertion(s) cannot report a finding for any release,"
						+ " so they pass unconditionally:\n  " + String.join("\n  ", dead)
						+ "\nFix the SQL in the assertion corpus.");
	}

	/** The detector must be shown to detect, or the test above proves nothing. */
	@Test
	void theDetectorCatchesTheContradictionAndTheNullComparison() {
		String contradiction = "INSERT INTO qa_result SELECT id FROM prospective.concept_active"
				+ " WHERE NOT isActiveMemberOf_cr_refset(id, 111) AND"
				+ " isActiveMemberOf_cr_refset(id, 111) AND active = 1";
		assertTrue(whyItCannotFire(contradiction) != null
						&& whyItCannotFire(contradiction).contains("adjacent"),
				"a predicate required and forbidden as adjacent conjuncts has to be reported");

		String nullCompare = "INSERT INTO qa_result SELECT id FROM prospective.values_active"
				+ " WHERE val.typeid = (null) AND val.value IS NULL";
		assertTrue(whyItCannotFire(nullCompare) != null
						&& whyItCannotFire(nullCompare).contains("NULL"),
				"a comparison to NULL has to be reported");
	}

	/** Shapes the contradiction check must not flag: both can fire. */
	@Test
	void theContradictionCheckToleratesOrsAndSubqueries() {
		String acrossOr = "SELECT id FROM prospective.concept_active WHERE"
				+ " (NOT isActiveMemberOf_cr_refset(id, 111) OR isActiveMemberOf_cr_refset(id, 111))"
				+ " AND active = 1";
		assertTrue(whyItCannotFire(acrossOr) == null,
				"either side of an OR is satisfiable, so this is not a contradiction");

		String acrossSubquery = "SELECT id FROM prospective.concept_active a WHERE"
				+ " NOT isActiveMemberOf_cr_refset(a.id, 111) AND EXISTS(SELECT 1 FROM"
				+ " prospective.relationship_active WHERE isActiveMemberOf_cr_refset(a.id, 111))";
		assertTrue(whyItCannotFire(acrossSubquery) == null,
				"a subquery is a separate scope, not the same conjunction");
	}

	@Test
	void theDetectorCatchesBothShapes() {
		String fromLess = "INSERT INTO qa_result (details) SELECT 'x' FROM (SELECT 1 FROM dual"
				+ " WHERE NOT EXISTS(SELECT GET_CR_ADRS_PT(1) = 'a name')) AS query";
		assertTrue(whyItCannotFire(fromLess) != null,
				"a NOT EXISTS over a FROM-less SELECT has to be reported");

		String ungrouped = "INSERT INTO qa_result (details) SELECT 'x' FROM (SELECT '0' FROM dual"
				+ " WHERE NOT EXISTS(SELECT COUNT(1) FROM prospective.ccsRefset_f)) AS query";
		assertTrue(whyItCannotFire(ungrouped) != null
						&& whyItCannotFire(ungrouped).contains("ungrouped aggregate"),
				"a NOT EXISTS over COUNT with no GROUP BY has to be reported");
	}

	/** A WHERE needs no FROM: {@code SELECT 1 WHERE 1=0} yields no row, so NOT EXISTS over it can hold. */
	@Test
	void aFromLessSelectWithAWhereIsSatisfiable() {
		String satisfiable = "INSERT INTO qa_result SELECT 1 FROM dual"
				+ " WHERE NOT EXISTS(SELECT 1 WHERE 1=0)";
		assertTrue(whyItCannotFire(satisfiable) == null,
				"a WHERE can reduce a FROM-less SELECT to zero rows, so this one"
						+ " can fire and must not be reported");
	}

	/** Shapes the bundled corpus uses that must not be flagged. */
	@Test
	void theDetectorPassesHealthySql() {
		String correlated = "INSERT INTO qa_result SELECT a.id FROM prospective.concept_s a"
				+ " WHERE NOT EXISTS(SELECT 1 FROM prospective.description_s b"
				+ " WHERE b.conceptid = a.id)";
		assertTrue(whyItCannotFire(correlated) == null,
				"a correlated NOT EXISTS over a table is the normal way to write"
						+ " 'has no matching row' and must not be flagged");

		String outerAggregate = "INSERT INTO qa_result SELECT COUNT(*) FROM prospective.concept_s"
				+ " WHERE active = '1'";
		assertTrue(whyItCannotFire(outerAggregate) == null,
				"an aggregate in the outer projection is not inside a NOT EXISTS");

		String groupedInside = "INSERT INTO qa_result SELECT a.id FROM prospective.concept_s a"
				+ " WHERE NOT EXISTS(SELECT COUNT(1) FROM prospective.description_s b"
				+ " WHERE b.conceptid = a.id GROUP BY b.conceptid)";
		assertTrue(whyItCannotFire(groupedInside) == null,
				"an aggregate WITH a GROUP BY returns no row for an empty group,"
						+ " so that NOT EXISTS is a real test");
	}

	/**
	 * Why the statement cannot report a finding, or null when it can.
	 *
	 * <p>Only a {@code NOT EXISTS} subquery is examined: {@code EXISTS(SELECT 1)} is a normal
	 * constant true, and an ungrouped aggregate in the outer projection is a normal count.
	 */
	private static String whyItCannotFire(String sql) {
		Matcher m = NOT_EXISTS_SELECT.matcher(sql);
		while (m.find()) {
			String inner = subqueryBody(sql, m.end());
			if (inner == null) {
				continue;
			}
			if (!FROM.matcher(inner).find()) {
				// Unless a WHERE can filter its one row away.
				if (FILTER.matcher(inner).find()) {
					continue;
				}
				return "NOT EXISTS over a SELECT with no FROM and no WHERE, which returns"
						+ " exactly one row. A function call there does not change it:"
						+ " EXISTS counts rows yielded, not what the function reads or"
						+ " returns";
			}
			String projection = FROM.split(inner, 2)[0];
			if (AGGREGATE.matcher(projection).find() && !GROUPED.matcher(inner).find()) {
				return "NOT EXISTS over an ungrouped aggregate, which returns one row"
						+ " even when the table is empty";
			}
		}
		Matcher c = ADJACENT_CONTRADICTION.matcher(sql);
		while (c.find()) {
			String left = c.group(2).replaceAll("\\s+", "").toLowerCase();
			String right = c.group(4).replaceAll("\\s+", "").toLowerCase();
			boolean leftNegated = c.group(1) != null;
			boolean rightNegated = c.group(3) != null;
			if (left.equals(right) && leftNegated != rightNegated) {
				return "the same predicate is required and forbidden as adjacent"
						+ " conjuncts: " + c.group(2).trim();
			}
		}
		Matcher n = NULL_COMPARISON.matcher(sql);
		if (n.find()) {
			return n.group(1) + " is compared to NULL, which is never true -"
					+ " use IS NULL, or bind the value this was meant to test";
		}
		return null;
	}

	/** The text between {@code SELECT} and the paren that closes its subquery. */
	private static String subqueryBody(String sql, int afterSelect) {
		int depth = 1;
		for (int i = afterSelect; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
				if (depth == 0) {
					return sql.substring(afterSelect, i);
				}
			}
		}
		return null;
	}

	private static DuckStore bundledStore() throws IOException {
		try (InputStream in = AssertionCanFireTest.class.getResourceAsStream("/duck/store.json")) {
			assertNotNull(in, "the bundled store is on the classpath");
			return DuckStore.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}
}
