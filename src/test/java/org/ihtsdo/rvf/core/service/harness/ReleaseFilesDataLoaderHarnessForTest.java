package org.ihtsdo.rvf.core.service.harness;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.config.Config;
import org.ihtsdo.rvf.core.service.config.DatabaseServiceConfig;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@Transactional
public class ReleaseFilesDataLoaderHarnessForTest {
	
	 @Resource(name = "dataSource")
		private DataSource dataSource;
		@Autowired
		private ReleaseDataManager releaseDataManager;
		private static final String INT_FILE_PATH = "/Users/mchu/SNOMED_Releases/SnomedCT_RF2Release_INT_20150131.zip";
		private static final String EXTENTION_FILE_PATH = "/Users/mchu/SNOMED_Releases/SnomedCT_SpanishRelease_INT_20141031.zip";
		
		
		//getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI()

		@Test
		public void testLoadSctData() throws Exception {
			assert dataSource != null;

			final File intFile = new File(INT_FILE_PATH);
			final File extentionFile = new File(EXTENTION_FILE_PATH);

			assertNotNull(extentionFile);
			final String versionName = "test_20150131";
			List<String> rf2FilesLoaded = new ArrayList<>();
			releaseDataManager.loadSnomedData(versionName,rf2FilesLoaded, intFile, extentionFile);
//			releaseDataManager.combineKnownVersions(versionName, "20150131", "20141031");
			try (
					Connection connection = dataSource.getConnection();
					ResultSet catalogs = connection.getMetaData().getCatalogs()) {
				boolean exists = false;
				while (catalogs.next()) {
					final String catalogName = catalogs.getString(1);
					System.out.println("catalog name:" + catalogName);
					if(catalogName.contains(versionName)){
						exists = true;
						break;
					}
				}
				assertTrue("Schema name must exist : " + versionName, exists);
			}
		}

}
