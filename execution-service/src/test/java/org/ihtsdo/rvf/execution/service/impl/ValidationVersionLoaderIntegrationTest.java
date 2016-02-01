package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
public class ValidationVersionLoaderIntegrationTest {
	@Resource(name = "snomedDataSource")
	private DataSource snomedDataSource;
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	ValidationVersionLoader dataLoader;
	private String prospectiveVersion;
	private ValidationRunConfig validationConfig;
	
	@Before
	public void setUp() {
		validationConfig = new ValidationRunConfig();
		validationConfig.setRf2DeltaOnly(true);
		validationConfig.setFailureExportMax(100);
		validationConfig.setFirstTimeRelease(false);
		validationConfig.setPrevIntReleaseVersion("int_20160131");
		File localProspectiveFile = new File(ClassLoader.getSystemResource("Daily_Export_Delta.zip").getFile());
		validationConfig.setLocalProspectiveFile(localProspectiveFile);
		dataLoader = new ValidationVersionLoader();
		prospectiveVersion = Long.toString(System.currentTimeMillis());
	}
	
	@Test
	public void testConstructProspectiveVersionWithRF2DeltaOnly() throws BusinessServiceException {
		List<String> filesLoaded = dataLoader.loadProspectiveDeltaWithPreviousSnapshotIntoDB(prospectiveVersion, validationConfig);
		Assert.assertEquals(1, filesLoaded.size());
		Assert.assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}
}
