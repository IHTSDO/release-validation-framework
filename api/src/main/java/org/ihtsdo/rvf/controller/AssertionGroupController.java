package org.ihtsdo.rvf.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
import org.ihtsdo.rvf.helper.AssertionHelper;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/groups")
public class AssertionGroupController {

	@Autowired
	private AssertionService assertionService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	@Autowired
	private AssertionHelper assertionHelper;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Logger logger = LoggerFactory.getLogger(AssertionGroupController.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<AssertionGroup> getGroups() {
		List<AssertionGroup> result = assertionService.getAllAssertionGroups();
		if (result == null ) {
			result = new ArrayList<>();
		}
		return result;
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<Assertion> getAssertionsForGroup(@PathVariable final Long id) {

		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		return assertionService.getAssertionsForGroup(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.POST)
	@ResponseBody
	public AssertionGroup addAssertionsToGroup(@PathVariable final Long id, @RequestBody(required = false) final List<String> assertionsList, final HttpServletResponse response) {

		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		
		//Do we have anything to add?
		if (assertionsList == null || assertionsList.size() == 0) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			final List<Assertion> assertions = getAssertions(assertionsList);
			for(final Assertion assertion : assertions){
				assertionService.addAssertionToGroup(assertion, group);
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}

		return group;
	}
	
	@RequestMapping(value = "{id}/addAllAssertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup addAllAssertions(@PathVariable final Long id) {
		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		final List<Assertion> assertionList = entityService.findAll(Assertion.class);
		final Set<Assertion> assertionSet = new HashSet<>(assertionList);
		group.setAssertions(assertionSet);
		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup removeAssertionsFromGroup(@PathVariable final Long id, @RequestBody(required = false) final List<Assertion> assertions) {

		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		for(final Assertion assertion : assertions){
			assertionService.removeAssertionFromGroup(assertion, group);
		}

		return group;
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup setAsAssertionsInGroup(@PathVariable final Long id, @RequestBody(required = false) final Set<Assertion> assertions) {

		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		// replace all existing assertions with current list
		group.setAssertions(assertions);

		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup getAssertionGroup(@PathVariable final Long id) {
		return (AssertionGroup) entityService.find(AssertionGroup.class, id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup deleteAssertionGroup(@PathVariable final Long id) {
		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		entityService.delete(group);
		return group;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public AssertionGroup createAssertionGroupWithName(@RequestParam final String name) {
		final AssertionGroup group = new AssertionGroup();
		group.setName(name);
		return (AssertionGroup) entityService.create(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup updateAssertionGroup(@PathVariable final Long id,
			@RequestParam final String name) {
		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		group.setName(name);
		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> executeAssertions(@PathVariable final Long id,
										   @RequestParam final Long runId, @RequestParam final String prospectiveReleaseVersion,
										   @RequestParam final String previousReleaseVersion) {

		final AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		final ExecutionConfig config = new ExecutionConfig(runId);
		config.setPreviousVersion(previousReleaseVersion);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		return assertionHelper.assertAssertions(assertionService.getAssertionsForGroup(group), config);
	}

	private List<Assertion> getAssertions(final List<String> items){

		final List<Assertion> assertions = new ArrayList<>();
		for(final String item: items){
			try
			{
				if(item.matches("\\d+")){
					// treat as assertion id and retrieve associated assertion
					final Assertion assertion = assertionService.find(Long.valueOf(item));
					if(assertion != null){
						assertions.add(assertion);
					}
				}
				else{
					assertions.add(objectMapper.readValue(item, Assertion.class));
				}
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return assertions;
	}
}
