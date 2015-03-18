package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
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
	
	 private static final String PROSPECTIVE_RELEASE = "20141031";
	private static final String PREVIOUS_RELEASE = "20140430";
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
	        releaseDataManager.setSchemaForRelease(PREVIOUS_RELEASE, "rvf_int_spanishedition_20140430");
	        releaseDataManager.setSchemaForRelease(PROSPECTIVE_RELEASE, "rvf_int_spanishedition_20141031");

	    }
	    
	    @Test
	    public void testAssertions() {
	    	//Assertion 111 and assertion test 107
	    	//36L
	    	//150L
	    	final List<String> groupNames = Arrays.asList("file-centric-validation", "release-type-validation", "component-centric-validation");
	    	final List<AssertionGroup> groups = new ArrayList<>();
	    	for (final String name : groupNames) {
	    		groups.add(assertionDao.getAssertionGroupsByName(name));
	    	}
	    	final Collection<Assertion> assertions = new ArrayList<>();
//	    	final Assertion assert79 = new Assertion();
//	    	assert79.setId(79L);
//	    	assertions.add(assert79);
	    	for (final AssertionGroup group: groups) {
	    		assertions.addAll(assertionDao.getAssertionsForGroup(group.getId()));
	    	}
	    	
	    	final List<AssertionTest> tests = new ArrayList<>();
	    	final AssertionTest test = new AssertionTest();
	    	for(final Assertion assertion : assertions) {
	    		tests.addAll(assertionDao.getAssertionTests(assertion));
	    	}
	    	//15012015112L;
	    	//1425919799121L
	    	//201503101114L;
	    	//201503120846L
	    	//201503120939L;
	    	//201503121422L;
	    	//201503122311L;
	    	//201503130923L;
	        final Long runId =201503130924L;
	        System.out.println("RunID:" + runId);
			// set both prospective and previous release
	        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, PROSPECTIVE_RELEASE, PREVIOUS_RELEASE);
	        for (final TestRunItem item : runItems) {
	        	 System.out.println("runItem = " + item);
	 	        System.out.println("runItem.isFailure() = " + item.isFailure());
	        }
	    }
}
