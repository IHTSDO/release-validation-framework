package org.ihtsdo.rvf.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
import org.ihtsdo.rvf.helper.AssertionHelper;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.amazonaws.services.kms.model.NotFoundException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@RequestMapping("/assertions")
@Api(value = "Assertions")
public class AssertionController {

	@Autowired
	private AssertionService assertionService;
	@Autowired
	private AssertionExecutionService assertionExecutionService;
	@Autowired
	private AssertionHelper assertionHelper;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get all assertions",
		notes = "Retrieves currently available assertions in the system" )
	public List<Assertion> getAssertions() {
		return assertionService.findAll();
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Retrieves all tests for an assertion",
		notes = "Retrieves all test which belongs to a given assertion id" )
	public List<Test> getTestsForAssertion(@PathVariable final String id) {

		final Assertion assertion = find(id);
		return assertionService.getTests(assertion.getId());
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Add tests to an assertion",
		notes = "Add one or more test to an assertion identified with provided assertion id."
				+ "And returns that assertion " )
	public Assertion setTestsForAssertion(@PathVariable final String id, @RequestBody(required = false) final List<Test> tests) {

		final Assertion assertion = find(id);
		if (assertion == null) {
			throw new EntityNotFoundException("Could not find assertion " + id );
		}
		assertionService.addTests(assertion, tests);
		return assertion;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Delete tests from an assertion",
		notes = "Delete tests from an assertion and returns an assertion from which tests was deleted" )
	public Assertion deleteTestsForAssertion(@PathVariable final String id, @RequestBody(required = false) final List<Test> tests) {

		final Assertion assertion = find(id);
		assertionService.deleteTests(assertion, tests);
		return assertion;
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get an assertion",
		notes = "Retrieves an assertion identified with given assertion id" )
	public ResponseEntity<Assertion> getAssertion(@PathVariable final String id) {
		Assertion assertion = find(id);
		if (assertion == null) {
			return new ResponseEntity<Assertion>((Assertion)null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Assertion>(assertion, HttpStatus.OK);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Delete an assertion",
		notes = "Delete an assertion identified by given assertion id and returns deleted assertion" )
	public Assertion deleteAssertion(@PathVariable final String id) {
		final Assertion assertion = find(id);
		assertionService.delete(assertion);
		return assertion;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation( value = "Create an assertion",
		notes = "Create an assertion with input supplied and returns it popluated with an assertion id" )
	public ResponseEntity<Assertion> createAssertion(@RequestBody final Assertion assertion) {
		//Firstly, the assertion must have a UUID (otherwise malformed request)
		if (assertion.getUuid() == null) {
			return new ResponseEntity<Assertion>((Assertion)null, HttpStatus.BAD_REQUEST);
		}
		
		//Now make sure we don't already have one of those (otherwise conflict)
		Assertion existingAssertion = assertionService.find(assertion.getUuid());
		if (existingAssertion != null) {
			return new ResponseEntity<Assertion>((Assertion)null, HttpStatus.CONFLICT);
		}
		
		Assertion newAssertion = assertionService.create(assertion);
		return new ResponseEntity<Assertion>(newAssertion, HttpStatus.CREATED);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Update an assertion",
		notes = "Update an assertion and returns this updated assertion" )
	public Assertion updateAssertion(@PathVariable final String id,
			@RequestBody(required = false) final Assertion assertion) {
		final Assertion assertion1 = find(id);
		assertion.setId(assertion1.getId());
		return assertionService.update(assertion);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Execute tests of an assertion",
		notes = "Execute all tests under an assertion with provided runid ( a user supplied identifier), "
				+ " prospective release version and previous release version."
				+ "Run id later used to retrieve assertion " )
	public ResponseEntity<Map<String, Object>> executeTest(@PathVariable final String id,
										   @RequestParam final Long runId, @RequestParam final String prospectiveReleaseVersion,
										   @RequestParam final String previousReleaseVersion) {

		final Assertion assertion = find(id);
		if (assertion == null) {
			return new ResponseEntity<Map<String, Object>>((Map<String, Object>)null, HttpStatus.NOT_FOUND);
		}
		//Creating a list of 1 here so we can use the same code and receive the same json as response
		final Collection<Assertion> assertions = new ArrayList<Assertion>(Arrays.asList(assertion));
		final ExecutionConfig config = new ExecutionConfig(runId);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);
		return new ResponseEntity<Map<String, Object>> (assertionHelper.assertAssertions(assertions, config), HttpStatus.OK);
	}

	@RequestMapping(value = "/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Execute tests of required assertions",
		notes = "Execute tests belongs to required assertions identified by their ids. "
				+ " This excution requires an user supplied run id to identify this run, "
				+ " previous release version and prospective release version" )
	public Map<String, Object> executeTest(@PathVariable final List<Long> ids,
										   @RequestParam final Long runId, @RequestParam final String prospectiveReleaseVersion,
										   @RequestParam final String previousReleaseVersion) {

		final ExecutionConfig config = new ExecutionConfig(runId);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);
		return assertionHelper.assertAssertions(assertionService.find(ids), config);
	}
	
	/**
	 * Attempts to look up id first as a UUID and if not, a database integer id value
	 * @param id
	 * @return the referenced assertion
	 */
	private Assertion find(String id) {
		if (id == null || id.isEmpty()) {
			return null;
		} else if (id.contains("-")) {
			UUID uuid = UUID.fromString(id);
			return assertionService.find(uuid);
		} else {
			Long longId = new Long(id);
			return assertionService.find(longId);
		}
		
	}
	

}
