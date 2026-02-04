package org.ihtsdo.rvf.core.service.config;

import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class ValidationJobResourceConfigTest extends IntegrationTest {
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@org.junit.jupiter.api.Test
	public void testUseCloud() {
		assertFalse(jobResourceConfig.isUseCloud());
	}
	
	@org.junit.jupiter.api.Test
	public void testGetCloud() {
		assertTrue(jobResourceConfig.getCloud().getBucketName().isEmpty());
	}
	@Test
	public void testGetLocalPath() {
		assertEquals("store/jobs/",jobResourceConfig.getLocal().getPath());
	}
}
