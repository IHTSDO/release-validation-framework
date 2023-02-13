package org.ihtsdo.rvf.core.service.config;

import org.ihtsdo.rvf.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
public class ValidationJobResourceConfigTest {
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
