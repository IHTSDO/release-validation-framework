package org.ihtsdo.rvf.core.service;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.ihtsdo.rvf.core.service.structure.resource.ZipFileResourceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class ValidationVersionLoaderIntegrationTest extends IntegrationTest {
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	private static final String FAILURE_MESSAGE = "failureMessage";
	
	@Autowired
	private ValidationVersionLoader dataLoader;
	private String prospectiveVersion;
	private ValidationRunConfig validationConfig;
	
	@BeforeEach
	public void setUp() {
		long runId = System.currentTimeMillis();
		validationConfig = new ValidationRunConfig();
		validationConfig.setFailureExportMax(100);
		validationConfig.setRunId(runId);
		validationConfig.setGroupsList(List.of("file-centric-validation"));
		new File(ClassLoader.getSystemResource("Daily_Export_Delta.zip").getFile());
	}
	
	@Test
	public void testConstructIntProspectiveVersionWithRF2DeltaOnly() throws BusinessServiceException {
		prospectiveVersion = validationConfig.getRunId().toString();
		MysqlExecutionConfig executionConfig = new MysqlExecutionConfig(validationConfig.getRunId());
		executionConfig.setProspectiveVersion(prospectiveVersion);
		executionConfig.setRf2DeltaOnly(true);
		List<String> filesLoaded = dataLoader.loadProspectiveDeltaAndCombineWithPreviousSnapshotIntoDB(executionConfig, null, null);
		assertEquals(1, filesLoaded.size());
		assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}

	@Test
	public void testConstructExtensionProspectiveVersionWithRF2DeltaOnly() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		validationConfig.addExtensionDependency("int_20160131");
		validationConfig.addPreviousRelease("dk_20160215");
		validationConfig.setRf2DeltaOnly(true);
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		ValidationStatusReport statusReport = new ValidationStatusReport(validationConfig);
		dataLoader.loadProspectiveVersion(null, statusReport, executionConfig, null);
		assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}

	@Test
	public void testProspectiveVersion() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		ValidationStatusReport statusReport = new ValidationStatusReport(validationConfig);
		dataLoader.loadProspectiveVersion(null, statusReport, executionConfig, null);
		assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	}

	@Test
	public void testProspectiveVersionWithExtension() throws Exception {
		prospectiveVersion = validationConfig.getRunId().toString();
		validationConfig.addExtensionDependency("int_20160131");
		validationConfig.addPreviousRelease("dk_20160215");
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		executionConfig.setRf2DeltaOnly(false);
		ValidationStatusReport statusReport = new ValidationStatusReport(validationConfig);
		dataLoader.loadProspectiveVersion(null, statusReport, executionConfig, null);
		assertTrue(releaseDataManager.isKnownRelease(prospectiveVersion));
	} 
	
	@Test
	public void testLoadPreviousVersion() throws Exception {
		validationConfig.setPreviousRelease("SnomedCT_RF2Release_INT_20130131.zip");
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		dataLoader.loadPreviousVersion( validationConfig.getPreviousRelease(), null, executionConfig);
		System.out.println(responseMap.get(FAILURE_MESSAGE));
		assertNotNull(responseMap.get(FAILURE_MESSAGE).toString());
	}
	
	@Test
	public void testLoadPreviousIntDerivativeVersion() throws Exception {
		
		validationConfig.addExtensionDependency("int_20160131");
		validationConfig.setPreviousRelease("SnomedCT_GPFPICPC2_Production_INT_20160731.zip");
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		Map<String, Object> responseMap = new HashMap<>();
		dataLoader.loadPreviousVersion(validationConfig.getPreviousRelease(), null, executionConfig);
		System.out.println(responseMap.get(FAILURE_MESSAGE));
		assertNotNull(responseMap.get(FAILURE_MESSAGE).toString());
	}
	
	
	@Test
	public void testLoadPreviousExtensionVersion() throws Exception {
		validationConfig.addExtensionDependency("int_20160131");
		validationConfig.setPreviousRelease("SnomedCT_RF2Release_SE1000052_20160531.zip");
		MysqlExecutionConfig executionConfig = dataLoader.createExecutionConfig(validationConfig);
		dataLoader.loadPreviousVersion(validationConfig.getPreviousRelease(), null, executionConfig);
	}
	
	@AfterEach
	public void tearDown() {
		validationConfig = null;
	}

	@Test
	public void testCopyFile() throws IOException {
		File prospectiveFile = File.createTempFile(validationConfig.getRunId() + "_" + validationConfig.getTestFileName(), ".zip");
		try (FileOutputStream out = new FileOutputStream(prospectiveFile); InputStream input = new FileInputStream(ClassLoader.getSystemResource("Daily_Export_Delta.zip").getFile())) {
			IOUtils.copy(input, out);
		}
		assertTrue(prospectiveFile.isFile());
		ResourceProvider resourceManager = new ZipFileResourceProvider(prospectiveFile);
		assertFalse(resourceManager.getFileNames().isEmpty());
	}
}
