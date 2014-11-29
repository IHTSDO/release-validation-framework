package org.ihtsdo.rvf.execution.service.impl;

import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testApplicationContext.xml"})
public class ReleaseDataManagerImplIntegrationTest {
    @Resource(name = "snomedDataSource")
    private DataSource snomedDataSource;
    @Autowired
    private ReleaseDataManager releaseDataManager;

    public void testLoadSctData() throws Exception {
        assert snomedDataSource != null;

        File inputFile = new File(getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI());
        assertNotNull(inputFile);
        String versionName = "20140131";
        String schemaName =releaseDataManager.loadSnomedData(versionName, true, inputFile);
        Connection connection = snomedDataSource.getConnection();
        ResultSet catalogs = connection.getMetaData().getCatalogs();
        boolean exists = false;
        while(catalogs.next())
        {
            String catalogName = catalogs.getString(1);
            if(catalogName.equals(schemaName)){
                exists = true;
                break;
            }
        }
        catalogs.close();
        connection.close();

        assertTrue("Schema name must exist : " + schemaName, exists);
    }

    @Test
    public void testUploadSctData() throws Exception {
        assert snomedDataSource != null;

        File inputFile = new File(getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI());
        assertNotNull(inputFile);
        boolean writeSucess =releaseDataManager.uploadPublishedReleaseData(inputFile, true, true);
        assertTrue("Upload must have been successful", writeSucess);

        assertTrue("Schema name for release data 20140131 must be known to data manager ", releaseDataManager.isKnownRelease("20140131"));

        assertTrue("Relese 20140131 must exist in all known releases ", releaseDataManager.getAllKnownReleases().contains("20140131"));
    }
}