package org.ihtsdo.rvf.rest.controller;

import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A test case for {@link AssertionController}.
 */
class ReleaseControllerIntegrationTest extends IntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private ReleaseDataManager releaseDataManager;

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
		mockMvc.perform(get("/releases").accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON)).andDo(print());
	}

	@Test
	public void testGetRelease() throws Exception {
		mockMvc.perform(get("/releases/{version}", "rvf_int_20140131").accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON));
	}

	@Test
	public void testGetMissingRelease() throws Exception {
		mockMvc.perform(get("/releases/{version}", "19000131").contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}
}