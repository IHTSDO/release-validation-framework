package org.ihtsdo.rvf.execution.service.impl;

import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.HashMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testApplicationContext.xml"})
@Transactional
public class AssertionExecutionServiceImplTest {

    @Autowired
    private AssertionExecutionService assertionExecutionService;
    @Autowired
    private DataSource qaDataSource;
    @Autowired
    private EntityService entityService;
    @Autowired
    private AssertionService assertionService;
    private Assertion assertion;
    private ReleaseCenter releaseCenter;
    private AssertionTest assertionTest;
    private org.ihtsdo.rvf.entity.Test test;

    @Before
    public void setUp() {
        // ensure database is clean
        assert entityService.findAll(org.ihtsdo.rvf.entity.Test.class).size() == 0;
        assert entityService.findAll(AssertionTest.class).size() == 0;
        assert entityService.findAll(ReleaseCenter.class).size() == 0;
        assert entityService.findAll(Assertion.class).size() == 0;

        assertion = assertionService.create("Assertion 1", new HashMap<String, String>());
        // create test
        test = new org.ihtsdo.rvf.entity.Test();
        test.setType(TestType.SQL);
        test.setName("Test 1");
        test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
        assert test != null;
        assert test.getId() != null;
        assert entityService.findAll(org.ihtsdo.rvf.entity.Test.class).size() > 0;

        // create release centre
        releaseCenter = new ReleaseCenter();
        releaseCenter.setName("Test release centre 1");
        releaseCenter = (ReleaseCenter) entityService.create(releaseCenter);
        assert releaseCenter != null;
        assert releaseCenter.getId() != null;
        assert entityService.findAll(releaseCenter).size() > 0;

        //create assertion test
        assertionTest = new AssertionTest();
        assertionTest.setAssertion(assertion);
        assertionTest.setTest(test);
        assertionTest.setCenter(releaseCenter);
        assertionTest = (AssertionTest) entityService.create(assertionTest);
        assert assertionTest != null;
        assert assertionTest.getId() != null;
        assert entityService.findAll(AssertionTest.class).size() > 0;
    }

    @Test
    public void testExecuteAssertionTest() throws Exception {
        assert assertionExecutionService != null;
        assert qaDataSource != null;

        ExecutionCommand command = new ExecutionCommand();
        command.setCode("Execute me".getBytes());
        test.setCommand(command);
        assertionTest.setTest(test);

        TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest);
        assert runItem != null;
        assert runItem.isFailure();
    }
}