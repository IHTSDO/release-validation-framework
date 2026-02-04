package org.ihtsdo.rvf.rest.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestModel;
import org.ihtsdo.rvf.core.data.model.ExecutionCommand;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A test case for {@link AssertionController}.
 */
@Transactional
class AssertionControllerIntegrationTest extends IntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private AssertionService assertionService;

	private Assertion assertion;

	@BeforeEach
	public void setUp() {
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
		mockMvc.perform(get("/assertions").accept(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions").accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string(containsString(assertion.getAssertionText())));
	}

	@Test
	public void testGetAssertion() throws Exception {

		final Long id = assertion.getAssertionId();
		mockMvc.perform(get("/assertions/{id}",id).accept(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions/{id}",id).accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("assertionId").value(id.intValue()));
	}

	@Test
	public void testDeleteAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		mockMvc.perform(delete("/assertions/delete/{id}", id).contentType(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/assertions/{id}", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	public void testDeleteMissingAssertion() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(delete("/assertions/{id}", id).contentType(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(delete("/assertions/{id}", id).contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testGetMissingAssertion() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(get("/assertions/{id}", id).contentType(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions/{id}", id).contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testCreateAssertion() throws Exception {

		 Assertion newAssertion = new Assertion();
		 String assertionName ="Testing create assertion";
		 newAssertion .setAssertionText(assertionName);
		 newAssertion.setKeywords("test");
		 newAssertion.setUuid(UUID.randomUUID());
		String paramsString = OBJECT_MAPPER.writeValueAsString(newAssertion);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/assertions").content(paramsString).contentType(APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("assertionText").value(assertionName));
	}

	@Test
	public void testUpdateAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		final String updatedName = "Updated Assertion Name";
		assertion.setAssertionText(updatedName);
		final String paramsString = OBJECT_MAPPER.writeValueAsString(assertion);
		mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(put("/assertions/{id}", id).content(paramsString).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("assertionText").value(updatedName));
	}

	@Test
	public void testGetTestsForAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		// create and add some tests to assertion
		assertionService.addTest(assertion, getRandomTest());
		assertionService.addTest(assertion, getRandomTest());

		mockMvc.perform(get("/assertions/{id}/tests", id).contentType(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/assertions/{id}/tests", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string(containsString("Random Test")));
	}

	@Test
	public void testAddTestsForAssertion() throws Exception {
		final Long id = assertion.getAssertionId();
		// create and add some tests to assertion
		final List<org.ihtsdo.rvf.core.data.model.Test> tests = new ArrayList<>();
		final org.ihtsdo.rvf.core.data.model.Test test1 = getRandomTest();
		tests.add(test1);
		final org.ihtsdo.rvf.core.data.model.Test test2 = getRandomTest();
		tests.add(test2);

		final String paramsString = OBJECT_MAPPER.writeValueAsString(tests);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/assertions/{id}/tests", id).content(paramsString).contentType(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON));
		// getting tests for assertion should now contain response with text Random Test
		mockMvc.perform(get("/assertions/{id}/tests", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string(containsString("Random Test")));
	}

	@Test
	public void testDeleteTestsForAssertion() throws Exception {
		// create sample data
		Assertion assertion = createAssertion("Assertion 1");
		assertNotNull(assertion);

		TestModel testModel = createTestModel(assertion);
		assertNotNull(testModel);

		// assert test before deletion
		ResultActions resultActions = mockMvc.perform(get("/assertions/{id}/tests", assertion.getAssertionId()).contentType(APPLICATION_JSON));
		List<TestModel> testModels = getResponseBody(resultActions, new TypeReference<>() {
		});
		assertFalse(testModels.isEmpty());

		// delete test
		mockMvc.perform(delete("/assertions/{id}/tests", assertion.getAssertionId()).param("testIds", testModel.getId().toString()).contentType(APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON));

		// assert test after deletion
		resultActions = mockMvc.perform(get("/assertions/{id}/tests", assertion.getAssertionId()).contentType(APPLICATION_JSON));
		testModels = getResponseBody(resultActions, new TypeReference<>() {
		});
		assertTrue(testModels.isEmpty());
	}

	@AfterEach
	public void tearDown() {
		assert assertionService != null;
		assertionService.delete(assertion);
	}

	private org.ihtsdo.rvf.core.data.model.Test getRandomTest(){
		final org.ihtsdo.rvf.core.data.model.Test test = new org.ihtsdo.rvf.core.data.model.Test();
		test.setName("Random Test " + UUID.randomUUID());
		test.setCommand(new ExecutionCommand());
		return test;
	}

	private TestModel getRandomAssertionTest() {
		org.ihtsdo.rvf.core.data.model.Test randomTest = getRandomTest();
		return new TestModel(randomTest);
	}

	private Assertion createAssertion(String assertionName) {
		try {
			Assertion assertion = new Assertion();
			assertion.setAssertionText(assertionName);
			ResultActions resultActions = mockMvc.perform(post("/assertions")
					.content(OBJECT_MAPPER.writeValueAsString(assertion))
					.contentType(APPLICATION_JSON));

			return getResponseBody(resultActions, Assertion.class);
		} catch (Exception e) {
			return null;
		}
	}

	private TestModel createTestModel(Assertion assertion) {
		try {
			// create
			org.ihtsdo.rvf.core.data.model.Test randomTest = getRandomTest();
			ResultActions resultActions = mockMvc.perform(post("/assertions/{id}/tests", assertion.getAssertionId())
					.content(OBJECT_MAPPER.writeValueAsString(List.of(randomTest)))
					.contentType(APPLICATION_JSON)
			);

			assertion = getResponseBody(resultActions, Assertion.class);
			assertNotNull(assertion);

			// retrieve (to get id)
			resultActions = mockMvc.perform(get("/assertions/{id}/tests", assertion.getAssertionId()));
			List<TestModel> tests = getResponseBody(resultActions, new TypeReference<>() {
			});
			if (tests == null || tests.isEmpty()) {
				return null;
			}

			return tests.iterator().next();
		} catch (Exception e) {
			return null;
		}
	}
}