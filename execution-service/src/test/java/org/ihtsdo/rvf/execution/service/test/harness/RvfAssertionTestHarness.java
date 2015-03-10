package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
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
	        releaseDataManager.setSchemaForRelease("spanishedition_20140430", "rvf_int_spanishedition_20140430");
	        releaseDataManager.setSchemaForRelease("spanishedition_20141031", "rvf_int_spanishedition_20141031");

	    }
	    
	    @Test
	    public void testAssertions() {
	    	//Assertion 111 and assertion test 107
	    	//36L
	    	//150L
	    	final Collection<Assertion> assertions = assertionDao.getAssertionsForGroup(3L);
	    	final List<AssertionTest> tests = new ArrayList<>();
	    	for(final Assertion assertion : assertions) {
	    		tests.addAll(assertionDao.getAssertionTests(assertion));
	    	}
	    	//15012015112L;
	    	//1425919799121L
	        final Long runId =201503101114L;
	        System.out.println("RunID:" + runId);
			// set both prospective and previous release
	        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, "spanishedition_20141031", "spanishedition_20140430");
	        for (final TestRunItem item : runItems) {
	        	 System.out.println("runItem = " + item);
	 	        System.out.println("runItem.isFailure() = " + item.isFailure());
	        }
	    }
}
