package org.ihtsdo.rvf.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;

import org.hamcrest.Matchers;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.service.EntityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
		test.setCommand(new ExecutionCommand());
		assert entityService != null;
		test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
		assert test != null;
		assert test.getId() != null;
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

		final Long id = test.getId();
		mockMvc.perform(get("/tests/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/tests/{id}",id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("id").value(id.intValue()));
	}

	@Test
	public void testDeleteTest() throws Exception {
		final Long id = test.getId();
//		mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	public void testDeleteMissingTest() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/tests/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("No entity found with given id " + id)));
	}

	@Test
	public void testCreateTest() throws Exception {

		final org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
		test.setName("Test Name");
		test.setDescription("Example Test Description");
		test.setCommand(new ExecutionCommand());
		final String paramsString = objectMapper.writeValueAsString(test);

		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/tests").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(post("/tests").content(paramsString).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("id").exists());
	}

	@Test
	public void testUpdateTest() throws Exception {
		final Long id = test.getId();
		final String updatedName = "Updated Test Name";
		test.setName(updatedName);
		final String paramsString = objectMapper.writeValueAsString(test);
		mockMvc.perform(put("/tests/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(put("/tests/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("name").value(updatedName));
	}

	@Test
	@Ignore
	public void executeTest() throws Exception{
		// set configuration
		final String template = "" +
				"select  " +
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

		final String execTestName = "Real - Concept has 1 defining relationship but is not primitive";
		final org.ihtsdo.rvf.entity.Test executableTest = new org.ihtsdo.rvf.entity.Test();
		executableTest.setName(execTestName);
		executableTest.setCommand(command);
		executableTest.setType(TestType.SQL);
		final org.ihtsdo.rvf.entity.Test returnedTest = objectMapper.readValue(mockMvc.perform(post("/tests").content(objectMapper.writeValueAsString(executableTest)).contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString(), org.ihtsdo.rvf.entity.Test.class);
		final Long executableTestId = returnedTest.getId();
		assert executableTestId != null;

		mockMvc.perform(get("/tests", executableTestId).contentType(MediaType.APPLICATION_JSON)).andDo(print());

		// execute test
		final String paramsString = objectMapper.writeValueAsString(returnedTest);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(get("/tests/{id}/run", executableTestId).content(paramsString).param("runId", "1")
				.param("prospectiveReleaseVersion", "rvf_int_20140731")
				.param("previousReleaseVersion", "postqa")
				.contentType(MediaType.APPLICATION_JSON)).andDo(print());
		// this test will fail unless we have a SNOMED CT database configured
		mockMvc.perform(get("/tests/{id}/run", executableTestId).content(paramsString).param("runId", "1")
				.param("prospectiveReleaseVersion", "rvf_int_20140731")
				.param("previousReleaseVersion", "postqa")
				.contentType(MediaType.APPLICATION_JSON))
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