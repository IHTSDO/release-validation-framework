package org.ihtsdo.rvf.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;

import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
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
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test case for {@link AssertionController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ApiTestConfig.class)
public class ReleaseControllerIntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private ReleaseDataManager releaseDataManager;

	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	@Before
	public void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		assertNotNull(releaseDataManager);
		releaseDataManager.uploadPublishedReleaseData(getClass().getResourceAsStream("/SnomedCT_Release_INT_20140131.zip") ,
				"SnomedCT_Release_INT_20140131.zip", "int","20140131");
		assertTrue("Schema name for release data 20140131 must be known to data manager ", releaseDataManager.isKnownRelease("rvf_int_20140131"));
		assertTrue("Release 20140131 must exist in all known releases ", releaseDataManager.getAllKnownReleases().contains("rvf_int_20140131"));
	}

	@Test
	public void testGetReleases() throws Exception {
		mockMvc.perform(get("/releases").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8)).andDo(print());
	}

	@Test
	public void testGetRelease() throws Exception {
		mockMvc.perform(get("/releases/{version}", "rvf_int_20140131").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8));
	}

	@Test
	public void testGetMissingRelease() throws Exception {
		mockMvc.perform(get("/releases/{version}", "19000131").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

//	@Test
//	public void testUploadRelease() throws Exception {
//
//		mockMvc.perform(
//				fileUpload("/releases/{version}", "20140131")
//						.file(new MockMultipartFile("file", "SnomedCT_Release_INT_20140131.zip", "application/zip",
//								getClass().getResourceAsStream("/SnomedCT_Release_INT_20140131.zip")))
//				.param("overWriteExisting", "false")
//				.param("purgeExistingDatabase", "false")
//				)
//				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
//				.andExpect(status().isOk())
//				.andExpect(content().string(containsString("true")))
//				.andDo(print());
//	}
}