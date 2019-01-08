package org.ihtsdo.rvf.execution.service.impl;

import org.ihtsdo.rvf.execution.service.ExecutionServiceTestConfig;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ExecutionServiceTestConfig.class})
public class ResourceDataLoaderImplIntegrationTest {
	
	@Autowired
	private ResourceDataLoader resourceDataLoader;

	@Test
	public void testLoadResource() {
		try {
			resourceDataLoader.loadResourceData("test");
		} catch (Exception e) {
			Assert.fail("Failed to load resources sucessfully!");
			e.printStackTrace();
		}
	}
}
