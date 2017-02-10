package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import java.util.List;

@Controller
@RequestMapping("/tests")
@ApiIgnore
@Api(position = 2, value = "Assertion tests")
@Deprecated
public class TestController {

	@Autowired
	private EntityService entityService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get all tests", notes = "Gets all test available in the system")
	public List<Test> getTests() {
		return entityService.findAll(Test.class);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get a specific test", notes = "Gets the specific test identified with this id")
	public Test getTest(@PathVariable Long id) {
		return (Test) entityService.find(Test.class, id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Delete a specific test", notes = "Deletes the specific test identified with this id")
	@ApiIgnore
	public Test deleteTest(@PathVariable Long id) {
		Test test = (Test) entityService.find(Test.class, id);
		entityService.delete(test);
		return test;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation(value = "Add a test", notes = "Adds a test and returns the same populated with an id")
	@ApiIgnore
	public Test createTest(@RequestBody Test test) {
		return (Test) entityService.create(test);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Update a specific test", notes = "Updates a specific test with supplied details")
	public Test updateTest(@PathVariable Long id,
			@RequestBody(required = false) Test test) {
		Test test1 = (Test) entityService.find(Test.class, id);
		test.setId(test1.getId());
		return (Test) entityService.update(test);
	}

	@RequestMapping(value = "count", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Retrieves the total number of tests", notes = "Retrieves the total number of tests available in the system")
	public Long countTests() {
		return entityService.count(Test.class);
	}
}
