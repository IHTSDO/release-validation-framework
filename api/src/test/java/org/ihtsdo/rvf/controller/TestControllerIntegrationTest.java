package org.ihtsdo.rvf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.helper.Configuration;
import org.ihtsdo.rvf.service.EntityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A test case for {@link org.ihtsdo.rvf.controller.TestController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class TestControllerIntegrationTest {

    @Autowired
    private WebApplicationContext ctx;
    private MockMvc mockMvc;
    @Autowired
    private EntityService entityService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    private org.ihtsdo.rvf.entity.Test test;

    @Before
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        test = new org.ihtsdo.rvf.entity.Test();
        test.setName("Test Name");
        test.setDescription("Example Test Description");

        assert entityService != null;
        assert entityService.count(org.ihtsdo.rvf.entity.Test.class) == 0;
        test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
        assert test != null;
        assert test.getId() != null;
        assert entityService.count(org.ihtsdo.rvf.entity.Test.class) > 0;
    }

    @Test
    public void testGetTests() throws Exception {
        mockMvc.perform(get("/tests").accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/tests").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString(test.getName())));
    }

    @Test
    public void testGetTest() throws Exception {

        Long id = test.getId();
        mockMvc.perform(get("/tests/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/tests/{id}",id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").value(id.intValue()));
    }

    @Test
    public void testDeleteTest() throws Exception {
        Long id = test.getId();
//        mockMvc.perform(delete("/tests/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteMissingTest() throws Exception {
        Long id = 29367234L;
//        mockMvc.perform(delete("/assertions/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testCreateTest() throws Exception {

        String paramsString = objectMapper.writeValueAsString(test);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/tests").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/tests").content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").exists());
    }

    @Test
    public void testUpdateTest() throws Exception {
        Long id = test.getId();
        String updatedName = "Updated Test Name";
        test.setName(updatedName);
        String paramsString = objectMapper.writeValueAsString(test);
        mockMvc.perform(put("/tests/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(put("/tests/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(updatedName));
    }

    @Test
    public void executeTest() throws Exception{
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
//        entityService.create(configuration);
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
        org.ihtsdo.rvf.entity.Test returnedTest = objectMapper.readValue(mockMvc.perform(post("/tests").content(objectMapper.writeValueAsString(executableTest)).contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString(), org.ihtsdo.rvf.entity.Test.class);
//        executableTest = (org.ihtsdo.rvf.entity.Test) entityService.create(executableTest);
        Long executableTestId = returnedTest.getId();
        assert executableTestId != null;

        mockMvc.perform(get("/tests", executableTestId).contentType(MediaType.APPLICATION_JSON)).andDo(print());

        // execute test
        String paramsString = objectMapper.writeValueAsString(returnedTest);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(get("/tests/{id}/run", executableTestId).content(paramsString).param("runId", "1").param("schemaName", "postqa").contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/tests/{id}/run", executableTestId).content(paramsString).param("runId", "1").param("schemaName", "postqa").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("executionId").exists())
                .andExpect(jsonPath("failure").value(false))
                .andExpect(jsonPath("failureMessage").value(Matchers.nullValue()))
                .andExpect(jsonPath("testTime").exists());
    }

    @After
    public void tearDown() throws Exception {
        assert entityService != null;
        if (test != null) {
            entityService.delete(test);
        }
    }
}