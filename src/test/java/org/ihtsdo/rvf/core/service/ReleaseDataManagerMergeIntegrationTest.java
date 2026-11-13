package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Resource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The merge paths that combine schemas - extension with dependency, delta into
 * snapshot, and deriving delta tables for an edition - must handle every table
 * that create-tables-mysql.sql creates. identifier_* has no id column; its RF2
 * key is (identifierSchemeId, alternateIdentifier). A dependency schema built
 * before a table was added to create-tables-mysql.sql does not have it at all.
 */
class ReleaseDataManagerMergeIntegrationTest extends IntegrationTest {

	private static final String SCHEME = "900000000000002006";
	private static final String MODULE = "900000000000207008";

	@Resource(name = "dataSource")
	private BasicDataSource dataSource;

	@Autowired
	private ReleaseDataManager releaseDataManager;

	private final List<String> schemas = new ArrayList<>();

	@AfterEach
	void dropSchemas() throws Exception {
		for (String schema : schemas) {
			releaseDataManager.dropSchema(schema);
		}
	}

	/**
	 * ValidationVersionLoader.combineCurrentExtensionWithDependencySnapshot merges
	 * every snapshot table with copyTableData(dependency, extension, combined, "%_s").
	 * identifier_s must be merged on its key, keeping the most recent row, and the
	 * tables after it must still be merged.
	 */
	@Test
	void extensionAndDependencySnapshotsAreMergedOnTheIdentifierKey() throws Exception {
		String dependency = schema("merge_dependency");
		String extension = schema("merge_extension");
		String combined = schema("merge_combined");
		insertIdentifier(dependency, "identifier_s", "A", "20200131", "1");
		insertIdentifier(dependency, "identifier_s", "B", "20200131", "1");
		insertIdentifier(extension, "identifier_s", "A", "20210131", "0");
		insertIdentifier(extension, "identifier_s", "C", "20210131", "1");
		insertLanguageRefset(dependency, "langrefset_s", "11111111-1111-1111-1111-111111111111");
		insertLanguageRefset(extension, "langrefset_s", "22222222-2222-2222-2222-222222222222");

		releaseDataManager.copyTableData(dependency, extension, combined, "%_s", null);

		assertEquals(List.of("A 20210131 0", "B 20200131 1", "C 20210131 1"), identifiers(combined, "identifier_s"));
		assertEquals(2, count(combined, "langrefset_s"), "tables after identifier_s must still be merged");
	}

	/**
	 * A dependency schema built before identifier_s existed: the extension's rows
	 * are still copied into the combined schema, and the merge does not fail.
	 */
	@Test
	void extensionRowsAreKeptWhenTheDependencySchemaPredatesTheTable() throws Exception {
		String dependency = schema("merge_old_dependency");
		String extension = schema("merge_extension");
		String combined = schema("merge_combined");
		dropIdentifierTables(dependency);
		insertIdentifier(extension, "identifier_s", "C", "20210131", "1");

		releaseDataManager.copyTableData(dependency, extension, combined, "%_s", null);

		assertEquals(List.of("C 20210131 1"), identifiers(combined, "identifier_s"));
	}

	/**
	 * Delta-only validation: ValidationVersionLoader copies the previous snapshot,
	 * then updateSnapshotTableWithDataFromDelta replaces snapshot rows that the
	 * delta changes. For identifier_s that has to match on the key.
	 */
	@Test
	void deltaRowsReplaceSnapshotRowsOnTheIdentifierKey() throws Exception {
		String prospective = schema("merge_delta_only");
		insertIdentifier(prospective, "identifier_s", "A", "20200131", "1");
		insertIdentifier(prospective, "identifier_s", "B", "20200131", "1");
		insertIdentifier(prospective, "identifier_d", "A", "20250131", "0");
		insertIdentifier(prospective, "identifier_d", "D", "20250131", "1");

		releaseDataManager.updateSnapshotTableWithDataFromDelta(prospective);

		assertEquals(List.of("A 20250131 0", "B 20200131 1", "D 20250131 1"), identifiers(prospective, "identifier_s"));
	}

	/**
	 * Edition-and-extension package without Delta files: delta rows are derived from
	 * the Full, excluding rows already in the dependency's Full (matched on the key),
	 * and the dependency's own new rows are added.
	 */
	@Test
	void editionDeltaIsDerivedUsingTheIdentifierKey() throws Exception {
		String dependency = schema("merge_edition_dependency");
		String prospective = schema("merge_edition");
		insertIdentifier(dependency, "identifier_f", "A", "20250131", "1");
		insertIdentifier(prospective, "identifier_f", "A", "20250131", "1");
		insertIdentifier(prospective, "identifier_f", "E", "20250131", "1");

		releaseDataManager.insertIntoProspectiveDeltaTables(prospective, editionConfig(prospective, dependency));

		// E is new in the edition. A arrives from the dependency's own new rows, not
		// from the edition's Full, so it appears once.
		assertEquals(List.of("A 20250131 1", "E 20250131 1"), identifiers(prospective, "identifier_d"));
	}

	/**
	 * The same derivation when the dependency schema was built before identifier_*
	 * existed: there is nothing to exclude or to add from it for that table, and the
	 * other tables are still derived.
	 */
	@Test
	void editionDeltaIsDerivedWhenTheDependencySchemaPredatesTheTable() throws Exception {
		String dependency = schema("merge_edition_old_dependency");
		String prospective = schema("merge_edition");
		dropIdentifierTables(dependency);
		insertIdentifier(prospective, "identifier_f", "E", "20250131", "1");
		insertLanguageRefset(prospective, "langrefset_f", "33333333-3333-3333-3333-333333333333");

		releaseDataManager.insertIntoProspectiveDeltaTables(prospective, editionConfig(prospective, dependency));

		assertEquals(List.of("E 20250131 1"), identifiers(prospective, "identifier_d"));
		assertEquals(1, count(prospective, "langrefset_d"));
	}

	private MysqlExecutionConfig editionConfig(String prospective, String dependency) {
		MysqlExecutionConfig config = new MysqlExecutionConfig(1L, false);
		config.setProspectiveVersion(prospective);
		config.setReleaseAsAnEdition(true);
		config.setExtensionValidation(true);
		config.setExtensionDependencyVersion(dependency);
		config.setPreviousEffectiveTime("20240131");
		config.addCurrentDependencyRelease("dependency.zip", dependency);
		config.addCurrentDependencyToPreviousEffectiveTime("dependency.zip", "20240131");
		return config;
	}

	private String schema(String name) throws Exception {
		String schema = releaseDataManager.createSchema(name);
		schemas.add(schema);
		return schema;
	}

	private void dropIdentifierTables(String schema) throws SQLException {
		for (String suffix : new String[]{"d", "s", "f"}) {
			execute("drop table " + schema + ".identifier_" + suffix);
		}
	}

	private void insertIdentifier(String schema, String table, String alternateIdentifier, String effectiveTime, String active) throws SQLException {
		execute("insert into " + schema + "." + table
				+ " (alternateidentifier, effectivetime, active, moduleid, identifierschemeid, referencedcomponentid)"
				+ " values ('" + alternateIdentifier + "', '" + effectiveTime + "', '" + active + "', " + MODULE + ", " + SCHEME + ", 138875005)");
	}

	private void insertLanguageRefset(String schema, String table, String id) throws SQLException {
		execute("insert into " + schema + "." + table
				+ " (id, effectivetime, active, moduleid, refsetid, referencedcomponentid, acceptabilityid)"
				+ " values ('" + id + "', '20250131', '1', " + MODULE + ", 900000000000509007, 1, 900000000000548007)");
	}

	private List<String> identifiers(String schema, String table) throws SQLException {
		List<String> rows = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("select alternateidentifier, effectivetime, active from "
					 + schema + "." + table + " order by alternateidentifier, effectivetime")) {
			while (resultSet.next()) {
				rows.add(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
			}
		}
		return rows;
	}

	private int count(String schema, String table) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("select count(*) from " + schema + "." + table)) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}

	private void execute(String sql) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}
}
