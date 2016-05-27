package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
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
	
	 private static final String PROSPECTIVE_RELEASE = "20150731";
	private static final String PREVIOUS_RELEASE = "20150131";
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
	    @Autowired
	    private ResourceDataLoader resourceDataLoader;

	    @Before
	    public void setUp() {

	        assertNotNull(entityService);
	        assertNotNull(releaseDataManager);
	        // register releases with release manager, since they will be used during SQL replacement
	        releaseDataManager.setSchemaForRelease(PREVIOUS_RELEASE, "rvf_regression_test_prospective");
	        releaseDataManager.setSchemaForRelease(PROSPECTIVE_RELEASE, "rvf_regression_test_previous");

	    }
	    
	    @Test
	    public void testAssertions() throws BusinessServiceException, SQLException, IOException {
	    	
	    	 resourceDataLoader.loadResourceData(releaseDataManager.getSchemaForRelease(PROSPECTIVE_RELEASE));
	        final List<Assertion> resources = assertionDao.getAssertionsByContainingKeyword("resource");
	        final Long runId =201503130928L;
			final ExecutionConfig config = new ExecutionConfig(runId);
			config.setPreviousVersion(PREVIOUS_RELEASE);
			config.setProspectiveVersion(PROSPECTIVE_RELEASE);
			assertionExecutionService.executeAssertions(resources, config);
	    	//Assertion 111 and assertion test 107
	    	//36L
	    	//150L
	    	//"release-type-validation", "component-centric-validation","file-centric-validation"
			//complexAndExtendedMapRefsetValidation
	    	final List<String> groupNames = Arrays.asList("complexAndExtendedMapRefsetValidation");
	    	final List<AssertionGroup> groups = new ArrayList<>();
	    	for (final String name : groupNames) {
	    		groups.add(assertionDao.getAssertionGroupsByName(name));
	    	}
	    	final Collection<Assertion> assertions = new ArrayList<>();
	    	final Assertion single = new Assertion();
	    	single.setId(97L);
	    	assertions.add(single);
//	    	for (final AssertionGroup group: groups) {
//	    		assertions.addAll(assertionDao.getAssertionsForGroup(group.getId()));
//	    	}
	    	
	    	final List<AssertionTest> tests = new ArrayList<>();
	    	final AssertionTest test = new AssertionTest();
	    	for(final Assertion assertion : assertions) {
	    		tests.addAll(assertionDao.getAssertionTests(assertion));
	    	}
	       
	        System.out.println("RunID:" + runId);
			// set both prospective and previous release
	        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, config);
	        System.out.println("TOTAL of assertions run:" + runItems.size());
	    }
	    
	    
	    
	    @Test
	    public void testProspectiveVersion() throws BusinessServiceException, SQLException, IOException {
	    	
	    	 resourceDataLoader.loadResourceData(releaseDataManager.getSchemaForRelease(PROSPECTIVE_RELEASE));
	        final List<Assertion> resources = assertionDao.getAssertionsByContainingKeyword("resource");
	        final Long runId =201503130928L;
			final ExecutionConfig config = new ExecutionConfig(runId);
			config.setPreviousVersion(PREVIOUS_RELEASE);
			config.setProspectiveVersion(PROSPECTIVE_RELEASE);
			assertionExecutionService.executeAssertions(resources, config);
	    	//Assertion 111 and assertion test 107
	    	//36L
	    	//150L
	    	//"release-type-validation", "component-centric-validation","file-centric-validation"
			//complexAndExtendedMapRefsetValidation
	    	final List<String> groupNames = Arrays.asList("SnapshotContentValidation");
	    	final List<AssertionGroup> groups = new ArrayList<>();
	    	for (final String name : groupNames) {
	    		groups.add(assertionDao.getAssertionGroupsByName(name));
	    	}
	    	final Collection<Assertion> assertions = new ArrayList<>();
//	    	final Assertion single = new Assertion();
//	    	single.setId(28L);
//	    	assertions.add(single);
	    	for (final AssertionGroup group: groups) {
	    		assertions.addAll(assertionDao.getAssertionsForGroup(group.getId()));
	    	}
	    	
	    	final List<AssertionTest> tests = new ArrayList<>();
	    	final AssertionTest test = new AssertionTest();
	    	for(final Assertion assertion : assertions) {
	    		tests.addAll(assertionDao.getAssertionTests(assertion));
	    	}
	       
	        System.out.println("RunID:" + runId);
			// set both prospective and previous release
	        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, config);
	        System.out.println("TOTAL of assertions run:" + runItems.size());
	    }
}
