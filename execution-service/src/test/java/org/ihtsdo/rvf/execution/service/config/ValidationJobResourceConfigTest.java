package org.ihtsdo.rvf.execution.service.config;

import static org.junit.Assert.*;

import org.ihtsdo.rvf.execution.service.ExecutionServiceTestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ExecutionServiceTestConfig.class})
public class ValidationJobResourceConfigTest {
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Test
	public void testUseCloud() {
		assertEquals(false, jobResourceConfig.isUseCloud());
	}
	
	@Test
	public void testGetCloud() {
		assertTrue(jobResourceConfig.getCloud().getBucketName().isEmpty());
	}
	@Test
	public void testGetLocalPath() {
		assertEquals("store/jobs/",jobResourceConfig.getLocal().getPath());
	}
}
