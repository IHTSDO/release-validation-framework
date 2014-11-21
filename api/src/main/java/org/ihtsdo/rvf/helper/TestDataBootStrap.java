package org.ihtsdo.rvf.helper;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility class for setting up sample data in the application for test
 */
@Transactional
@Component
public class TestDataBootStrap {

    @Autowired
    private AssertionService assertionService;
    @Autowired
    private EntityService entityService;
    private Assertion assertion;
    private ReleaseCenter releaseCenter;

    public TestDataBootStrap() {
    }

    public void initData(){
        setUpBaseEntities();
        setupExecutableTest();
    }

    public void setUpBaseEntities() {

        releaseCenter = (ReleaseCenter) entityService.create(entityService.getIhtsdo());
        assert releaseCenter.getId() != null;
        assert assertionService != null;
        assertion = new Assertion();
        assertion.setName("Assertion Name");
        assertion.setName("Assertion Description");
        assertion.setDocLink("http://snomed.org/tig/1.pdf");
        assertion.setStatement("Human readable statement");
        assertion = assertionService.create(assertion);
        assert assertion != null;
    }

    public void setupExecutableTest(){
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
        entityService.create(configuration);
        ExecutionCommand command = new ExecutionCommand();
        command.setTemplate(template);
        command.setCode("Execute me".getBytes());
        command.setConfiguration(configuration);

        String execTestName = "Real - Concept has 1 defining relationship but is not primitive";
        org.ihtsdo.rvf.entity.Test executableTest = new org.ihtsdo.rvf.entity.Test();
        executableTest.setName(execTestName);
//        executableTest.setConfiguration(configuration);
        executableTest.setCommand(command);
        executableTest.setType(TestType.SQL);
        executableTest = (org.ihtsdo.rvf.entity.Test) entityService.create(executableTest);
        Long executableTestId = executableTest.getId();
        assert executableTestId != null;

        // associate test with assertion
        assertionService.addTest(assertion, executableTest);
    }
}
