package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
@Transactional
public class RvfAssertionTestHarness {
	
	 @Autowired
	    private AssertionExecutionService assertionExecutionService;
	    @Resource(name = "dataSource")
	    private DataSource dataSource;
	    @Resource(name = "snomedDataSource")
	    private DataSource snomedDataSource;
	    @Autowired
	    private EntityService entityService;
	    @Autowired
	    private AssertionService assertionService;
	    @Autowired
	    private ReleaseDataManager releaseDataManager;
	    @Autowired
	    AssertionDao assertionDao;

	    @Before
	    public void setUp() {

	        assertNotNull(entityService);
	        assertNotNull(releaseDataManager);
	        // register releases with release manager, since they will be used during SQL replacement
	        releaseDataManager.setSchemaForRelease("20140731", "rvf_int_20140731");
	        releaseDataManager.setSchemaForRelease("20150131", "rvf_int_20150131");

	    }
	    
	    @Test
	    public void testAssertion() {
	    	//Assertion 111 and assertion test 107
	    	final Collection<AssertionTest> tests = assertionDao.getAssertionTests(36L);
	    	
	    	for(final AssertionTest test : tests) {
	    		System.out.println(test);
	    	}

	        final Long runId =15012015112L;
			// set both prospective and previous release
	        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, "20150131", "20140731");
	        for (final TestRunItem item : runItems) {
	        	 System.out.println("runItem = " + item);
	 	        System.out.println("runItem.isFailure() = " + item.isFailure());
	        }
	    }
}
