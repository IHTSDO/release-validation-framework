package org.ihtsdo.rvf.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.repository.AssertionGroupRepository;
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
 * A test case for {@link org.ihtsdo.rvf.controller.AssertionGroupController}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@Transactional
@ContextConfiguration(classes = ApiTestConfig.class)
public class AssertionGroupControllerIntegrationTest {

	@Autowired
	private WebApplicationContext ctx;
	private MockMvc mockMvc;
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private AssertionGroupRepository assertionGroupRepo;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
	private AssertionGroup group;

	@Before
	public void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
		group = new AssertionGroup();
		group.setName("Test group");

		assertNotNull(assertionService);
		assertNotNull(assertionGroupRepo);
		group = (AssertionGroup) assertionGroupRepo.save(group);
		assertNotNull(group.getId());
	}

	@Test
	public void testGetGroups() throws Exception {
		mockMvc.perform(get("/groups").accept(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/groups").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(content().string(containsString(group.getName()))).andDo(print());
	}

	@Test
	public void testGetGroup() throws Exception {

		final Long id = group.getId();
		mockMvc.perform(get("/groups/{id}",id).accept(MediaType.APPLICATION_JSON)).andDo(print());
		mockMvc.perform(get("/groups/{id}",id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("id").value(id.intValue()));
	}

	@Test
	@Ignore
	public void testDeleteGroup() throws Exception {
		final Long id = group.getId();
		mockMvc.perform(delete("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print());
	}

	@Test
	public void testDeleteMissingGroup() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(delete("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("Unable to find org.ihtsdo.rvf.entity.AssertionGroup with id " + id))).andDo(print());
	}

	@Test
	public void testGetMissingGroup() throws Exception {
		final Long id = 29367234L;
		mockMvc.perform(get("/groups/{id}", id).contentType(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("No entity found with given id " + id))).andDo(print());
	}

	@Test
	public void testCreateGroup() throws Exception {

		final String name = "New Test Assertion Group";
		mockMvc.perform(post("/groups").param("name", name).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("id").exists()).andDo(print());
	}

	@Test
	public void testUpdateGroup() throws Exception {
		final Long id = group.getId();
		final String updatedName = "Updated Assertion Group Name";
		group.setName(updatedName);
		final String paramsString = objectMapper.writeValueAsString(group);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(put("/groups/{id}", id).param("name", updatedName).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("name").value(updatedName)).andDo(print());
	}

	@Test
	public void testGetAssertionsForGroup() throws Exception {
		final Long id = group.getId();
		// create and add some assertions
		assertionService.addAssertionToGroup(getRandomAssertion(), group);
		assertionService.addAssertionToGroup(getRandomAssertion(), group);

		mockMvc.perform(get("/groups/{id}/assertions", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
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
		mockMvc.perform(post("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8)).andDo(print());

		// getting tests for assertion should now contain response with text Random Test
		mockMvc.perform(get("/groups/{id}/assertions", id).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].assertionText", is("Test assertion")));
	}

	@Test
	public void testDeleteTestsForAssertion() throws Exception {
		final Long id = group.getId();
		// create and add some assertions
		final List<Assertion> assertions = new ArrayList<>();
		assertions.add(getRandomAssertion());
		assertions.add(getRandomAssertion());

		final String paramsString = objectMapper.writeValueAsString(assertions);
		System.out.println("paramsString = " + paramsString);
		mockMvc.perform(delete("/groups/{id}/assertions", id).content(paramsString).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8)).andDo(print());
	}


	@After
	public void tearDown() throws Exception {
		assert assertionGroupRepo != null;
		if (group != null) {
			assertionGroupRepo.delete(group);
		}
	}

	private Assertion getRandomAssertion(){
		final org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
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