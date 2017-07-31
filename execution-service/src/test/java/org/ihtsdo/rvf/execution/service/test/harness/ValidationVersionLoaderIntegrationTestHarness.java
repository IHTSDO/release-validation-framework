package org.ihtsdo.rvf.execution.service.test.harness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
import org.ihtsdo.rvf.execution.service.impl.RvfDbScheduledEventGenerator;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationVersionLoader;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
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
public class ValidationVersionLoaderIntegrationTestHarness {
	@Resource(name = "dataSource")
	private DataSource dataSource;
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	private static final String FAILURE_MESSAGE = "failureMessage";
	
	@Autowired
	ValidationVersionLoader dataLoader;
	private String prospectiveVersion;
	private String previousVersion;
	private ValidationRunConfig validationConfig;
	
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
		List<String> filesLoaded = dataLoader.loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(prospectiveVersion, validationConfig,null);
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
		validationConfig.setPrevIntReleaseVersion("SnomedCT_RF2Release_INT_20130131.zip");
		validationConfig.setS3PublishBucketName("local.publish.bucket");
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadPreviousVersion(executionConfig, validationConfig, responseMap);
		Assert.assertEquals(false, isLoaded);
		System.out.println(responseMap.get(FAILURE_MESSAGE));
		Assert.assertNotNull(responseMap.get(FAILURE_MESSAGE).toString());
		
	}
	
	@Test
	public void testLoadPreviousIntDerivativeVersion() throws Exception {
		
		validationConfig.setExtensionDependency("int_20160131");
		validationConfig.setPreviousExtVersion("SnomedCT_GPFPICPC2_Production_INT_20160731.zip");
		validationConfig.setS3PublishBucketName("local.publish.bucket");
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadPreviousVersion(executionConfig, validationConfig, responseMap);
		Assert.assertEquals(false, isLoaded);
		System.out.println(responseMap.get(FAILURE_MESSAGE));
		Assert.assertNotNull(responseMap.get(FAILURE_MESSAGE).toString());
	}
	
	
	@Test
	public void testLoadPreviousExtensionVersion() throws Exception {
		validationConfig.setExtensionDependency("int_20160131");
		validationConfig.setPreviousExtVersion("SnomedCT_RF2Release_SE1000052_20160531.zip");
		validationConfig.setS3PublishBucketName("local.publish.bucket");
		ExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		boolean isLoaded = dataLoader.loadPreviousVersion(executionConfig, validationConfig, responseMap);
		Assert.assertEquals(false, isLoaded);
		System.out.println(responseMap.get(FAILURE_MESSAGE));
		Assert.assertNotNull(responseMap.get(FAILURE_MESSAGE).toString());
	}
	
	@After
	public void tearDown() throws SQLException {
		if (prospectiveVersion != null) {
			releaseDataManager.dropVersion(prospectiveVersion);
		}
		if (previousVersion != null) {
			releaseDataManager.dropVersion(previousVersion);
		}
		validationConfig = null;
	}
	
	@Test
	public void testCopyFile() throws IOException {
		File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_" + validationConfig.getTestFileName(), ".zip");
		FileOutputStream out = new FileOutputStream(prospectiveFile);
		InputStream input = new FileInputStream(ClassLoader.getSystemResource("Daily_Export_Delta.zip").getFile());
		IOUtils.copy(input,out);
		IOUtils.closeQuietly(input);
		IOUtils.closeQuietly(out);
		Assert.assertTrue(prospectiveFile.isFile());
		ResourceProvider resourceManager = new ZipFileResourceProvider(prospectiveFile);
		Assert.assertTrue(!resourceManager.getFileNames().isEmpty());
		
	}
}
