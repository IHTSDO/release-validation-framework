package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseDataManagerIntegrationTest extends IntegrationTest {
	@Resource(name = "dataSource")
	private BasicDataSource dataSource;
	@Autowired
	private ReleaseDataManager releaseDataManager;

	@Test
	public void testLoadSctData() throws Exception {
		assert dataSource != null;

		URL url = getClass().getResource("/SnomedCT_Release_INT_20140131.zip");
		assertNotNull(url);
		final File inputFile = new File(url.toURI());
		assertNotNull(inputFile);
		final String versionName = "20140131";
		List<String> rf2FilesLoaded = new ArrayList<>();
		final String schemaName = releaseDataManager.loadSnomedData(versionName, rf2FilesLoaded, Collections.emptyList(), inputFile);
		try (
				Connection connection = dataSource.getConnection();
				ResultSet catalogs = connection.getMetaData().getCatalogs()) {
			boolean exists = false;
			while (catalogs.next()) {
				final String catalogName = catalogs.getString(1);
				if(catalogName.equals(schemaName)){
					exists = true;
					break;
				}
			}
			assertTrue(exists, "Schema name must exist : " + schemaName);
			assertNotNull(releaseDataManager.getZipFileForKnownRelease(versionName));
		}
	}

	@org.junit.jupiter.api.Test
	public void testUploadSctData() throws Exception {
		assert dataSource != null;
		URL url = getClass().getResource("/SnomedCT_Release_INT_20140131.zip");
		assertNotNull(url);
		final File inputFile = new File(url.toURI());
		assertNotNull(inputFile);
		final boolean writeSucess =releaseDataManager.uploadPublishedReleaseData(inputFile, "int", "20140131", Collections.emptyList());
		assertTrue(writeSucess, "Upload must have been successful");

		assertTrue(releaseDataManager.isKnownRelease("rvf_int_20140131"), "Schema name for release 20140131 must be known to data manager ");

		assertTrue(releaseDataManager.getAllKnownReleases().contains("rvf_int_20140131"), "Relese 20140131 must exist in all known releases ");
	}

	/**
	 * RF2 files are loaded by position, so the identifier tables declare their
	 * columns in the order of the RF2 Identifier file:
	 * alternateIdentifier, effectiveTime, active, moduleId, identifierSchemeId, referencedComponentId.
	 */
	@Test
	public void testIdentifierFileIsLoadedIntoItsColumns() throws Exception {
		File dir = Files.createTempDirectory("rvf-identifier").toFile();
		File zip = new File(dir, "SnomedCT_IdentifierTest_INT_20250131.zip");
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			out.putNextEntry(new ZipEntry("SnomedCT_IdentifierTest_INT_20250131/Snapshot/Terminology/sct2_Identifier_Snapshot_INT_20250131.txt"));
			out.write(("alternateIdentifier\teffectiveTime\tactive\tmoduleId\tidentifierSchemeId\treferencedComponentId\r\n"
					+ "ALT-1\t20250131\t1\t900000000000207008\t900000000000002006\t138875005\r\n").getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		String schemaName = releaseDataManager.loadSnomedData("identifier_load_test", new ArrayList<>(), Collections.emptyList(), zip);
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("select alternateidentifier, effectivetime, active, moduleid, identifierschemeid, referencedcomponentid from "
					 + schemaName + ".identifier_s")) {
			assertTrue(resultSet.next(), "the Identifier row must be loaded");
			assertEquals("ALT-1", resultSet.getString(1));
			assertEquals("20250131", resultSet.getString(2));
			assertEquals("1", resultSet.getString(3));
			assertEquals(900000000000207008L, resultSet.getLong(4));
			assertEquals(900000000000002006L, resultSet.getLong(5));
			assertEquals(138875005L, resultSet.getLong(6));
		} finally {
			releaseDataManager.dropSchema(schemaName);
			FileUtils.deleteQuietly(dir);
		}
	}
}