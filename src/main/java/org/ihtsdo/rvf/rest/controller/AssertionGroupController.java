package org.ihtsdo.rvf.rest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.repository.AssertionGroupRepository;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.rest.helper.AssertionHelper;
import org.ihtsdo.rvf.rest.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/groups")
@Tag(name = "Assertions Groups")
public class AssertionGroupController {

	@Autowired
	private AssertionService assertionService;

	@Autowired
	private AssertionGroupRepository assertionGroupRepository;

	@Autowired
	private AssertionHelper assertionHelper;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Logger logger = LoggerFactory.getLogger(AssertionGroupController.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all assertion groups", description = "Retrieves all assertion groups defined in the system.")
	public List<AssertionGroup> getGroups() {
		List<AssertionGroup> result = assertionService.getAllAssertionGroups();
		if (result == null) {
			result = new ArrayList<>();
		}
		return result;
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all assertions for a given assertion group", description = "Retrieves all assertions for a given assertion group identified by the group id.")
	public List<Assertion> getAssertionsForGroup(@Parameter(description = "Assertion group id") @PathVariable final Long id) {
		AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		return new ArrayList<>(group.getAssertions());
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.POST)
	@ResponseBody
	@Operation(summary = "Add assertions to a group", description = "Adds assertions to the assertion group identified by the group id.")
	public AssertionGroup addAssertionsToGroup(@PathVariable final Long id,
			@RequestBody(required = false) final List<String> assertionsList, final HttpServletResponse response) throws JsonProcessingException {

		final AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		// Do we have anything to add?
		if (assertionsList == null || assertionsList.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			final List<Assertion> assertions = getAssertions(assertionsList);
			for (final Assertion assertion : assertions) {
				assertionService.addAssertionToGroup(assertion, group);
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
		return group;
	}

	@RequestMapping(value = "{id}/addAllAssertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Add all assertions available in the system to a group", description = "Adds all assertions available in the system to a group."
			+ " This api may only be used when user desires to add all assertion found in the system to an assertion group"
			+ " otherwise use {id}/assertions api as post call.")
	public AssertionGroup addAllAssertions(@PathVariable final Long id) {
		AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		List<Assertion> assertionList = assertionService.findAll();
		group.setAssertions(new HashSet<>(assertionList));
		return assertionGroupRepository.save(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Delete assertions from a group", description = "Removes supplied assertions from a given assertion group")
	public AssertionGroup removeAssertionsFromGroup(
			@PathVariable final Long id,
			@Parameter(description = "Only assertion id is required") @RequestBody final List<Assertion> assertions) {
		AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		for (final Assertion assertion : group.getAssertions()) {
			group = assertionService.removeAssertionFromGroup(assertion, group);
		}
		return group;
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Add supplied assertions to an assertion group", description = "Replaces existing assertions of an assertion group with supplied assertions")
	public AssertionGroup setAsAssertionsInGroup(@PathVariable final Long id,
			@RequestBody(required = false) final Set<Assertion> assertions) {

		final AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		// replace all existing assertions with current list
		group.setAssertions(assertions);
		return assertionGroupRepository.save(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get an assertion group", description = "Retrieves an assertion group for a given id")
	public AssertionGroup getAssertionGroup(@PathVariable final Long id) {
		return assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Delete an assertion group", description = "Deletes an assertion group from the system")
	public AssertionGroup deleteAssertionGroup(@Parameter(description = "Assertion group id") @PathVariable final Long id) {
		final AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		group.removeAllAssertionsFromGroup();
		assertionGroupRepository.delete(group);
		return group;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an assertion group with specified name", description = "Creates an assertion group with specified name")
	public AssertionGroup createAssertionGroupWithName(@RequestParam final String name) {
		final AssertionGroup group = new AssertionGroup();
		group.setName(name);
		return assertionGroupRepository.save(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Update an assertion group", description = "Updates the group name for the assertion group identified by the group id.")
	public AssertionGroup updateAssertionGroup(@PathVariable final Long id,
			@Parameter(description = "Assertion group name") @RequestParam final String name) {
		AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		group.setName(name);
		return assertionGroupRepository.save(group);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Execute all tests for a given assertion group", description = "Executes all tests for the assertion group identified by the group id.")
	public Map<String, Object> executeAssertions(
			@Parameter(description = "Assertion group id") @PathVariable final Long id,
			@Parameter(description = "Unique number") @RequestParam final Long runId,
			@Parameter(description = "Prospective version") @RequestParam final String prospectiveReleaseVersion,
			@Parameter(description = "Previous release version", required = false) @RequestParam final String previousReleaseVersion) {

		AssertionGroup group = assertionGroupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
		MysqlExecutionConfig config = new MysqlExecutionConfig(runId);
		config.setPreviousVersion(previousReleaseVersion);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setFailureExportMax(-1);
		return assertionHelper.assertAssertions(group.getAssertions(), config);
	}

	private List<Assertion> getAssertions(final List<String> items) throws JsonProcessingException {
		final List<Assertion> assertions = new ArrayList<>();
		for (final String item : items) {
			if (item.matches("\\d+")) {
				// treat as assertion id and retrieve associated assertion
				final Assertion assertion = assertionService.find(Long.valueOf(item));
				if (assertion != null) {
					assertions.add(assertion);
				}
			} else {
				assertions.add(objectMapper.readValue(item, Assertion.class));
			}
		}
		return assertions;
	}
}
