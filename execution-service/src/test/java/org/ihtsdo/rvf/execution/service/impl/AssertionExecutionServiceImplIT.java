package org.ihtsdo.rvf.execution.service.impl;

import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;
import org.ihtsdo.rvf.helper.Configuration;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
@Transactional
public class AssertionExecutionServiceImplIT {

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
    private Assertion assertion;
    private AssertionTest assertionTest;
    private org.ihtsdo.rvf.entity.Test test;

    @Before
    public void setUp() {
        // ensure database is clean
        assert entityService.count(org.ihtsdo.rvf.entity.Test.class) == 0;
        assert entityService.count(AssertionTest.class) == 0;
        assert entityService.count(ReleaseCenter.class) == 0;
        assert entityService.count(Assertion.class) == 0;

        assertion = assertionService.create(new HashMap<String, String>());
        // create test
        test = new org.ihtsdo.rvf.entity.Test();
        test.setType(TestType.SQL);
        test.setName("Test 1");
        test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
        assert test != null;
        assert test.getId() != null;
        assert entityService.count(org.ihtsdo.rvf.entity.Test.class) > 0;

        //create assertion test
        assertionTest = new AssertionTest();
        assertionTest.setAssertion(assertion);
        assertionTest.setTest(test);
        assertionTest = (AssertionTest) entityService.create(assertionTest);
        assert assertionTest != null;
        assert assertionTest.getId() != null;
        assert entityService.count(AssertionTest.class) > 0;
    }

    @Test
    public void testExecuteAssertionTest() throws Exception {
        assert assertionExecutionService != null;
        assert dataSource != null;
        assert snomedDataSource != null;

        // set configuration
        String template = "" +
                "select  " +
                "concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
                "from <PROSPECTIVE>.concept_<SNAPSHOT> a  " +
                "inner join <PROSPECTIVE>.stated_relationship_<SNAPSHOT> b on a.id = b.id " +
                "where a.active = '1' " +
                "and b.active = '1' " +
                "and a.definitionstatusid != '900000000000074008' " +
                "group by b.sourceid " +
                "having count(*) = 1;";
        Configuration configuration = new Configuration();
        ExecutionCommand command = new ExecutionCommand();
        command.setTemplate(template);
        command.setCode("Execute me".getBytes());
        command.setConfiguration(configuration);
        test.setName("Real - Concept has 1 defining relationship but is not primitive");
        test.setCommand(command);

        assertionTest.setTest(test);

        // set both prospective and previous release
        TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, 1L, "rvf_int_20140731", "postqa");
        assertNotNull(runItem);
        System.out.println("runItem = " + runItem);
        System.out.println("runItem.isFailure() = " + runItem.isFailure());
        assertTrue("Test must have passed", !runItem.isFailure());
    }

    @Test
    public void testExecuteAssertionTestWithMultipleStatements() throws Exception {
        assert assertionExecutionService != null;
        assert dataSource != null;
        assert snomedDataSource != null;

        // set configuration
        String template = "" +
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
        Configuration configuration = new Configuration();
        ExecutionCommand command = new ExecutionCommand();
        command.setTemplate(template);
        command.setCode("Execute me".getBytes());
        command.setConfiguration(configuration);
        test.setName("Fake - Concept has 8 defining relationship but is not primitive");
        test.setCommand(command);

        assertionTest.setTest(test);

        // set both prospective and previous release
        TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, 2L, "rvf_int_20140731", "postqa");
        assertNotNull(runItem);
        assertTrue("Test must have passed", runItem.isFailure());
    }
}