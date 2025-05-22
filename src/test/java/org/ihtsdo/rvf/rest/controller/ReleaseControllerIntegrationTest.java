package org.ihtsdo.rvf.rest.controller;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A test case for {@link AssertionController}.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
public class ReleaseControllerIntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private ReleaseDataManager releaseDataManager;

	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

	@BeforeEach
	public void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		assertNotNull(releaseDataManager);
		releaseDataManager.uploadPublishedReleaseData(getClass().getResourceAsStream("/SnomedCT_Release_INT_20140131.zip") ,
				"SnomedCT_Release_INT_20140131.zip", "int","20140131", Collections.emptyList());
		assertTrue(releaseDataManager.isKnownRelease("rvf_int_20140131"), "Schema name for release data 20140131 must be known to data manager ");
		assertTrue(releaseDataManager.getAllKnownReleases().contains("rvf_int_20140131"), "Release 20140131 must exist in all known releases ");
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
}