package org.ihtsdo.rvf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.helper.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
public class TestUploadFileControllerIntegrationTest {

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    @Before
    public void setup() throws ServletException {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testUploadTestPackage() throws Exception {
        MvcResult result = mockMvc.perform(
                fileUpload("/test-file")
                        .file(new MockMultipartFile("file", "SnomedCT_Release_INT_20140831.zip", "application/zip",
                                getClass().getResourceAsStream("/SnomedCT_Release_INT_20140831.zip")))
                .requestAttr("writeSuccess", Boolean.FALSE))
                .andDo(print())
                .andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        assertTrue(content.length() > 0);
    }

    @Test
    public void testPostUploadTestPackage() throws Exception {
        MvcResult result = mockMvc.perform(
                fileUpload("/test-post")
                        .file(new MockMultipartFile("file", "ValidPostconditionAll.zip", "application/zip",
                                getClass().getResourceAsStream("/ValidPostconditionAll.zip")))
        )
                .andDo(print())
                .andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().length() > 0);
    }

    @Test
    public void testUploadTestPackageExtendedMap() throws Exception {
        MvcResult result = mockMvc.perform(
                fileUpload("/test-file")
                        .file(new MockMultipartFile("file", "SnomedCT_test2_INT_20140131.zip", "application/zip",
                                getClass().getResourceAsStream("/SnomedCT_test2_INT_20140131.zip")))
        )
                .andDo(print())
                .andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().length() > 0);
    }

    @Test
    public void testUploadTestDescription() throws Exception {
        MvcResult result = mockMvc.perform(
                fileUpload("/test-pre")
                        .file(new MockMultipartFile("file", "rel2_Description_Delta-en_INT_20240731.txt", "application/zip",
                                getClass().getResourceAsStream("/rel2_Description_Delta-en_INT_20240731.txt")))
        )
                .andDo(print())
                .andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().length() > 0);
    }

    @Test
    public void testUploadPre() throws Exception {
        MvcResult result = mockMvc.perform(
                fileUpload("/test-file")
                        .file(new MockMultipartFile("file", "rel2_sRefset_SimpleMapDelta_INT_20140731.txt", "application/zip",
                                getClass().getResourceAsStream("/rel2_sRefset_SimpleMapDelta_INT_20140731.txt")))
        )
                .andDo(print())
                .andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().length() > 0);
    }

    @Test
    public void testRunPostTestPackage() throws Exception {

        Assertion assertion = new Assertion();
        assertion.setName("Concept has 1 defining relationship but is not primitive");
        assertion.setDescription("Concept has 1 defining relationship but is not primitive");
        // save assertion
        String paramsString = objectMapper.writeValueAsString(assertion);
        System.out.println("paramsString = " + paramsString);
        // we have to strip the id property added by Jackson since this causes conflicts when Spring tries to convert content into Assertion
        paramsString = paramsString.replaceAll("\"id\":null,", "");
        System.out.println("paramsString after = " + paramsString);
        MvcResult returnedResponse = mockMvc.perform(post("/assertions").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andReturn();

        Assertion assertion2 = objectMapper.readValue(returnedResponse.getResponse().getContentAsString(), Assertion.class);
        assertNotNull("Returned assertion must not be null", assertion2);
        assertNotNull("Returned assertion must have an id", assertion2.getId());
        Long assertionId = assertion2.getId();

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
        command.setStatements(Collections.<String>emptyList());

        String execTestName = "Real - Concept has 1 defining relationship but is not primitive";
        org.ihtsdo.rvf.entity.Test executableTest = new org.ihtsdo.rvf.entity.Test();
        executableTest.setName(execTestName);
        executableTest.setCommand(command);
        executableTest.setType(TestType.SQL);

        paramsString = objectMapper.writeValueAsString(Collections.singletonList(executableTest));
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/assertions/{id}/tests", assertionId)
                .content(paramsString)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        // create assertion group
        String name = "Test Assertion Group";
        returnedResponse = mockMvc.perform(post("/groups").param("name", name).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").exists()).andReturn();
        AssertionGroup group = objectMapper.readValue(returnedResponse.getResponse().getContentAsString(), AssertionGroup.class);
        assertNotNull("Returned group must not be null", group);
        assertNotNull("Returned group must have an id", group.getId());

        // add assertions to group
        List<Assertion> assertions = Collections.singletonList(assertion2);
        paramsString = objectMapper.writeValueAsString(assertions);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/groups/{id}/assertions", group.getId())
                .content(paramsString)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(print());


        List<AssertionGroup> groups = Collections.singletonList(group);
        mockMvc.perform(fileUpload("/run-post")
                .file(new MockMultipartFile("file", "SnomedCT_test2_INT_20140131.zip", "application/zip",
                        getClass().getResourceAsStream("/SnomedCT_test2_INT_20140131.zip")))
                .file(new MockMultipartFile("manifest", "manifest_20250731.xml", "application/xml",
                        getClass().getResourceAsStream("/manifest_20250731.xml")))
                .param("groups", objectMapper.writeValueAsString(groups))
                .param("prospectiveReleaseVersion", "20140731")
                .param("previousReleaseVersion", "20140731")
                .param("writeSuccesses", "false")
                .param("runId", "1"))
                .andDo(print());

    }
}
