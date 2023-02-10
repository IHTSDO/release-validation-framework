package org.ihtsdo.rvf.core.service.util;

import static org.junit.Assert.assertNotNull;

import java.sql.Connection;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.service.RvfDynamicDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class RvfDynamicDataSourceTest {

	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;

	@Test
	@Ignore
	public void testGettingLargeNumberOfConnections() throws Exception {
		assertNotNull(rvfDynamicDataSource);
		// try and 10 connections
		for(int i=0; i<10; i++){
			System.out.print("Connection : " + i + ",");
			try (Connection connection = rvfDynamicDataSource.getConnection("rvf_master")) {
				assertNotNull(connection);
			}
		}
	}
}