package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.service.util.RF2FileTableMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads an unpacked RF2 release into DuckDB tables: the DuckDB counterpart of
 * {@code ReleaseFileDataLoader}, which uses {@code load data local infile} into a MySQL schema.
 *
 * <ul>
 *   <li>The RF2 file name to table mapping is RVF's own {@link RF2FileTableMapper}, the code
 *       the MySQL path uses.</li>
 *   <li>Column names and DuckDB types come from the store's {@code tableColumns}, which the
 *       publisher derives from {@code create-tables-mysql.sql}. That keeps SCTIDs as BIGINT:
 *       read with inferred or VARCHAR types, id comparisons go through DOUBLE and stop
 *       matching above 2^53.</li>
 * </ul>
 *
 * <p>A file that maps to a table the store does not declare is not loaded: nothing can query it.
 */
public final class DuckMaterialiser {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckMaterialiser.class);

	/**
	 * The dominant join key per table. Loading in that order lets DuckDB's per-row-group
	 * min/max statistics prune as the MySQL schema's indexes do; correctness does not depend on it.
	 */
	private static final Map<String, String> SORT_KEY = Map.of(
			"concept_s", "id",
			"description_s", "conceptid",
			"textdefinition_s", "conceptid",
			"relationship_s", "sourceid",
			"stated_relationship_s", "sourceid",
			"relationship_concrete_values_s", "sourceid");

	private static final String DEFAULT_SORT = "referencedcomponentid";

	/**
	 * RF2 has no NULLs and MySQL loads an empty field as an empty string, so nothing may be
	 * read as NULL. The empty string cannot be the null string either: an equality join on an
	 * optional column would become NULL = NULL and never match. So the null string is a
	 * character no RF2 field holds.
	 */
	private static final String NO_NULLSTR = "\\x01";

	private DuckMaterialiser() {
	}

	public record Result(int tablesLoaded, int emptyFiles, int placeholders, long rows,
			long raggedRows, long millis) {
	}

	private record Loaded(long rows, long ragged) {
	}

	/**
	 * Loads every RF2 file under {@code releaseDir} into {@code schema}, then creates an empty
	 * table for every declared table the release does not ship, as RVF's MySQL schema has the
	 * whole table set whether or not a file exists.
	 */
	public static Result materialise(Connection con, Path releaseDir, String schema,
			Map<String, String> tableColumns) throws SQLException, IOException {
		long t0 = System.currentTimeMillis();
		Map<String, List<Path>> files = releaseFiles(releaseDir);
		List<String> loaded = new ArrayList<>();
		int emptyFiles = 0;
		long rows = 0;
		long ragged = 0;

		try (Statement st = con.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS " + schema);

			for (Map.Entry<String, List<Path>> e : files.entrySet()) {
				String table = e.getKey();
				String columns = tableColumns.get(table);
				if (columns == null) {
					LOGGER.debug("no column spec for {} - not materialised", table);
					continue;
				}
				// A zero-byte file has no header for read_csv to consume. MySQL loads it as
				// zero rows, and so does this.
				List<Path> loadable = new ArrayList<>();
				for (Path file : e.getValue()) {
					if (Files.size(file) == 0) {
						LOGGER.info("empty file {} - not loaded into {}", file.getFileName(), table);
						emptyFiles++;
					} else {
						loadable.add(file);
					}
				}
				if (loadable.isEmpty()) {
					continue;
				}
				Loaded l = load(con, schema, table, loadable, columns);
				rows += l.rows();
				ragged += l.ragged();
				loaded.add(table);
			}

			int placeholders = 0;
			for (Map.Entry<String, String> e : new TreeMap<>(tableColumns).entrySet()) {
				if (loaded.contains(e.getKey())) {
					continue;
				}
				st.execute("CREATE TABLE IF NOT EXISTS " + schema + "." + e.getKey() + " (" + e.getValue() + ")");
				placeholders++;
			}
			Result r = new Result(loaded.size(), emptyFiles, placeholders, rows, ragged,
					System.currentTimeMillis() - t0);
			LOGGER.info("materialised {} tables ({} rows, {} malformed), {} empty files, "
					+ "{} placeholders in {}ms", r.tablesLoaded(), r.rows(), r.raggedRows(),
					r.emptyFiles(), r.placeholders(), r.millis());
			return r;
		}
	}

	/**
	 * Loads every file that maps to this table as one relation.
	 *
	 * <p>The mapping is many-to-one: a multilingual edition ships a language refset and a
	 * description file per language, and MySQL loads all of them into one table.
	 * {@code read_csv} takes the list, so the ORDER BY sorts the whole relation.
	 *
	 * <p>Columns are matched to the file by its header where the header names exactly the
	 * declared columns (see {@link #fileOrder}). {@code load data local infile} is positional,
	 * so where a file's column order differs from the DDL's, MySQL loads values into the
	 * wrong columns; this does not reproduce that.
	 */
	private static Loaded load(Connection con, String schema, String table, List<Path> files,
			String columns) throws SQLException, IOException {
		List<String> ddlNames = columnNames(columns);
		// With `columns` given, read_csv skips the header line and assigns fields by
		// position, so the spec must be in the file's order.
		List<String> order = fileOrder(files, ddlNames, table);
		String spec = columnSpec(columns, order);
		String sort = sortKey(table, columns);
		String fileList = fileList(files);
		// Projected in the DDL's order, so every schema agrees on column order for the
		// SELECT * copies in the extension combine and the delta-only snapshot rebuild.
		String sql = "CREATE OR REPLACE TABLE " + schema + "." + table + " AS SELECT "
				+ String.join(", ", ddlNames) + " FROM read_csv([" + fileList + "], delim='\t', header=true, columns={"
				+ spec + "}, "
				// RF2 is neither quoted nor escaped.
				+ "quote='', escape='', nullstr='" + NO_NULLSTR + "', "
				// Never drop a row: a dropped row is a failure the report cannot show.
				+ "ignore_errors=false) ORDER BY " + sort;
		long ragged = 0;
		// A new statement per attempt: the DuckDB driver leaves a failed statement unusable.
		try (Statement st = con.createStatement()) {
			st.execute(sql);
		} catch (SQLException e) {
			// The strict read refuses a file with rows of the wrong width, or a value that will
			// not cast (a blank in a SMALLINT column). MySQL loads both, padding, truncating and
			// writing 0. RVF's regression fixture has such files, and structural validation runs
			// concurrently rather than gating this pass, so fall back to MySQL's tolerance.
			ragged = loadRagged(con, schema, table, files, columns, order, sort);
			LOGGER.warn("{}: strict read refused ({}), reloaded tolerantly - {} of its rows do "
					+ "not match the {} declared columns, padded and truncated as MySQL would. "
					+ "Files: {}", table, e.getMessage().replace('\n', ' ').trim(), ragged,
					ddlNames.size(), files.stream().map(f -> f.getFileName().toString()).toList());
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("SELECT count(*) FROM " + schema + "." + table)) {
			rs.next();
			return new Loaded(rs.getLong(1), ragged);
		}
	}

	/**
	 * The tolerant read: each line as one column, split in SQL and projected by position.
	 *
	 * <p>An index past the end of the split is NULL, which pads; an index the projection does
	 * not use is discarded, which truncates. {@code try_cast}, so one malformed value does not
	 * fail the release. Unlike MySQL, a missing field is NULL rather than '' or 0; neither
	 * matches a real component, and structural validation reports the row either way.
	 */
	private static long loadRagged(Connection con, String schema, String table, List<Path> files,
			String columns, List<String> order, String sort) throws SQLException {
		Map<String, String> types = columnTypes(columns);
		String split = "SELECT str_split(line, chr(9)) AS f FROM read_csv([" + fileList(files)
				+ "], header=false, skip=1, delim=chr(1), quote='', escape='', "
				+ "columns={'line':'VARCHAR'})";
		List<String> projection = new ArrayList<>();
		for (String name : columnNames(columns)) {
			int position = order.indexOf(name) + 1;
			projection.add("try_cast(f[" + position + "] AS " + types.get(name) + ") AS " + name);
		}
		try (Statement st = con.createStatement()) {
			st.execute("CREATE OR REPLACE TABLE " + schema + "." + table + " AS SELECT "
					+ String.join(", ", projection) + " FROM (" + split + ") ORDER BY " + sort);
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("SELECT count(*) FROM (" + split + ") WHERE len(f) <> " + order.size())) {
			rs.next();
			return rs.getLong(1);
		}
	}

	/**
	 * The column order to give {@code read_csv}: the files' own when their header names
	 * exactly the declared columns, and the DDL's otherwise.
	 *
	 * <p>Some refsets carry column names RVF's generic table does not have, and are loaded
	 * positionally, as MySQL loads them. Files mapping to one table that disagree about their
	 * header are also loaded in DDL order, and logged.
	 */
	private static List<String> fileOrder(List<Path> files, List<String> ddlNames, String table)
			throws IOException {
		List<String> header = null;
		for (Path file : files) {
			List<String> names = headerOf(file);
			if (header == null) {
				header = names;
			} else if (!header.equals(names)) {
				LOGGER.warn("files for {} disagree about their header ({} vs {}) - loading in "
						+ "declared column order", table, header, names);
				return ddlNames;
			}
		}
		if (header == null || header.equals(ddlNames)) {
			return ddlNames;
		}
		if (!new HashSet<>(header).equals(new HashSet<>(ddlNames))) {
			LOGGER.warn("{} is declared as {} but its file has {} - loading positionally, as "
					+ "MySQL does", table, ddlNames, header);
			return ddlNames;
		}
		LOGGER.info("{} ships its columns as {} - loading by name, not position", table, header);
		return header;
	}

	private static List<String> headerOf(Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			String line = reader.readLine();
			if (line == null) {
				return List.of();
			}
			List<String> names = new ArrayList<>();
			for (String name : line.split("\t", -1)) {
				names.add(name.trim().toLowerCase(Locale.ROOT));
			}
			return names;
		}
	}

	private static String fileList(List<Path> files) {
		return files.stream().map(f -> "'" + f.toAbsolutePath() + "'").collect(Collectors.joining(", "));
	}

	private static List<String> columnNames(String columns) {
		return new ArrayList<>(columnTypes(columns).keySet());
	}

	/** {@code "id BIGINT, active VARCHAR"} to {@code {id=BIGINT, active=VARCHAR}}, names lower-cased. */
	private static Map<String, String> columnTypes(String columns) {
		Map<String, String> types = new LinkedHashMap<>();
		for (String col : columns.split(",")) {
			String[] nameType = col.trim().split("\\s+", 2);
			types.put(nameType[0].toLowerCase(Locale.ROOT), nameType[1]);
		}
		return types;
	}

	/** The {@code read_csv} columns struct, {@code 'name':'TYPE'}, in {@code order}. */
	private static String columnSpec(String columns, List<String> order) {
		Map<String, String> types = columnTypes(columns);
		List<String> parts = new ArrayList<>();
		for (String name : order) {
			parts.add("'" + name + "':'" + types.get(name) + "'");
		}
		return String.join(",", parts);
	}

	private static String sortKey(String table, String columns) {
		List<String> names = columnNames(columns);
		String preferred = SORT_KEY.getOrDefault(table, DEFAULT_SORT);
		return names.contains(preferred) ? preferred : names.get(0);
	}

	/**
	 * Every RF2 .txt under the release, grouped by the table RVF loads it into, sorted so the
	 * load order is reproducible.
	 */
	static Map<String, List<Path>> releaseFiles(Path releaseDir) throws IOException {
		Map<String, List<Path>> out = new LinkedHashMap<>();
		try (Stream<Path> walk = Files.walk(releaseDir)) {
			List<Path> txt = walk.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".txt"))
					.sorted()
					.toList();
			for (Path p : txt) {
				String table = RF2FileTableMapper.getLegacyTableName(p.getFileName().toString());
				if (table != null) {
					out.computeIfAbsent(table, t -> new ArrayList<>()).add(p);
				}
			}
		}
		return out;
	}
}
