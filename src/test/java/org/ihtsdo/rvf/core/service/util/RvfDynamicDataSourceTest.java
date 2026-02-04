package org.ihtsdo.rvf.core.service.util;

import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.service.RvfDynamicDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RvfDynamicDataSourceTest extends IntegrationTest {

	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;

	@Test
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