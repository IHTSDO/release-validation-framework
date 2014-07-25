package org.ihtsdo.rvf.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
public class TestUploadFileControllerTest {

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

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
}