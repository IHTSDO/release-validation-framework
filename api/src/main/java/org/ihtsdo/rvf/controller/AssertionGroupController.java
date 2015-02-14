package org.ihtsdo.rvf.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
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
import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@RequestMapping("/groups")
@Api(value = "Assertions Groups")
public class AssertionGroupController {

	@Autowired
	private AssertionService assertionService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	@Autowired
	private AssertionHelper assertionHelper;
	private ObjectMapper objectMapper = new ObjectMapper();
	private final Logger logger = LoggerFactory.getLogger(AssertionGroupController.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get all assertion groups",
		notes = "Retrieves all assertion groups defined in the system. "
				+ "Assertion group is used to define group of similar type of assertions" )
	public List<AssertionGroup> getGroups() {
		return entityService.findAll(AssertionGroup.class);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get all assertions of a group",
		notes = "Retrieves all assertions from a specific assertion group" )
	public List<Assertion> getAssertionsForGroup(@PathVariable Long id) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		return assertionService.getAssertionsForGroup(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Add assertions to a group",
		notes = "Add assertions to an assertion group identified by given id" )
	public AssertionGroup addAssertionsToGroup(@PathVariable Long id, @RequestBody(required = false) List<String> assertionsList) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		List<Assertion> assertions = getAssertions(assertionsList);
		for(Assertion assertion : assertions){
			assertionService.addAssertionToGroup(assertion, group);
		}

		return group;
	}
	
	@RequestMapping(value = "{id}/addAllAssertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Add all assertions available in the system to a group",
		notes = "Add all assertions available in the system to a group."
				+ " This api may only be used when user desires to add all assertion found in the system to an assertion group"
				+ " otherwise use {id}/assertions api as post call." )
	public AssertionGroup addAllAssertions(@PathVariable Long id) {
		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		List<Assertion> assertionList = (List<Assertion>)entityService.findAll(Assertion.class);
		Set<Assertion> assertionSet = new HashSet<>(assertionList);
		group.setAssertions(assertionSet);
		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Delete assertion from a group",
		notes = "Removes supplied assertions from a given assertion group" )
	public AssertionGroup removeAssertionsFromGroup(@PathVariable Long id, @RequestBody(required = false) List<Assertion> assertions) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		for(Assertion assertion : assertions){
			assertionService.removeAssertionFromGroup(assertion, group);
		}

		return group;
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Add supplied assertions to an assertion group",
		notes = "Replaces existing assertions of an assertion group with supplied assertions" )
	public AssertionGroup setAsAssertionsInGroup(@PathVariable Long id, @RequestBody(required = false) Set<Assertion> assertions) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		// replace all existing assertions with current list
		group.setAssertions(assertions);

		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get an assertion group",
		notes = "Retrieves an assertion group for a given id" )
	public AssertionGroup getAssertionGroup(@PathVariable Long id) {
		return (AssertionGroup) entityService.find(AssertionGroup.class, id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Delete an assertion group",
		notes = "Delete an assertion group from the system" )
	public AssertionGroup deleteAssertionGroup(@PathVariable Long id) {
		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		entityService.delete(group);
		return group;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation( value = "Create an assertion group with specified name",
		notes = "Create an assertion group with specified name" )
	public AssertionGroup createAssertionGroupWithName(@RequestParam String name) {
		AssertionGroup group = new AssertionGroup();
		group.setName(name);
		return (AssertionGroup) entityService.create(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Update an assertion group",
		notes = "Update an assertion group identified with an id and name" )
	public AssertionGroup updateAssertionGroup(@PathVariable Long id,
			@RequestParam String name) {
		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		group.setName(name);
		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Execute all tests available in an assertion group",
		notes = "Execute all tests available in an assertion group identified by assertion group id. "
				+ "User is required to supply a run id,"
				+ " previous release version and prospective release version" )
	public Map<String, Object> executeAssertions(@PathVariable Long id,
										   @RequestParam Long runId, @RequestParam String prospectiveReleaseVersion,
										   @RequestParam String previousReleaseVersion) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		return assertionHelper.assertAssertions(assertionService.getAssertionsForGroup(group), runId, prospectiveReleaseVersion, previousReleaseVersion);
	}

	private List<Assertion> getAssertions(List<String> items){

		List<Assertion> assertions = new ArrayList<>();
		for(String item: items){
			try
			{
				if(item.matches("\\d+")){
					// treat as assertion id and retrieve associated assertion
					Assertion assertion = assertionService.find(Long.valueOf(item));
					if(assertion != null){
						assertions.add(assertion);
					}
				}
				else{
					assertions.add(objectMapper.readValue(item, Assertion.class));
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return assertions;
	}
}
