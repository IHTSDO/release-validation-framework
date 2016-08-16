package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.junit.After;
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
	
	@Autowired
	ValidationVersionLoader dataLoader;
	private String prospectiveVersion;
	private String previousVersion;
	private ValidationRunConfig validationConfig;
	@Autowired
	private RvfDbScheduledEventGenerator scheduleEventGenerator;
	
	@Before
	public void setUp() {
		long runId = System.currentTimeMillis();
		validationConfig = new ValidationRunConfig();
		validationConfig.setFailureExportMax(100);
		validationConfig.setRunId(runId);
		validationConfig.setGroupsList(Arrays.asList("file-centric-validation"));
		File localProspectiveFile = new File(ClassLoader.getSystemResource("Daily_Export_Delta.zip").getFile());
		validationConfig.setLocalProspectiveFile(localProspectiveFile);
		
	}
	
	@Test
	public void testConstructIntProspectiveVersionWithRF2DeltaOnly() throws BusinessServiceException {
		validationConfig.setRf2DeltaOnly(true);
		prospectiveVersion = validationConfig.getRunId().toString();
		List<String> filesLoaded = dataLoader.loadProspectiveDeltaWithPreviousSnapshotIntoDB(prospectiveVersion, validationConfig,null);
		Assert.assertEquals(1, filesLoaded.size());
		Assert.assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}
	
	
	@Test
	public void testConstructExtensionProspectiveVersionWithRF2DeltaOnly() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		validationConfig.addExtensionDependencyVersion("int_20160131");
		validationConfig.addPreviousExtVersion("dk_20160215");
		validationConfig.setRf2DeltaOnly(true);
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadProspectiveVersion(executionConfig, responseMap, validationConfig);
		Assert.assertEquals(true, isLoaded);
		Assert.assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}
	
	
	@Test
	public void testProspectiveVersion() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadProspectiveVersion(executionConfig, responseMap, validationConfig);
		Assert.assertEquals(true, isLoaded);
		Assert.assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}
	
	
	
	@Test
	public void testProspectiveVersionWithExtension() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		validationConfig.addExtensionDependencyVersion("int_20160131");
		validationConfig.addPreviousExtVersion("dk_20160215");
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		executionConfig.setReleaseValidation(false);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadProspectiveVersion(executionConfig, responseMap, validationConfig);
		Assert.assertEquals(true, isLoaded);
		Assert.assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	} 
	
	@Test
	public void testLoadPreviousVersion() throws Exception {
		validationConfig.setFirstTimeRelease(false);
		validationConfig.setPrevIntReleaseVersion("SnomedCT_RF2Release_INT_20130131.zip");
		validationConfig.setS3PublishBucketName("local.publish.bucket");
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadPreviousVersion(executionConfig, responseMap, validationConfig);
		Assert.assertEquals(true, isLoaded);
		previousVersion = executionConfig.getPreviousVersion();
		Assert.assertTrue(releaseDataManager.isKnownRelease(previousVersion));
		
	}
	@After
	public void tearDown() throws SQLException {
		if (prospectiveVersion != null) {
			scheduleEventGenerator.createDropReleaseSchemaEvent(releaseDataManager.getSchemaForRelease(prospectiveVersion));
			releaseDataManager.dropVersion(prospectiveVersion);
		}
		
		if (previousVersion != null) {
			scheduleEventGenerator.createDropReleaseSchemaEvent(releaseDataManager.getSchemaForRelease(previousVersion));
			releaseDataManager.dropVersion(previousVersion);
		}
		validationConfig = null;
		
	}
}
