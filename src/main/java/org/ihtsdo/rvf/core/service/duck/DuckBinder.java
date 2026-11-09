package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.otf.RF2Constants;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes one run's values into a precompiled statement. Textual only.
 *
 * <p>The Java half of the publisher's {@code bind()}. The values and defaults are those
 * {@code AssertionExecutionService.transformSql} substitutes on the MySQL engine. The
 * sentinel strings come from the store rather than from constants here, so the publisher
 * and the binder cannot drift apart.
 */
public final class DuckBinder {

	/**
	 * A bound statement, or the release it needs that this run does not have.
	 *
	 * <p>{@link #sql()} is null exactly when {@link #skippedFor()} is set.
	 */
	public record Bound(String sql, String skippedFor) {

		public boolean isSkipped() {
			return skippedFor != null;
		}
	}

	/**
	 * The qa_result table as a statement names it before binding: the publisher rewrites
	 * the bare word rather than a sentinel, and so does this.
	 */
	public static final Pattern QA_RESULT = Pattern.compile("\\bqa_result\\b");

	private static final String PREVIOUS = "<PREVIOUS>";
	private static final String DEPENDENCY = "<DEPENDENCY>";

	private final Map<String, String> sentinels;
	private final Config config;

	/**
	 * The run inputs, separate from {@code MysqlExecutionConfig} so the binder needs neither
	 * a Spring context nor a database.
	 *
	 * @param previousSchema   null when the run has no previous release
	 * @param dependencySchema null when the run has no dependency release
	 * @param emptySchema      a schema holding every declared table with no rows, or null;
	 *                         see {@link #bind}
	 */
	public record Config(long runId, String prospectiveSchema, String previousSchema,
			String dependencySchema, String qaResultTable, String defaultModuleId,
			Collection<String> includedModules, String version, String emptySchema) {

		/** For callers with no empty schema to offer. */
		public Config(long runId, String prospectiveSchema, String previousSchema,
				String dependencySchema, String qaResultTable, String defaultModuleId,
				Collection<String> includedModules, String version) {
			this(runId, prospectiveSchema, previousSchema, dependencySchema, qaResultTable,
					defaultModuleId, includedModules, version, null);
		}

		/** {@code AssertionExecutionService}'s fallbacks for a missing module id and version. */
		Config withRvfDefaults() {
			return new Config(runId, prospectiveSchema, previousSchema, dependencySchema,
					qaResultTable,
					// A blank module id lands in a BIGINT column and fails the assertion.
					StringUtils.hasLength(defaultModuleId) ? defaultModuleId : RF2Constants.SCTID_CORE_MODULE,
					includedModules,
					// A blank version would make every `effectivetime = '<VERSION>'` match nothing.
					StringUtils.hasLength(version) ? version : "NOT_SUPPLIED",
					emptySchema);
		}
	}

	public DuckBinder(Map<String, String> sentinels, Config config) {
		this.sentinels = sentinels;
		this.config = config.withRvfDefaults();
	}

	/**
	 * Binds one statement of the assertion {@code assertionId}.
	 *
	 * <p>A statement still naming {@code <PREVIOUS>} or {@code <DEPENDENCY>} after binding
	 * needs a release this run does not hold, and is skipped, per statement, so an
	 * assertion's other statements still run; an assertion whose every {@code qa_result}
	 * writer was skipped reports "not run", and one with only some of them skipped carries a
	 * note saying so. The MySQL engine instead binds an empty schema for a missing release and
	 * runs the statement, so it reports a comparison against no rows as a result.
	 *
	 * <p>Two exceptions. A run that declares there is no previous release (a first-time
	 * release) binds {@code <PREVIOUS>} to an empty schema, as MySQL does: there, "nothing was
	 * there before" is the answer. And a statement whose every use of {@code <DEPENDENCY>} is
	 * an anti-join ({@code LEFT JOIN ... WHERE alias.x IS NULL}, or
	 * {@code NOT EXISTS (SELECT ... FROM <DEPENDENCY>.t ...)}) asks "is this not in the
	 * dependency?", which an empty release answers, as MySQL's empty schema does: yes. It is
	 * bound to the empty schema and runs. A statement that compares against the dependency is
	 * still skipped, and so is one that needs a previous release the run neither supplied nor
	 * declared absent: "there is no before" and "nothing was there before" are different
	 * answers.
	 */
	public Bound bind(String statement, String assertionId) {
		String s = statement;
		for (Map.Entry<String, String> e : sentinels.entrySet()) {
			s = s.replace(e.getValue(), valueFor(e.getKey(), assertionId));
		}
		for (String release : List.of(PREVIOUS, DEPENDENCY)) {
			if (!s.contains(release)) {
				continue;
			}
			String emptySchema = DEPENDENCY.equals(release) ? config.emptySchema() : null;
			if (emptySchema != null && onlyAntiJoined(s, release)) {
				s = s.replace(release, emptySchema);
				continue;
			}
			return new Bound(null, release);
		}
		s = padSpaceTerms(s);
		return new Bound(QA_RESULT.matcher(s).replaceAll(config.qaResultTable()), null);
	}

	private static final Pattern TERM_REFERENCE = Pattern.compile("\\b(\\w+)\\.term\\b");

	private static final Pattern GROUPS_BY_TERM = Pattern.compile(
			"GROUP\\s+BY[^;]*\\bterm\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern DERIVED_TABLE = Pattern.compile("\\)\\s+AS\\s+\\w+", Pattern.CASE_INSENSITIVE);

	/**
	 * Makes a statement that groups by {@code term} ignore trailing spaces, as MySQL's
	 * PAD SPACE collation does.
	 *
	 * <p>MySQL groups {@code 'Exposure to vibration  '} with {@code 'Exposure to vibration '}
	 * (concept 703860006 in RVF's regression fixture) and reports the duplicate; DuckDB
	 * compares exactly. Every {@code alias.term} in the statement is wrapped, because a
	 * GROUP BY key and the SELECT expression over it must agree. Statements that group
	 * inside a derived table are left alone: rewriting the inner projection renames the
	 * column the outer query joins on. A no-op on data without trailing spaces.
	 *
	 * <p>Doing this with the parse tree, keeping the projection's name, belongs in the
	 * publisher; this keeps the two engines answering the same question until it does.
	 */
	private static String padSpaceTerms(String sql) {
		if (sql.toLowerCase(Locale.ROOT).contains("rtrim(")) {
			return sql;
		}
		if (GROUPS_BY_TERM.matcher(sql).find() && !DERIVED_TABLE.matcher(sql).find()) {
			return TERM_REFERENCE.matcher(sql).replaceAll("rtrim($1.term)");
		}
		return sql;
	}

	/**
	 * True when every mention of {@code release} is an anti-join, so an empty release answers
	 * the statement as MySQL's empty schema does. Two shapes, and nothing else: the corpus is
	 * transpiled, so the shapes are uniform, and being permissive here would run a comparison
	 * against nothing.
	 *
	 * <p>{@code NOT x IN (SELECT ... FROM release ...)} would be answered the same way too (an
	 * empty list makes it true on both engines, even for a NULL {@code x}), but no statement
	 * uses it against the dependency, and recognising it means parsing its operand, so it
	 * stays skipped. {@code EXISTS}, {@code IN}, scalar subqueries and plain joins read the
	 * release's rows, and are skipped.
	 */
	private static boolean onlyAntiJoined(String sql, String release) {
		int mentions = 0;
		for (int i = sql.indexOf(release); i >= 0; i = sql.indexOf(release, i + 1)) {
			mentions++;
		}
		return mentions > 0 && leftAntiJoins(sql, release) + notExistsOver(sql, release) == mentions;
	}

	/** The {@code LEFT JOIN release.t AS alias} whose alias is then required to be NULL. */
	private static int leftAntiJoins(String sql, String release) {
		Matcher joins = Pattern.compile("LEFT\\s+JOIN\\s+" + Pattern.quote(release)
				+ "\\.\\w+\\s+AS\\s+(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
		int antiJoined = 0;
		while (joins.find()) {
			String alias = joins.group(1);
			if (Pattern.compile("\\b" + Pattern.quote(alias) + "\\.\\w+\\s+IS\\s+NULL",
					Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
				antiJoined++;
			}
		}
		return antiJoined;
	}

	private static final Pattern NOT_EXISTS = Pattern.compile("\\bNOT\\s+EXISTS\\s*\\(", Pattern.CASE_INSENSITIVE);

	/** Text ending in a NOT that would negate the NOT EXISTS after it. */
	private static final Pattern NEGATED = Pattern.compile("\\bNOT\\s*(?:\\(\\s*)?$", Pattern.CASE_INSENSITIVE);

	/**
	 * The {@code NOT EXISTS} subqueries that read {@code release} alone:
	 * {@code NOT EXISTS (SELECT 1 FROM release.t AS d WHERE ...)}. Such a subquery yields no
	 * row from an empty table, so the NOT EXISTS holds. The select list must be plain columns
	 * or constants, because an ungrouped aggregate yields one row even from an empty table,
	 * and a set operation or HAVING could yield rows the table did not.
	 */
	private static int notExistsOver(String sql, String release) {
		Pattern readsOnlyRelease = Pattern.compile("\\s*SELECT\\s+(?:DISTINCT\\s+)?[\\w.*]+(?:\\s*,\\s*[\\w.*]+)*"
				+ "\\s+FROM\\s+" + Pattern.quote(release) + "\\.\\w+(?:\\s+(?:AS\\s+)?(?!WHERE\\b)\\w+)?"
				+ "(?:\\s+WHERE\\b(?:(?!\\b(?:UNION|INTERSECT|EXCEPT|HAVING)\\b).)*)?\\s*",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = NOT_EXISTS.matcher(sql);
		int count = 0;
		while (m.find()) {
			if (NEGATED.matcher(sql).region(0, m.start()).find()) {
				continue;
			}
			int close = closingParen(sql, m.end() - 1);
			if (close >= 0 && readsOnlyRelease.matcher(outermostLevel(sql.substring(m.end(), close))).matches()) {
				count++;
			}
		}
		return count;
	}

	/** The index of the parenthesis closing the one at {@code open}, or -1. Skips string literals. */
	private static int closingParen(String sql, int open) {
		int depth = 0;
		boolean quoted = false;
		for (int i = open; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '\'') {
				quoted = !quoted;
			} else if (!quoted && c == '(') {
				depth++;
			} else if (!quoted && c == ')' && --depth == 0) {
				return i;
			}
		}
		return -1;
	}

	/** {@code sql} with every parenthesised part and string literal emptied, leaving its outermost level. */
	private static String outermostLevel(String sql) {
		StringBuilder out = new StringBuilder(sql.length());
		int depth = 0;
		boolean quoted = false;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '\'') {
				quoted = !quoted;
				if (depth == 0) {
					out.append(c);
				}
			} else if (quoted) {
				continue;
			} else if (c == '(') {
				if (depth++ == 0) {
					out.append(c);
				}
			} else if (c == ')') {
				if (--depth == 0) {
					out.append(c);
				}
			} else if (depth == 0) {
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * The run's value for a placeholder.
	 *
	 * <p>A release this run does not hold resolves back to its own placeholder text, which
	 * is what {@link #bind} detects to skip the statement.
	 */
	private String valueFor(String placeholder, String assertionId) {
		return switch (placeholder) {
			case "<RUNID>" -> String.valueOf(config.runId());
			case "<ASSERTIONUUID>" -> assertionId;
			case "<PROSPECTIVE>", "<TEMP>" -> config.prospectiveSchema();
			case PREVIOUS -> orPlaceholder(config.previousSchema(), PREVIOUS);
			case DEPENDENCY -> orPlaceholder(config.dependencySchema(), DEPENDENCY);
			case "<MODULEID>" -> config.defaultModuleId();
			case "<VERSION>" -> config.version();
			// The same substitution under two names. <INCLUDED_MODULES> also has a "no
			// filter" value, because assertions branch on `'NULL' = '<INCLUDED_MODULES>'`.
			case "<MODULEIDS>" -> includedModules();
			case "<INCLUDED_MODULES>" -> includedModules().isEmpty() ? "NULL" : includedModules();
			default -> "";
		};
	}

	private String includedModules() {
		return config.includedModules() == null ? "" : String.join(",", config.includedModules());
	}

	private static String orPlaceholder(String schema, String placeholder) {
		return Optional.ofNullable(schema).filter(StringUtils::hasLength).orElse(placeholder);
	}
}
