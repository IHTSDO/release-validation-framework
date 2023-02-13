package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
public class ReleaseDataManagerIntegrationTest {
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
		final String schemaName = releaseDataManager.loadSnomedData(versionName, rf2FilesLoaded, inputFile);
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
		final boolean writeSucess =releaseDataManager.uploadPublishedReleaseData(inputFile, "int", "20140131");
		assertTrue(writeSucess, "Upload must have been successful");

		assertTrue(releaseDataManager.isKnownRelease("rvf_int_20140131"), "Schema name for release 20140131 must be known to data manager ");

		assertTrue(releaseDataManager.getAllKnownReleases().contains("rvf_int_20140131"), "Relese 20140131 must exist in all known releases ");
	}
}