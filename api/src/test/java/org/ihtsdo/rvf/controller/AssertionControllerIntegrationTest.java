package org.ihtsdo.rvf.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.ReleaseCenter;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test case for {@link org.ihtsdo.rvf.controller.AssertionController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class AssertionControllerIntegrationTest {

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
    private Map<String, String> params;
    private Assertion assertion;
    private ReleaseCenter releaseCenter;

    @Before
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        params = new HashMap<String, String>();
        params.put("name", "Assertion Name");
        params.put("description", "Assertion Description");
        params.put("statement", "Assertion Statement");
        params.put("docLink", "Assertion Doc Link");

        releaseCenter = (ReleaseCenter) entityService.create(entityService.getIhtsdo());
        assert releaseCenter.getId() != null;
        assert assertionService != null;
        assertion = assertionService.create(params);
        assert assertion != null;
    }

    @Test
    public void testGetAssertions() throws Exception {
        mockMvc.perform(get("/assertions").accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/assertions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString(assertion.getAssertionText())));
    }

    @Test
    public void testGetAssertion() throws Exception {

        final Long id = assertion.getId();
        mockMvc.perform(get("/assertions/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/assertions/{id}",id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").value(id.intValue()));
    }

    @Test
    public void testDeleteAssertion() throws Exception {
        final Long id = assertion.getId();
//        mockMvc.perform(delete("/assertions/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteMissingAssertion() throws Exception {
        final Long id = 29367234L;
        mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testGetMissingAssertion() throws Exception {
        final Long id = 29367234L;
        mockMvc.perform(get("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testCreateAssertion() throws Exception {

        final Assertion assertion1 = new Assertion();
        assertion1.setAssertionText("Testing assertion");
        String paramsString = objectMapper.writeValueAsString(assertion1);
        System.out.println("paramsString = " + paramsString);
        // we have to strip the id property added by Jackson since this causes conflicts when Spring tries to convert content into Assertion
        paramsString = paramsString.replaceAll("\"id\":null,", "");
        System.out.println("paramsString after = " + paramsString);
        mockMvc.perform(post("/assertions").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/assertions").content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").exists());
    }

    @Test
    public void testUpdateAssertion() throws Exception {
        final Long id = assertion.getId();
        final String updatedName = "Updated Assertion Name";
        params.put("name", updatedName);
        final String paramsString = objectMapper.writeValueAsString(params);
        mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(updatedName));
    }

    @Test
    public void testGetTestsForAssertion() throws Exception {
        final Long id = assertion.getId();
        // create and add some tests to assertion
        assertionService.addTest(assertion, getRandomTest());
        assertionService.addTest(assertion, getRandomTest());

        mockMvc.perform(get("/assertions/{id}/tests", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/assertions/{id}/tests", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString("Random Test")));
    }

    @Test
    public void testAddTestsForAssertion() throws Exception {
        final Long id = assertion.getId();
        // create and add some tests to assertion
        final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
        final org.ihtsdo.rvf.entity.Test test1 = getRandomTest();
        assertNotNull(test1.getId());
        tests.add(test1);
        final org.ihtsdo.rvf.entity.Test test2 = getRandomTest();
        assertNotNull(test2.getId());
        tests.add(test2);

        final String paramsString = objectMapper.writeValueAsString(tests);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8));

        // getting tests for assertion should now contain response with text Random Test
        mockMvc.perform(get("/assertions/{id}/tests", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString("Random Test")));
    }

    @Test
    public void testDeleteTestsForAssertion() throws Exception {
        final Long id = assertion.getId();
        // create and add some tests to assertion
        final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
        final org.ihtsdo.rvf.entity.Test test1 = getRandomTest();
        assertNotNull(test1.getId());
        tests.add(test1);
        final org.ihtsdo.rvf.entity.Test test2 = getRandomTest();
        assertNotNull(test2.getId());
        tests.add(test2);

        final String paramsString = objectMapper.writeValueAsString(tests);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8));
    }


    @After
    public void tearDown() throws Exception {
        assert assertionService != null;
        assertionService.delete(assertion);
        entityService.delete(releaseCenter);
    }

    private org.ihtsdo.rvf.entity.Test getRandomTest(){
        final org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
        test.setName("Random Test " + UUID.randomUUID());
        test.setCommand(new ExecutionCommand());
        return (org.ihtsdo.rvf.entity.Test) entityService.create(test);
    }
}