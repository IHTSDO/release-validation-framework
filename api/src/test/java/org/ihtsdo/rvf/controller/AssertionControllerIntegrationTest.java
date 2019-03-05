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
import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.repository.TestRepository;
import org.ihtsdo.rvf.service.AssertionService;
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
 * A test case for {@link org.ihtsdo.rvf.controller.AssertionController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@Transactional
@ContextConfiguration(classes = ApiTestConfig.class)
public class AssertionControllerIntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private TestRepository testRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
	private Assertion assertion;

	@Before
	public void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		Assertion newAssertion = new Assertion();
		newAssertion .setAssertionText("Assertion Name");
		newAssertion.setKeywords("test");
		newAssertion.setUuid(UUID.randomUUID());
		assert assertionService != null;
		assertion = assertionService.create(newAssertion);
		assert assertion != null;
		assert assertion.getAssertionId() != null;
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

		final Long id = assertion.getAssertionId();
		mockMvc.perform(get("/assertions/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions/{id}",id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("assertionId").value(id.intValue()));
	}

	@Test
	public void testDeleteAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		mockMvc.perform(delete("/assertions/delete/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	public void testDeleteMissingAssertion() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testGetMissingAssertion() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(get("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testCreateAssertion() throws Exception {

		 Assertion newAssertion = new Assertion();
		 String assertionName ="Testing create assertion";
		 newAssertion .setAssertionText(assertionName);
		 newAssertion.setKeywords("test");
		 newAssertion.setUuid(UUID.randomUUID());
		String paramsString = objectMapper.writeValueAsString(newAssertion);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/assertions").content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("assertionText").value(assertionName));
	}

	@Test
	public void testUpdateAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		final String updatedName = "Updated Assertion Name";
		assertion.setAssertionText(updatedName);
		final String paramsString = objectMapper.writeValueAsString(assertion);
		mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("assertionText").value(updatedName));
	}

	@Test
	public void testGetTestsForAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
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
	@Ignore
	public void testAddTestsForAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
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
		mockMvc.perform(post("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print())
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
		final Long id = assertion.getAssertionId();
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
		mockMvc.perform(delete("/assertions/{id}/tests", id).param("testIds", test1.getId().toString()).contentType(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8));
	}


	@After
	public void tearDown() throws Exception {
		assert assertionService != null;
		assertionService.delete(assertion);
	}

	private org.ihtsdo.rvf.entity.Test getRandomTest(){
		final org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
		test.setName("Random Test " + UUID.randomUUID());
		test.setCommand(new ExecutionCommand());
		return testRepository.save(test);
	}
}