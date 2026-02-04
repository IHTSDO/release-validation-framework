package org.ihtsdo.rvf.core.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
class AssertionExecutionServiceIntegrationTest extends IntegrationTest {
	@Autowired
	private AssertionExecutionService assertionExecutionService;

	@Autowired
	private AssertionService assertionService;

	@Autowired
	private ReleaseDataManager releaseDataManager;

	private Assertion assertion = new Assertion();
	private final AssertionTest assertionTest = new AssertionTest();
	private org.ihtsdo.rvf.core.data.model.Test test;

	private MysqlExecutionConfig config;

	@BeforeEach
	public void setUp() throws BusinessServiceException {
		assertNotNull(assertionService);
		assertNotNull(releaseDataManager);

		// register releases with release manager, since they will be used during SQL replacement
		config = new MysqlExecutionConfig(12345L);
		config.setPreviousVersion("rvf_int_20140131");
		config.setProspectiveVersion("rvf_int_20140131");

		assertion.setAssertionText("Assertion test");
		assertion = assertionService.create(assertion);
		// create test
		test = new org.ihtsdo.rvf.core.data.model.Test();
		test.setType(TestType.SQL);
		test.setName("Test 1");
		assertionService.addTest(assertion.getAssertionId(), test);

		if (!releaseDataManager.isKnownRelease("rvf_int_20140131")) {
			releaseDataManager.uploadPublishedReleaseData(getClass().getResourceAsStream("/SnomedCT_Release_INT_20140131.zip"), "SnomedCT_Release_INT_20140131.zip", "int", "20140131", Collections.emptyList());
		}
	}

	@Test
	public void testExecuteAssertionTest() {
		// given
		ExecutionCommand executionCommand = new ExecutionCommand();
		executionCommand.setTemplate("select * from <PROSPECTIVE>.simplerefset_<DELTA> limit 1;");
		test.setName("Single entry exists");
		test.setCommand(executionCommand);
		assertionTest.setTest(test);
		assertionTest.setAssertion(assertion);

		// when
		TestRunItem testRunItem = assertionExecutionService.executeAssertionTest(assertionTest, config);

		// then
		Assertions.assertEquals(1L, testRunItem.getFailureCount());
	}

	@Test
	public void testExecuteAssertionTestWithMultipleStatements() {
		// given
		ExecutionCommand executionCommand = new ExecutionCommand();
		String templateA = "select * from <PROSPECTIVE>.simplerefset_<DELTA> limit 1;";
		String templateB = "select * from <PROSPECTIVE>.simplemaprefset_<DELTA> limit 1;";
		executionCommand.setTemplate(templateA + templateB);
		test.setName("Multiple entries exists");
		test.setCommand(executionCommand);
		assertionTest.setTest(test);
		assertionTest.setAssertion(assertion);

		// when
		TestRunItem testRunItem = assertionExecutionService.executeAssertionTest(assertionTest, config);

		// then
		Assertions.assertEquals(2L, testRunItem.getFailureCount());
	}
}