package org.ihtsdo.rvf.execution.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ExecutionServiceTestConfig.class})
public class ReleaseDataManagerIntegrationTest {
	@Resource(name = "dataSource")
	private BasicDataSource dataSource;
	@Autowired
	private ReleaseDataManager releaseDataManager;

	@Test
	public void testLoadSctData() throws Exception {
		assert dataSource != null;

		final File inputFile = new File(getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI());
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
			assertTrue("Schema name must exist : " + schemaName, exists);
			assertNotNull(releaseDataManager.getZipFileForKnownRelease(versionName));
		}
	}

	@Test
	public void testUploadSctData() throws Exception {
		assert dataSource != null;
		final File inputFile = new File(getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI());
		assertNotNull(inputFile);
		final boolean writeSucess =releaseDataManager.uploadPublishedReleaseData(inputFile, "int", "20140131");
		assertTrue("Upload must have been successful", writeSucess);

		assertTrue("Schema name for release 20140131 must be known to data manager ", releaseDataManager.isKnownRelease("rvf_int_20140131"));

		assertTrue("Relese 20140131 must exist in all known releases ", releaseDataManager.getAllKnownReleases().contains("rvf_int_20140131"));
	}
}