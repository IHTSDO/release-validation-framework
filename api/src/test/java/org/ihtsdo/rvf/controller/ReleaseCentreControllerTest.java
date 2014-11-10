package org.ihtsdo.rvf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.entity.ReleaseCenter;
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
 * A releaseCentre case for {@link org.ihtsdo.rvf.controller.ReleaseCentreController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class ReleaseCentreControllerTest {

    @Autowired
    private WebApplicationContext ctx;
    private MockMvc mockMvc;
    @Autowired
    private EntityService entityService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    private ReleaseCenter releaseCentre;

    @Before
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        releaseCentre = new ReleaseCenter();
        releaseCentre.setName("Release Centre for International Release");
        releaseCentre.setShortName("IHTSDO");

        assert entityService != null;
        assert entityService.count(org.ihtsdo.rvf.entity.Test.class) == 0;
        releaseCentre = (ReleaseCenter) entityService.create(releaseCentre);
        assert releaseCentre != null;
        assert releaseCentre.getId() != null;
        assert entityService.count(ReleaseCenter.class) > 0;
    }

    @Test
    public void testGetTests() throws Exception {
        mockMvc.perform(get("/releasecentres").accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/releasecentres").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString(releaseCentre.getName())));
    }

    @Test
    public void testGetTest() throws Exception {

        Long id = releaseCentre.getId();
        mockMvc.perform(get("/releasecentres/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(get("/releasecentres/{id}",id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").value(id.intValue()));
    }

    @Test
    public void testDeleteTest() throws Exception {
        Long id = releaseCentre.getId();
//        mockMvc.perform(delete("/releasecentres/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/releasecentres/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteMissingTest() throws Exception {
        Long id = 29367234L;
//        mockMvc.perform(delete("/releasecentres/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(delete("/releasecentres/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No entity found with given id " + id)));
    }

    @Test
    public void testCreateTest() throws Exception {

        String paramsString = objectMapper.writeValueAsString(releaseCentre);
        System.out.println("paramsString = " + paramsString);
        mockMvc.perform(post("/releasecentres").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(post("/releasecentres").content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id").exists());
    }

    @Test
    public void testUpdateTest() throws Exception {
        Long id = releaseCentre.getId();
        String updatedName = "Updated Release Centre Name";
        releaseCentre.setName(updatedName);
        String paramsString = objectMapper.writeValueAsString(releaseCentre);
        mockMvc.perform(put("/releasecentres/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
        mockMvc.perform(put("/releasecentres/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(updatedName));
    }


    @After
    public void tearDown() throws Exception {
        assert entityService != null;
        if (releaseCentre != null) {
            entityService.delete(releaseCentre);
        }
    }
}