package org.ihtsdo.rvf.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.ExecutionCommand;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
public class TestUploadFileControllerIntegrationTest {

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

	@BeforeEach
	public void setup() throws ServletException {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@org.junit.jupiter.api.Test
	public void testUploadTestPackage() throws Exception {
		final MvcResult result = mockMvc.perform(
				multipart("/test-file")
				.file( new MockMultipartFile("file", "SnomedCT_Release_INT_20140831.zip", "application/zip",
								getClass().getResourceAsStream("/SnomedCT_Release_INT_20140831.zip")))
				.requestAttr("writeSuccess", Boolean.FALSE))
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		final String content = result.getResponse().getContentAsString();
		assertTrue(content.length() > 0);
	}

	@org.junit.jupiter.api.Test
	public void testPostUploadTestPackage() throws Exception {
		final MvcResult result = mockMvc.perform(
				multipart("/test-post")
				.file(new MockMultipartFile("file", "ValidPostconditionAll.zip", "application/zip",
								getClass().getResourceAsStream("/ValidPostconditionAll.zip")))
		)
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertTrue(result.getResponse().getContentAsString().length() > 0);
	}

	@Test
	public void testUploadTestPackageExtendedMap() throws Exception {
		final MvcResult result = mockMvc.perform(
				multipart("/test-file")
				.file(new MockMultipartFile("file", "SnomedCT_Release_INT_20140831.zip", "application/zip",
								getClass().getResourceAsStream("/SnomedCT_test2_INT_20140131.zip")))
		)
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertTrue(result.getResponse().getContentAsString().length() > 0);
	}

	@org.junit.jupiter.api.Test
	public void testUploadTestDescription() throws Exception {
		final MvcResult result = mockMvc.perform(
				multipart("/test-pre")
				.file(new MockMultipartFile("file", "rel2_Description_Delta-en_INT_20240731.txt", "application/zip",
								getClass().getResourceAsStream("/rel2_Description_Delta-en_INT_20240731.txt")))
		)
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertTrue(result.getResponse().getContentAsString().length() > 0);
	}

	@Test
	public void testUploadPre() throws Exception {
		final MvcResult result = mockMvc.perform(
				multipart("/test-file")
				.file(new MockMultipartFile("file", "rel2_sRefset_SimpleMapDelta_INT_20140731.txt", "application/zip",
								getClass().getResourceAsStream("/rel2_sRefset_SimpleMapDelta_INT_20140731.txt")))
		)
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertTrue(result.getResponse().getContentAsString().length() > 0);
	}

	@org.junit.jupiter.api.Test
	@Disabled
	public void testRunPostTestPackage() throws Exception {

		final Assertion assertion = new Assertion();
		assertion.setAssertionText("Concept has 1 defining relationship but is not primitive");
		// save assertion
		String paramsString = objectMapper.writeValueAsString(assertion);
		System.out.println("paramsString = " + paramsString);
		// we have to strip the id property added by Jackson since this causes conflicts when Spring tries to convert content into Assertion
		paramsString = paramsString.replaceAll("\"id\":null,", "");
		System.out.println("paramsString after = " + paramsString);
		MvcResult returnedResponse = mockMvc.perform(post("/assertions").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andReturn();

		final Assertion assertion2 = objectMapper.readValue(returnedResponse.getResponse().getContentAsString(), Assertion.class);
		assertNotNull(assertion2, "Returned assertion must not be null");
		assertNotNull(assertion2.getAssertionId(), "Returned assertion must have an id");
		final Long assertionId = assertion2.getAssertionId();

		// set configuration
		final String template = "select  " +
				"concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.')  " +
				"from <PROSPECTIVE>.concept_<SNAPSHOT> a  " +
				"inner join <PROSPECTIVE>.stated_relationship_<SNAPSHOT> b on a.id = b.id " +
				"where a.active = '1' " +
				"and b.active = '1' " +
				"and a.definitionstatusid != '900000000000074008' " +
				"group by b.sourceid " +
				"having count(*) = 1;";
		final ExecutionCommand command = new ExecutionCommand();
		command.setTemplate(template);
		command.setStatements(Collections.emptyList());

		final String execTestName = "Real - Concept has 1 defining relationship but is not primitive";
		final org.ihtsdo.rvf.core.data.model.Test executableTest = new org.ihtsdo.rvf.core.data.model.Test();
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
		final String name = "Test Assertion Group";
		returnedResponse = mockMvc.perform(post("/groups").param("name", name).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("id").exists()).andReturn();
		final AssertionGroup group = objectMapper.readValue(returnedResponse.getResponse().getContentAsString(), AssertionGroup.class);
		assertNotNull(group, "Returned group must not be null");
		assertNotNull(group.getId(), "Returned group must have an id");

		// add assertions to group
		final List<String> assertionIds= Collections.singletonList(assertionId.toString());
		paramsString = objectMapper.writeValueAsString(assertionIds);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/groups/{id}/assertions", group.getId())
				.content(paramsString)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andDo(print());


		final String[] groups = new String[1];
		Collections.singletonList(group.getId().toString()).toArray(groups);
		mockMvc.perform(multipart("/run-post",
				new MockMultipartFile("file", "SnomedCT_test2_INT_20140131.zip", "application/zip",
						getClass().getResourceAsStream("/SnomedCT_test2_INT_20140131.zip")))
				.file(new MockMultipartFile("manifest", "manifest_20250731.xml", "application/xml",
						getClass().getResourceAsStream("/manifest_20250731.xml")))
				.param("groups", groups)
				.param("prospectiveReleaseVersion", "20140731")
				.param("previousReleaseVersion", "20140731")
				.param("writeSuccesses", "false")
				.param("runId", "1"))
				.andDo(print());
	}
}
