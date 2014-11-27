package org.ihtsdo.rvf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.helper.Configuration;
import org.ihtsdo.rvf.service.AssertionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A test case for {@link org.ihtsdo.rvf.controller.AssertionGroupController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class AssertionGroupControllerIntegrationTest {

    @Autowired
    private WebApplicationContext ctx;
    private MockMvc mockMvc;
    @Autowired
    private AssertionService assertionService;
    @Autowired
    private EntityService entityService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    private AssertionGroup group;

    @Before
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        group = new AssertionGroup();
        group.setName("Test group");

        assertNotNull(assertionService);
        assertNotNull(entityService);
        group = (AssertionGroup) entityService.create(group);
        assertNotNull(group.getId());
    }

    @Test
    public void testGetGroups() throws Exception {
        mockMvc.perform(get("/groups").accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/groups").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString(group.getName())));
    }

    @Test
    public void testGetGroup() throws Exception {

        Long id = group.getId();
        mockMvc.perform(get("/groups/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/groups/{id}",id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").value(id.intValue()));
    }

    @Test
    public void testDeleteGroup() throws Exception {
        Long id = group.getId();
//        mockMvc.perform(delete("/assertions/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteMissingGroup() throws Exception {
        Long id = 29367234L;
        mockMvc.perform(delete("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testGetMissingGroup() throws Exception {
        Long id = 29367234L;
        mockMvc.perform(get("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testCreateGroup() throws Exception {

        String name = "New Test Assertion Group";
        mockMvc.perform(post("/groups").param("name", name).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/groups").param("name", name).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").exists());
    }

    @Test
    public void testUpdateGroup() throws Exception {
        Long id = group.getId();
        String updatedName = "Updated Assertion Group Name";
        group.setName(updatedName);
        String paramsString = objectMapper.writeValueAsString(group);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(put("/groups/{id}", id).param("name", updatedName).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(put("/groups/{id}", id).param("name", updatedName).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(updatedName));
    }

    @Test
    public void testGetAssertionsForGroup() throws Exception {
        Long id = group.getId();
        // create and add some assertions
        assertionService.addAssertionToGroup(getRandomAssertion(), group);
        assertionService.addAssertionToGroup(getRandomAssertion(), group);

        mockMvc.perform(get("/groups/{id}/assertions", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/groups/{id}/assertions", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test assertion")));
    }

    @Test
    public void testAddAssertionsToGroup() throws Exception {
        Long id = group.getId();
        // create and add some assertions
        List<Assertion> assertions = new ArrayList<>();
        assertions.add(getRandomAssertion());
        assertions.add(getRandomAssertion());

        String paramsString = objectMapper.writeValueAsString(assertions);
        System.out.println("paramsString = " + paramsString);
//        mockMvc.perform(post("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8));

        // getting tests for assertion should now contain response with text Random Test
        mockMvc.perform(get("/groups/{id}/assertions", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test assertion")));
    }

    @Test
    public void testDeleteTestsForAssertion() throws Exception {
        Long id = group.getId();
        // create and add some assertions
        List<Assertion> assertions = new ArrayList<>();
        assertions.add(getRandomAssertion());
        assertions.add(getRandomAssertion());

        String paramsString = objectMapper.writeValueAsString(assertions);
        System.out.println("paramsString = " + paramsString);
//        mockMvc.perform(post("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8));
    }


    @After
    public void tearDown() throws Exception {
        assert entityService != null;
        if (group != null) {
            entityService.delete(group);
        }
    }

    private Assertion getRandomAssertion(){
        org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
        test.setName("Random Test " + UUID.randomUUID());
        test.setCommand(new ExecutionCommand(new Configuration()));

        Assertion assertion = new Assertion();
        assertion.setName("Test assertion");
        assertion.setDescription("Test assertion description");
        // save assertion
        assertion = assertionService.addTest(assertion, test);
        assertNotNull(assertion.getId());

        return assertion;
    }
}