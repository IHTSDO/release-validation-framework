package org.ihtsdo.rvf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

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
	private ObjectMapper objectMapper = new ObjectMapper();
	private final Logger logger = LoggerFactory.getLogger(AssertionGroupController.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<AssertionGroup> getGroups() {
		return entityService.findAll(AssertionGroup.class);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<Assertion> getAssertionsForGroup(@PathVariable Long id) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		return assertionService.getAssertionsForGroup(group);
	}

	@RequestMapping(value = "{id}/assertions", method = RequestMethod.POST)
	@ResponseBody
	public AssertionGroup addAssertionsToGroup(@PathVariable Long id, @RequestBody(required = false) List<String> assertionsList, HttpServletResponse response) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		
		//Do we have anything to add?
		if (assertionsList == null || assertionsList.size() == 0) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			List<Assertion> assertions = getAssertions(assertionsList);
			for(Assertion assertion : assertions){
				assertionService.addAssertionToGroup(assertion, group);
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}

		return group;
	}
	
	@RequestMapping(value = "{id}/addAllAssertions", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
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
	public AssertionGroup setAsAssertionsInGroup(@PathVariable Long id, @RequestBody(required = false) Set<Assertion> assertions) {

		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		// replace all existing assertions with current list
		group.setAssertions(assertions);

		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup getAssertionGroup(@PathVariable Long id) {
		return (AssertionGroup) entityService.find(AssertionGroup.class, id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup deleteAssertionGroup(@PathVariable Long id) {
		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		entityService.delete(group);
		return group;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public AssertionGroup createAssertionGroupWithName(@RequestParam String name) {
		AssertionGroup group = new AssertionGroup();
		group.setName(name);
		return (AssertionGroup) entityService.create(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public AssertionGroup updateAssertionGroup(@PathVariable Long id,
			@RequestParam String name) {
		AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
		group.setName(name);
		return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
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
