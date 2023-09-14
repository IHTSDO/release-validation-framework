package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;


@ContextConfiguration(classes = TestConfig.class)
@Transactional
@Disabled
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

	private Assertion assertion  = new Assertion();
	private final AssertionTest assertionTest = new AssertionTest();
	private org.ihtsdo.rvf.core.data.model.Test test;

	private MysqlExecutionConfig config;

	@BeforeEach
	public void setUp() {
		assertNotNull(assertionService);
		assertNotNull(releaseDataManager);

		// register releases with release manager, since they will be used during SQL replacement
		config = new MysqlExecutionConfig(12345L);
		config.setPreviousVersion("rvf_int_20140131");
		config.setProspectiveVersion("rvf_int_20140731");

		assertion.setAssertionText("Assertion test");
		assertion = assertionService.create(assertion);
		// create test
		test = new org.ihtsdo.rvf.core.data.model.Test();
		test.setType(TestType.SQL);
		test.setName("Test 1");
		assertionService.addTest(assertion, test);
	}

	@Test
	public void testExecuteAssertionTest() {
		assert assertionExecutionService != null;
		assert dataSource != null;
		// set configuration
		final String template = "select  " +
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
		assertEquals("Test must have passed", 0, runItem.getFailureCount());
	}

	@Test
	public void testExecuteAssertionTestWithMultipleStatements() {
		assert assertionExecutionService != null;
		assert dataSource != null;
		// set configuration
		final String template = "create or replace view v_act_langrs as " +
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
		assertEquals("Test must have passed", 0, runItem.getFailureCount());
	}
}