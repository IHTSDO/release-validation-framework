package org.ihtsdo.rvf.execution.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
public class ReleaseDataManagerImplIntegrationTest {
	@Resource(name = "dataSource")
	private DataSource dataSource;
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
		final boolean writeSucess =releaseDataManager.uploadPublishedReleaseData(inputFile, "INT", "20140131");
		assertTrue("Upload must have been successful", writeSucess);

		assertTrue("Schema name for release data 20140131 must be known to data manager ", releaseDataManager.isKnownRelease("20140131"));

		assertTrue("Relese 20140131 must exist in all known releases ", releaseDataManager.getAllKnownReleases().contains("20140131"));
	}
}