package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import java.util.List;

@Controller
@RequestMapping("/tests")
@Api(value = "Tests")
public class TestController {

    @Autowired
    private EntityService entityService;
    @Autowired
    private AssertionExecutionService assertionExecutionService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get all tests", notes = "Get all test available in the system" )
    public List<Test> getTests() {
        return entityService.findAll(Test.class);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get a specific test", notes = "Get a specific test identified with this id" )
    public Test getTest(@PathVariable Long id) {
        return (Test) entityService.find(Test.class, id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Delete a specific test", notes = "Delete a specific test identified with this id" )
    public Test deleteTest(@PathVariable Long id) {
        Test test = (Test) entityService.find(Test.class, id);
        entityService.delete(test);
        return test;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
	@ApiOperation( value = "Add a test", notes = "Add a test and returns the same populated with an id" )
    public Test createTest(@RequestBody Test test) {
        return (Test) entityService.create(test);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Update a specific test", notes = "Update a specific test with supplied details" )
    public Test updateTest(@PathVariable Long id,
                                     @RequestBody(required = false) Test test) {
        Test test1 = (Test) entityService.find(Test.class, id);
        test.setId(test1.getId());
        return (Test) entityService.update(test);
    }

    @RequestMapping(value = "count", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Retrieves count all tests", notes = "Retrieves count tests available in the system" )
    public Long countTests() {
        return entityService.count(Test.class);
    }

/*
 *  PGW: I don't think it's valid to execute a test without it being linked to an assertion.   Tests need to know what 
 *  assertion they belong to in order to report what rule is being broken.  
    @RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public TestRunItem executeTest(@PathVariable Long id,
                                   @RequestParam Long runId, @RequestParam String prospectiveReleaseVersion,
                                   @RequestParam String previousReleaseVersion) {
        Test test1 = (Test) entityService.find(Test.class, id);
        return assertionExecutionService.executeTest(test1, runId, prospectiveReleaseVersion, previousReleaseVersion);
    } */
}
