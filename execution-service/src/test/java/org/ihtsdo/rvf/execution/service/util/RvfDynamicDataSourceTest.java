package org.ihtsdo.rvf.execution.service.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
public class RvfDynamicDataSourceTest {

    @Autowired
    private RvfDynamicDataSource rvfDynamicDataSource;

    @Test
    public void testGettingLargeNumberOfConnections() throws Exception {
        assertNotNull(rvfDynamicDataSource);

        // try and 5000 connections
        for(int i=0; i<5000; i++){
            System.out.print("Connection : " + i + ",");
            Connection connection = rvfDynamicDataSource.getConnection("rvf_int_20140731");
            assertNotNull(connection);
            connection.close();
        }
    }
}