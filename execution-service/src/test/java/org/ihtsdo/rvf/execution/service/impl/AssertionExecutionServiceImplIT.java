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

import javax.sql.DataSource;
import java.util.HashMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testApplicationContext.xml"})
@Transactional
public class AssertionExecutionServiceImplIT {

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

        // create release centre
        releaseCenter = new ReleaseCenter();
        releaseCenter.setName("Test release centre 1");
        releaseCenter = (ReleaseCenter) entityService.create(releaseCenter);
        assert releaseCenter != null;
        assert releaseCenter.getId() != null;
        assert entityService.count(releaseCenter.getClass()) > 0;

        //create assertion test
        assertionTest = new AssertionTest();
        assertionTest.setAssertion(assertion);
        assertionTest.setTest(test);
        assertionTest.setCenter(releaseCenter);
        assertionTest = (AssertionTest) entityService.create(assertionTest);
        assert assertionTest != null;
        assert assertionTest.getId() != null;
        assert entityService.count(AssertionTest.class) > 0;
    }

    @Test
    public void testExecuteAssertionTest() throws Exception {
        assert assertionExecutionService != null;
        assert qaDataSource != null;

        // set configuration
        String template = "" +
                "select  " +
                "concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
                "from curr_concept_table_name a  " +
                "inner join curr_stated_relationship_table_name b on a.id_column_name = b.source_id_column_name " +
                "where a.active_column_name = '1' " +
                "and b.active_column_name = '1' " +
                "and a.definition_status_id != '900000000000074008' " +
                "group by b.source_id_column_name " +
                "having count(*) = 1;";
        Configuration configuration = new Configuration();
        configuration.setValue("curr_concept_table_name", "curr_concept_s");
        configuration.setValue("curr_stated_relationship_table_name", "curr_stated_relationship_s");
        configuration.setValue("id_column_name", "id");
        configuration.setValue("active_column_name", "active");
        configuration.setValue("definition_status_id", "definitionstatusid");
        configuration.setValue("source_id_column_name", "sourceid");
        ExecutionCommand command = new ExecutionCommand();
        command.setTemplate(template);
        command.setCode("Execute me".getBytes());
        command.setConfiguration(configuration);
        test.setName("Real - Concept has 1 defining relationship but is not primitive");
        test.setConfiguration(configuration);
        test.setCommand(command);

        assertionTest.setTest(test);

        TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, 1);
        assert runItem != null;
        assert ! runItem.isFailure();
    }

    @Test
    public void testExecuteAssertionTestWithMultipleStatements() throws Exception {
        assert assertionExecutionService != null;
        assert qaDataSource != null;

        // set configuration
        String template = "" +
                "create or replace view v_act_langrs as " +
                "select referencedcomponentid " +
                "from curr_langrefset_s  " +
                "where active = '1';" +
                "select  " +
                "concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
                "from curr_concept_table_name a  " +
                "inner join curr_stated_relationship_table_name b on a.id_column_name = b.source_id_column_name " +
                "where a.active_column_name = '1' " +
                "and b.active_column_name = '1' " +
                "and a.definition_status_id != '900000000000074008' " +
                "group by b.source_id_column_name " +
                "having count(*) = 8;";
        Configuration configuration = new Configuration();
        configuration.setValue("curr_concept_table_name", "curr_concept_s");
        configuration.setValue("curr_stated_relationship_table_name", "curr_stated_relationship_s");
        configuration.setValue("id_column_name", "id");
        configuration.setValue("active_column_name", "active");
        configuration.setValue("definition_status_id", "definitionstatusid");
        configuration.setValue("source_id_column_name", "sourceid");
        ExecutionCommand command = new ExecutionCommand();
        command.setTemplate(template);
        command.setCode("Execute me".getBytes());
        command.setConfiguration(configuration);
        test.setName("Fake - Concept has 8 defining relationship but is not primitive");
        test.setConfiguration(configuration);
        test.setCommand(command);

        assertionTest.setTest(test);

        TestRunItem runItem = assertionExecutionService.executeAssertionTest(assertionTest, 1);
        assert runItem != null;
        assert runItem.isFailure();
    }
}