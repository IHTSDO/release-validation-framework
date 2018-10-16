package org.ihtsdo.rvf.execution.service.impl;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.MysqlConfig;
import org.ihtsdo.rvf.execution.service.ExecutionServiceConfig;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ExecutionServiceConfig.class, MysqlConfig.class})
public class ResourceDataLoaderImplIntegrationTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	@Autowired
	ResourceDataLoader resourceDataLoader;

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
