package org.ihtsdo.rvf.executionservice;

import org.ihtsdo.rvf.dataservice.service.AssertionService;
import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.executionservice.config.MysqlExecutionConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ExecutionServiceTestConfig.class)
@Transactional
@Ignore
public class AssertionExecutionServiceIntegrationTest {
	private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceIntegrationTest.class);
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	
	@Resource(name = "dataSource")
	private DataSource dataSource;
	
	@Autowired
	private AssertionService assertionService;
	
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	private Assertion assertion;
	private AssertionTest assertionTest;
	private org.ihtsdo.rvf.entity.Test test;

	private MysqlExecutionConfig config;

	@Before
	public void setUp() {
		assertNotNull(assertionService);
		assertNotNull(releaseDataManager);

		// register releases with release manager, since they will be used during SQL replacement
		config = new MysqlExecutionConfig(12345L);
		config.setPreviousVersion("rvf_int_20140131");
		config.setProspectiveVersion("rvf_int_20140731");

		assertion  = new Assertion();
		assertion.setAssertionText("Assertion test");
		assertion = assertionService.create(assertion);
		// create test
		test = new org.ihtsdo.rvf.entity.Test();
		test.setType(TestType.SQL);
		test.setName("Test 1");
		assertionService.addTest(assertion, test);
	}

	@Test
	public void testExecuteAssertionTest() throws Exception {
		assert assertionExecutionService != null;
		assert dataSource != null;
		// set configuration
		final String template = "" +
				"select  " +
				"concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
				"from <PROSPECTIVE>.concept_<SNAPSHOT> a  " +
				"inner join <PROSPECTIVE>.stated_relationship_<SNAPSHOT> b on a.id = b.id " +
				"where a.active = '1' " +
				"and b.active = '1' " +
				"and a.definitionstatusid != '900000000000074008' " +
				"group by b.sourceid " +
				"having count(*) = 1;";
		final ExecutionCommand command = new ExecutionCommand();
		command.setTemplate(template);
		test.setName("Real - Concept has 1 defining relationship but is not primitive");
		test.setCommand(command);

		assertionTest.setTest(test);

		// set both prospective and previous release
		final TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, config);
		assertNotNull(runItem);
		logger.debug("runItem = " + runItem);
		assertEquals("Test must have passed", 0, (long) runItem.getFailureCount());
	}

	@Test
	public void testExecuteAssertionTestWithMultipleStatements() throws Exception {
		assert assertionExecutionService != null;
		assert dataSource != null;
		// set configuration
		final String template = "" +
				"create or replace view v_act_langrs as " +
				"select referencedcomponentid " +
				"from <PROSPECTIVE>.langrefset_<SNAPSHOT> " +
				"where active = '1';" +
				"select  " +
				"concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
				"from <PROSPECTIVE>.concept_<SNAPSHOT> a  " +
				"inner join <PROSPECTIVE>.stated_relationship_<SNAPSHOT> b on a.id = b.id " +
				"where a.active = '1' " +
				"and b.active = '1' " +
				"and a.definitionstatusid != '900000000000074008' " +
				"group by b.sourceid " +
				"having count(*) = 8;";
		final ExecutionCommand command = new ExecutionCommand();
		command.setTemplate(template);
		test.setName("Fake - Concept has 8 defining relationship but is not primitive");
		test.setCommand(command);

		assertionTest.setTest(test);

		// set both prospective and previous release
		final TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, config);
		assertNotNull(runItem);
		assertEquals("Test must have passed", 0, (long) runItem.getFailureCount());
	}
}