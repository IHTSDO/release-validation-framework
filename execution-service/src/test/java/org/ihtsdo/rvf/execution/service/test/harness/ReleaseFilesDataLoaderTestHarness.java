package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
@Transactional
public class ReleaseFilesDataLoaderTestHarness {
	
	 @Resource(name = "snomedDataSource")
	    private DataSource snomedDataSource;
	    @Autowired
	    private ReleaseDataManager releaseDataManager;
	    private final String intFilePath = "/Users/mchu/SNOMED_Releases/SnomedCT_RF2Release_INT_20150131.zip";
		private final String extentionFilePath = "/Users/mchu/SNOMED_Releases/SnomedCT_SpanishRelease_INT_20141031.zip";
		
		
		//getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI()

	    @Test
	    public void testLoadSctData() throws Exception {
	        assert snomedDataSource != null;

	        final File intFile = new File(intFilePath);
	        final File extentionFile = new File(extentionFilePath);
	       
	        assertNotNull(extentionFile);
	        final String versionName = "test_20150131";
	        releaseDataManager.loadSnomedData(versionName, intFile, extentionFile);
	        try (
	        		Connection connection = snomedDataSource.getConnection();
	        		ResultSet catalogs = connection.getMetaData().getCatalogs(); ) {
	        	boolean exists = false;
	            while (catalogs.next()) {
	                final String catalogName = catalogs.getString(1);
	                if(catalogName.equals(versionName)){
	                    exists = true;
	                    break;
	                }
	            }
	            assertTrue("Schema name must exist : " + versionName, exists);
	        }
	    }

}
