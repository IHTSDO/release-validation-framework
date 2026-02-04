package org.ihtsdo.rvf.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.ExecutionCommand;
import org.ihtsdo.rvf.core.data.repository.AssertionGroupRepository;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A test case for {@link AssertionGroupController}.
 */
@Transactional
class AssertionGroupControllerIntegrationTest extends IntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private AssertionGroupRepository assertionGroupRepo;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			APPLICATION_JSON.getType(),
			APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);
	private AssertionGroup group;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		group = new AssertionGroup();
		group.setName("Test group");

		assertNotNull(assertionService);
		assertNotNull(assertionGroupRepo);
		group = assertionGroupRepo.save(group);
		assertNotNull(group.getId());
	}

	@Test
	public void testGetGroups() throws Exception {
		mockMvc.perform(get("/groups").accept(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/groups").accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string(containsString(group.getName()))).andDo(print());
	}

	@org.junit.jupiter.api.Test
	public void testGetGroup() throws Exception {

		final Long id = group.getId();
		mockMvc.perform(get("/groups/{id}",id).accept(APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/groups/{id}",id).accept(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("id").value(id.intValue()));
	}

	@Test
	public void testDeleteGroup() throws Exception {
		final Long id = group.getId();
		mockMvc.perform(delete("/groups/{id}", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print());
	}

	@org.junit.jupiter.api.Test
	public void testDeleteMissingGroup() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(delete("/groups/{id}", id).contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("No entity found with given id " + id))).andDo(print());
	}

	@Test
	public void testGetMissingGroup() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(get("/groups/{id}", id).contentType(APPLICATION_JSON)).andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("No entity found with given id " + id))).andDo(print());
	}

	@Test
	public void testCreateGroup() throws Exception {

		final String name = "New Test Assertion Group";
		mockMvc.perform(post("/groups").param("name", name).contentType(APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("id").exists()).andDo(print());
	}

	@org.junit.jupiter.api.Test
	public void testUpdateGroup() throws Exception {
		final Long id = group.getId();
		final String updatedName = "Updated Assertion Group Name";
		group.setName(updatedName);
		final String paramsString = objectMapper.writeValueAsString(group);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(put("/groups/{id}", id).param("name", updatedName).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("name").value(updatedName)).andDo(print());
	}

	@org.junit.jupiter.api.Test
	public void testGetAssertionsForGroup() throws Exception {
		final Long id = group.getId();
		// create and add some assertions
		assertionService.addAssertionToGroup(getRandomAssertion(), group);
		assertionService.addAssertionToGroup(getRandomAssertion(), group);

		mockMvc.perform(get("/groups/{id}/assertions", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].assertionText", is("Test assertion"))).andDo(print());
	}

	@Test
	public void testAddAssertionsToGroup() throws Exception {
		final Long id = group.getId();
		// create and add some assertions
		final List<Long> assertions = new ArrayList<>();
		assertions.add(getRandomAssertion().getAssertionId());
		assertions.add(getRandomAssertion().getAssertionId());

		final String paramsString = objectMapper.writeValueAsString(assertions);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(post("/groups/{id}/assertions", id).content(paramsString).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON)).andDo(print());

		// getting tests for assertion should now contain response with text Random Test
		mockMvc.perform(get("/groups/{id}/assertions", id).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].assertionText", is("Test assertion")));
	}

	@org.junit.jupiter.api.Test
	public void testDeleteTestsForAssertion() throws Exception {
		final Long id = group.getId();
		// create and add some assertions
		final List<Assertion> assertions = new ArrayList<>();
		assertions.add(getRandomAssertion());
		assertions.add(getRandomAssertion());

		final String paramsString = objectMapper.writeValueAsString(assertions);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(delete("/groups/{id}/assertions", id).content(paramsString).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON)).andDo(print());
	}


	@AfterEach
	public void tearDown() {
		assert assertionGroupRepo != null;
		if (group != null) {
			assertionGroupRepo.delete(group);
		}
	}

	private Assertion getRandomAssertion(){
		final org.ihtsdo.rvf.core.data.model.Test test = new org.ihtsdo.rvf.core.data.model.Test();
		test.setName("Random Test " + UUID.randomUUID());
		test.setCommand(new ExecutionCommand());

		Assertion assertion = new Assertion();
		assertion.setAssertionText("Test assertion");
		// save assertion
		assertion = assertionService.addTest(assertion, test);
		assertNotNull(assertion.getAssertionId());

		return assertion;
	}
}