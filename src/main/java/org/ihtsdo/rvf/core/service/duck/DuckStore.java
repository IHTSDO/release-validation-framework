package org.ihtsdo.rvf.core.service.duck;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The precompiled assertion store: RVF's SQL assertion corpus, already transpiled to DuckDB.
 *
 * <p>Transpilation depends only on the assertion text, so it is done once, at publish time,
 * by {@code tools/duck-publisher}. A store holds, per assertion, the DuckDB statements with
 * placeholder sentinels where a run's values go ({@link DuckBinder} substitutes them), plus
 * the assertion's manifest metadata. It also carries the corpus's group rules, so the store
 * alone answers which assertions a run's groups select. The service never parses SQL.
 *
 * <p>Read-only and immutable: load once, share across runs.
 */
public final class DuckStore {

	/** Bumped by the publisher when the store's shape changes incompatibly. */
	public static final int SUPPORTED_FORMAT_VERSION = 1;

	static final JsonMapper MAPPER = JsonMapper.builder().build();

	private final JsonNode root;

	private DuckStore(JsonNode root) {
		this.root = root;
	}

	public static DuckStore read(Path storeFile) throws IOException {
		return parse(Files.readString(storeFile));
	}

	public static DuckStore parse(String json) throws IOException {
		JsonNode root;
		try {
			root = MAPPER.readTree(json);
		} catch (JacksonException e) {
			throw new IOException("the assertion store is not valid JSON: " + e.getOriginalMessage(), e);
		}
		int version = root.path("formatVersion").asInt(-1);
		if (version != SUPPORTED_FORMAT_VERSION) {
			// Every field below is read by name with a default, so a store of another
			// shape would otherwise read as empty and report no findings.
			throw new IOException("store formatVersion " + version
					+ " is not supported (expected " + SUPPORTED_FORMAT_VERSION + ")");
		}
		checkTableColumns(root.path("tableColumns"));
		return new DuckStore(root);
	}

	private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	private static final Pattern COLUMN = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s+([A-Za-z]+)(?:\\(\\d+\\))?");

	/** The column types a table declaration may use. */
	private static final Set<String> COLUMN_TYPES = Set.of("BIGINT", "INTEGER", "SMALLINT", "TINYINT",
			"HUGEINT", "BOOLEAN", "DOUBLE", "FLOAT", "DECIMAL", "VARCHAR", "TEXT", "DATE", "TIMESTAMP");

	/**
	 * Refuses a {@code tableColumns} section that is anything but plain table names declaring
	 * {@code name TYPE} or {@code name TYPE(n)} columns.
	 *
	 * <p>The engine writes these strings into DDL and {@code read_csv} calls before a run's
	 * connection is locked down, so a value like {@code id BIGINT); COPY ...} would run as SQL
	 * with full file access. {@code TYPE(n, m)} is refused too: the column lists are split on
	 * commas.
	 */
	private static void checkTableColumns(JsonNode tableColumns) throws IOException {
		if (tableColumns.isMissingNode()) {
			return;
		}
		if (!tableColumns.isObject()) {
			throw new IOException("the store's tableColumns is not an object");
		}
		for (String table : tableColumns.propertyNames()) {
			if (!IDENTIFIER.matcher(table).matches()) {
				throw new IOException("tableColumns declares a table named \"" + table
						+ "\", which is not a plain identifier");
			}
			JsonNode columns = tableColumns.path(table);
			if (!columns.isString()) {
				throw new IOException("tableColumns declares " + table + " as " + columns + ", not a column list");
			}
			for (String column : columns.asString().split(",", -1)) {
				Matcher m = COLUMN.matcher(column.strip());
				if (!m.matches() || !COLUMN_TYPES.contains(m.group(2).toUpperCase(Locale.ROOT))) {
					throw new IOException("tableColumns declares " + table + " as \"" + columns.asString()
							+ "\"; each column must be `name TYPE` or `name TYPE(n)` with TYPE one of "
							+ new TreeSet<>(COLUMN_TYPES));
				}
			}
		}
	}

	/**
	 * This store's own identity, or empty for a store published without one.
	 *
	 * <p>The declared digest is recomputed rather than trusted, so an edited store cannot claim
	 * the identity of the one that was published.
	 *
	 * @throws IOException if the declared digest does not match the store's content
	 */
	public Optional<PackRecord> identity() throws IOException {
		JsonNode pack = root.path("pack");
		if (pack.isMissingNode() || pack.isNull()) {
			return Optional.empty();
		}
		String declared = pack.path("digest").asString("");
		String actual = assertionDigest();
		if (!declared.equals(actual)) {
			throw new IOException("store " + pack.path("name").asString("?") + "@"
					+ pack.path("version").asString("?") + " declares digest "
					+ declared + " but its content hashes to " + actual
					+ ". The store has been edited since it was published, so its "
					+ "identity does not describe what it would run.");
		}
		return Optional.of(new PackRecord(
				pack.path("name").asString(""),
				pack.path("version").asString(""),
				declared,
				corpusRef(),
				assertions().size()));
	}

	/** The corpus commit this store was published from, or empty if the publisher did not know it. */
	public String corpusRef() {
		return root.path("pack").path("corpusRef").asString("");
	}

	/**
	 * {@code sha256} over what this store would execute and select, as the publisher defines it.
	 *
	 * <p>The sorted {@code uuid \t source hash \t keywords \t severity \t text} lines,
	 * {@code \n--\n}, the sorted {@code prerequisite file \t hash} lines, {@code \n--\n}, the
	 * sorted {@code grouping file \t hash} lines. Over identities rather than the serialised
	 * JSON, so key order, formatting and a republish with another transpiler do not change it.
	 * The statements, ports and {@code tableColumns} are not covered.
	 */
	public String assertionDigest() {
		JsonNode assertions = root.path("assertions");
		List<String> lines = new ArrayList<>();
		for (String uuid : assertions.propertyNames()) {
			JsonNode a = assertions.path(uuid);
			lines.add(uuid + '\t' + a.path("sha256").asString("") + '\t' + a.path("keywords").asString("")
					+ '\t' + a.path("severity").asString("") + '\t' + a.path("text").asString(""));
		}
		Collections.sort(lines);
		StringBuilder material = new StringBuilder(String.join("\n", lines));
		material.append("\n--\n").append(String.join("\n", sortedFileHashes(root.path("prerequisites"))));
		material.append("\n--\n").append(String.join("\n", sortedFileHashes(root.path("grouping"))));
		return "sha256:" + sha256Hex(material.toString());
	}

	private static List<String> sortedFileHashes(JsonNode array) {
		List<String> lines = new ArrayList<>();
		for (JsonNode node : array) {
			lines.add(node.path("file").asString("") + '\t' + node.path("sha256").asString(""));
		}
		Collections.sort(lines);
		return lines;
	}

	static String sha256Hex(String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	JsonNode tree() {
		return root;
	}

	public int formatVersion() {
		return root.path("formatVersion").asInt(-1);
	}

	/** The literal a run id is substituted for. */
	public String runIdSentinel() {
		return root.path("runIdSentinel").asString("");
	}

	/** The table name {@code qa_result} is rewritten to per run. */
	public String qaResultToken() {
		return root.path("qaResultToken").asString("");
	}

	/** Every table the DDL declares, whether or not a release ships it. */
	public List<String> knownTables() {
		return strings(root.path("knownTables"));
	}

	/** The sqlglot version that compiled these statements. */
	public String transpilerVersion() {
		return root.path("generator").path("sqlglot").asString("");
	}

	/** SHA-256 of the publisher sources that produced this store; see {@code tools/duck-publisher}. */
	public String publisherIdentity() {
		return root.path("generator").path("publisher").asString("");
	}

	/**
	 * A store's identity: its name, version and digest, the assertion corpus commit it was
	 * published from (empty when the publisher did not know it) and its size.
	 */
	public record PackRecord(String name, String version, String digest, String corpusRef, int assertions) {
	}

	/**
	 * What produced this store: its {@link #identity()}, or nothing for a store published
	 * without one. This is what a validation report records.
	 */
	public List<PackRecord> provenance() throws IOException {
		return identity().map(List::of).orElseGet(List::of);
	}

	/** One assertion's precompiled statements and the metadata to report it. */
	public record StoredAssertion(String uuid, String file, String sha256, String text,
			String keywords, String severity, List<String> statements) {
	}

	/**
	 * {@code placeholder -> sentinel}, in the order the publisher applied them.
	 *
	 * <p>Order matters: one sentinel could otherwise be a prefix of another, and binding in a
	 * different order from the one that installed them can corrupt overlapping tokens.
	 */
	public Map<String, String> sentinels() {
		Map<String, String> out = new LinkedHashMap<>();
		for (JsonNode n : root.path("sentinels")) {
			out.put(n.path("placeholder").asString(""), n.path("sentinel").asString(""));
		}
		return Collections.unmodifiableMap(out);
	}

	/** Assertions by uuid, in store order. */
	public Map<String, StoredAssertion> assertions() {
		Map<String, StoredAssertion> out = new LinkedHashMap<>();
		JsonNode node = root.path("assertions");
		for (String uuid : node.propertyNames()) {
			JsonNode a = node.path(uuid);
			out.put(uuid, new StoredAssertion(uuid, a.path("file").asString(""),
					a.path("sha256").asString(""),
					a.path("text").asString(""), a.path("keywords").asString(""),
					a.path("severity").asString(""), strings(a.path("statements"))));
		}
		return Collections.unmodifiableMap(out);
	}

	/**
	 * Column definitions for every table the DDL declares, {@code table -> "col TYPE, col TYPE"}.
	 *
	 * <p>Used to create an empty table for a file the release does not ship, so an assertion
	 * that mentions it reports no findings instead of failing on a missing table.
	 */
	public Map<String, String> tableColumns() {
		Map<String, String> out = new LinkedHashMap<>();
		JsonNode node = root.path("tableColumns");
		for (String table : node.propertyNames()) {
			out.put(table, node.path(table).asString(""));
		}
		return Collections.unmodifiableMap(out);
	}

	/**
	 * DuckDB setup statements run once per schema before any assertion: macros standing in
	 * for MySQL functions the corpus calls ({@code substring_index}, {@code cleanExpression}).
	 */
	public List<String> ports() {
		return strings(root.path("ports"));
	}

	/** Pre-requisite statements, in file then statement order. Empty for the international corpus. */
	public List<String> prerequisiteStatements() {
		List<String> out = new ArrayList<>();
		for (JsonNode p : root.path("prerequisites")) {
			out.addAll(strings(p.path("statements")));
		}
		return Collections.unmodifiableList(out);
	}

	/** A corpus file carried verbatim: {@code groups.xml} or {@code policies.xml}. */
	public record GroupingFile(String file, String sha256, String text) {
	}

	/** The corpus's group rules, by file name. */
	public Map<String, GroupingFile> grouping() {
		Map<String, GroupingFile> out = new LinkedHashMap<>();
		for (JsonNode node : root.path("grouping")) {
			String file = node.path("file").asString("");
			out.put(file, new GroupingFile(file, node.path("sha256").asString(""),
					node.path("text").asString("")));
		}
		return Collections.unmodifiableMap(out);
	}

	/** Provenance for a log line: what built this store. */
	public String generatorDescription() {
		JsonNode g = root.path("generator");
		return g.isMissingNode() ? "unknown" : g.toString();
	}

	private static List<String> strings(JsonNode array) {
		List<String> out = new ArrayList<>();
		for (JsonNode n : array) {
			out.add(n.asString(""));
		}
		return Collections.unmodifiableList(out);
	}
}
