package org.ihtsdo.rvf.core.service.duck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Loading RF2 into DuckDB tables the way RVF's MySQL loader does: typed from the
 * store's DDL, tolerant of empty and malformed files, every file of a table loaded.
 */
class DuckMaterialiserTest {

	private static final Map<String, String> COLUMNS = new LinkedHashMap<>();
	static {
		COLUMNS.put("concept_s", "id BIGINT, effectivetime VARCHAR, active VARCHAR, "
				+ "moduleid BIGINT, definitionstatusid BIGINT");
		COLUMNS.put("description_d", "id BIGINT, effectivetime VARCHAR, active VARCHAR, "
				+ "moduleid BIGINT, conceptid BIGINT, languagecode VARCHAR, typeid BIGINT, "
				+ "term VARCHAR, casesignificanceid BIGINT");
	}

	private static final String DESCRIPTION_HEADER =
			"id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId\n";

	@Test
	void emptyRf2FileBecomesAnEmptyTableRatherThanAFailedLoad(@TempDir Path release) throws Exception {
		// A zero-byte file has no header for read_csv to consume, so it would fail
		// the whole release, where MySQL loads zero rows and carries on.
		write(release, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt",
				"id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "138875005\t20260831\t1\t900000000000207008\t900000000000074008\n");
		write(release, "Delta/Terminology/sct2_Description_Delta-en_XX1000000_20260831.txt", "");

		try (Connection con = connect()) {
			DuckMaterialiser.Result r = DuckMaterialiser.materialise(con, release, "prospective", COLUMNS);
			assertEquals(1, r.tablesLoaded());
			assertEquals(1, r.emptyFiles());
			assertEquals(1, r.placeholders(), "the emptied file's table still has to exist");
			assertEquals(1, count(con, "concept_s"));
			assertEquals(0, count(con, "description_d"), "empty file, but a queryable table");
		}
	}

	@Test
	void sctidsLoadAsBigintNotVarchar(@TempDir Path release) throws Exception {
		// Compared via DOUBLE, extension SCTIDs above 2^53 stop matching exactly.
		// Typing from the store, not read_csv inference, keeps them BIGINT; an
		// inferred load would look fine on small ids.
		write(release, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt",
				"id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "4100000001000001106\t20260831\t1\t900000000000207008\t900000000000074008\n");

		try (Connection con = connect()) {
			DuckMaterialiser.materialise(con, release, "prospective", COLUMNS);
			try (Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(
							"SELECT typeof(id), id = 4100000001000001106 FROM prospective.concept_s")) {
				assertTrue(rs.next());
				assertEquals("BIGINT", rs.getString(1));
				assertTrue(rs.getBoolean(2), "an SCTID above 2^53 must compare exactly");
			}
		}
	}

	@Test
	void everyFileMappingToATableIsLoaded(@TempDir Path release) throws Exception {
		// RF2 file names map to tables many-to-one: a multilingual edition ships
		// one Description and Language file per language, and MySQL loads them
		// all into one table. Keeping one path per table would silently drop the rest.
		write(release, "Snapshot/Terminology/sct2_Description_Snapshot-en_CH1000195_20260831.txt",
				DESCRIPTION_HEADER
				+ "101013\t20260831\t1\t900000000000207008\t138875005\ten\t900000000000003001\tSNOMED CT Concept\t900000000000020002\n");
		write(release, "Snapshot/Terminology/sct2_Description_Snapshot-fr_CH1000195_20260831.txt",
				DESCRIPTION_HEADER
				+ "101014\t20260831\t1\t900000000000207008\t138875005\tfr\t900000000000003001\tConcept SNOMED CT\t900000000000020002\n");

		Map<String, String> columns = Map.of("description_s", COLUMNS.get("description_d"));
		try (Connection con = connect()) {
			DuckMaterialiser.Result r = DuckMaterialiser.materialise(con, release, "prospective", columns);
			assertEquals(1, r.tablesLoaded(), "two files, one table");
			assertEquals(2, r.rows(), "both languages, not just whichever sorted last");
			assertEquals(2, count(con, "description_s"));
		}
	}

	@Test
	void aRaggedFileLoadsEveryRowRatherThanNone(@TempDir Path release) throws Exception {
		// Malformed content is what a validator is for. read_csv refuses a ragged
		// file outright; MySQL's loader takes every row, padding a short one and
		// truncating a long one, and so must this.
		write(release, "Delta/Terminology/sct2_Description_Delta-en_XX1000000_20260831.txt",
				DESCRIPTION_HEADER
				// well-formed
				+ "101013\t20260831\t1\t900000000000207008\t138875005\ten\t900000000000003001\tSNOMED CT Concept\t900000000000020002\n"
				// one field short: caseSignificanceId missing
				+ "101014\t20260831\t1\t900000000000207008\t138875005\ten\t900000000000003001\tShort row\n"
				// one field too many, as an unescaped tab inside a term does
				+ "101015\t20260831\t1\t900000000000207008\t138875005\ten\t900000000000003001\tLong row\t900000000000020002\tspill\n");

		try (Connection con = connect()) {
			DuckMaterialiser.Result r = DuckMaterialiser.materialise(con, release, "prospective", COLUMNS);
			assertEquals(3, r.rows(), "every row, including the two malformed ones");
			assertEquals(2, r.raggedRows(), "and it says how many were malformed");

			try (Statement st = con.createStatement();
					ResultSet rs = st.executeQuery("SELECT term, casesignificanceid "
							+ "FROM prospective.description_d ORDER BY id")) {
				assertTrue(rs.next());
				assertEquals("SNOMED CT Concept", rs.getString(1));
				assertEquals(900000000000020002L, rs.getLong(2));

				assertTrue(rs.next());
				assertEquals("Short row", rs.getString(1), "the fields present are still read");
				rs.getLong(2);
				assertTrue(rs.wasNull(), "a missing field pads rather than shifting the row");

				assertTrue(rs.next());
				assertEquals("Long row", rs.getString(1), "the declared fields keep their meaning");
				assertEquals(900000000000020002L, rs.getLong(2), "the spill is dropped, not shifted in");
			}
		}
	}

	@Test
	void aWellFormedFileIsNotCountedAsRagged(@TempDir Path release) throws Exception {
		// The tolerant read is a fallback; as the normal path every release would
		// report malformed rows it does not have.
		write(release, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt",
				"id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "138875005\t20260831\t1\t900000000000207008\t900000000000074008\n");

		try (Connection con = connect()) {
			DuckMaterialiser.Result r = DuckMaterialiser.materialise(con, release, "prospective", COLUMNS);
			assertEquals(1, r.rows());
			assertEquals(0, r.raggedRows());
		}
	}

	@Test
	void oneBadFileDoesNotPoisonTheRest(@TempDir Path release) throws Exception {
		// DuckDB's JDBC driver leaves a failed statement holding an unsuccessful
		// pending result, so a statement shared across files would turn the first
		// bad file into every later table failing.
		write(release, "Delta/Terminology/sct2_Description_Delta-en_XX1000000_20260831.txt",
				DESCRIPTION_HEADER
				+ "101014\t20260831\t1\t900000000000207008\t138875005\ten\t900000000000003001\tShort row\n");
		write(release, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt",
				"id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "138875005\t20260831\t1\t900000000000207008\t900000000000074008\n");

		try (Connection con = connect()) {
			DuckMaterialiser.Result r = DuckMaterialiser.materialise(con, release, "prospective", COLUMNS);
			assertEquals(2, r.tablesLoaded(), "the good table loads too");
			assertEquals(1, count(con, "concept_s"));
			assertEquals(1, count(con, "description_d"));
		}
	}

	private static Connection connect() throws Exception {
		Class.forName("org.duckdb.DuckDBDriver");
		return DriverManager.getConnection("jdbc:duckdb:");
	}

	/**
	 * {@code read_csv}'s {@code columns} binds by position, so a file whose header
	 * orders the declared columns differently must be loaded by name. The values
	 * are all numeric so a positional load would succeed silently with the wrong data.
	 */
	@Test
	void aFileWhoseColumnOrderDiffersFromTheDeclarationLoadsByName(@TempDir Path release)
			throws Exception {
		write(release, "Snapshot/Terminology/sct2_Concept_Snapshot_XX1000000_20260831.txt",
				"id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "138875005\t20260831\t1\t900000000000207008\t900000000000074008\n");
		Map<String, String> columns = Map.of("concept_s",
				"definitionstatusid BIGINT, moduleid BIGINT, id BIGINT, effectivetime VARCHAR, active VARCHAR");

		try (Connection con = connect()) {
			DuckMaterialiser.materialise(con, release, "prospective", columns);
			try (Statement st = con.createStatement();
					ResultSet rs = st.executeQuery("SELECT id, moduleid, definitionstatusid, active"
							+ " FROM prospective.concept_s")) {
				assertTrue(rs.next());
				assertEquals(138875005L, rs.getLong("id"));
				assertEquals(900000000000207008L, rs.getLong("moduleid"));
				assertEquals(900000000000074008L, rs.getLong("definitionstatusid"));
				assertEquals("1", rs.getString("active"));
			}
		}
	}

	/**
	 * A refset whose header names columns the generic table does not declare has
	 * nothing to match by name, so it loads positionally, as MySQL does.
	 */
	@Test
	void aFileWhoseColumnNamesAreNotDeclaredStillLoadsPositionally(@TempDir Path release)
			throws Exception {
		write(release, "Snapshot/Refset/Content/der2_cRefset_ExampleAssociationSnapshot_XX1000000_20260831.txt", """
				id	effectiveTime	active	moduleId	refsetId	referencedComponentId	linkedConceptId
				a1	20260831	1	900000000000207008	900	111	222
				""");
		Map<String, String> columns = Map.of("associationrefset_s",
				"id VARCHAR, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, "
				+ "refsetid BIGINT, referencedcomponentid BIGINT, targetcomponentid BIGINT");

		try (Connection con = connect()) {
			DuckMaterialiser.materialise(con, release, "prospective", columns);
			try (Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(
							"SELECT referencedcomponentid, targetcomponentid FROM prospective.associationrefset_s")) {
				assertTrue(rs.next());
				assertEquals(111L, rs.getLong("referencedcomponentid"));
				assertEquals(222L, rs.getLong("targetcomponentid"));
			}
		}
	}

	private static void write(Path root, String relative, String content) throws Exception {
		Path p = root.resolve(relative);
		Files.createDirectories(p.getParent());
		Files.writeString(p, content);
	}

	private static long count(Connection con, String table) throws Exception {
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("SELECT count(*) FROM prospective." + table)) {
			rs.next();
			return rs.getLong(1);
		}
	}
}
