package org.ihtsdo.rvf.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.helper.AssertionHelper;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/assertions")
@Api(tags = "Assertions", description = "-")
public class AssertionController {
	@Autowired
	private AssertionService assertionService;
	@Autowired
	private AssertionHelper assertionHelper;
	@Autowired
	private ReleaseDataManager releaseDataManager;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get all assertions", notes = "Retrieves all assertions available in the system.")
	public List<Assertion> getAssertions() {
		return assertionService.findAll();
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid ID supplied."),
			@ApiResponse(code = 404, message = "Assertion tests not found.") })
	@ApiOperation(value = "Retrieves all tests for an assertion", notes = "Retrieves all tests which belong to a given assertion id.")
	public List<Test> getTestsForAssertion(@PathVariable final String id) {
		final Assertion assertion = find(id);

		return assertionService.getTestsByAssertionId(assertion.getAssertionId());
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Add tests to an assertion", notes = "Add one or more tests to an assertion identified by the id which can be the assertion id or uuid.")
	public Assertion addTestsForAssertion(@PathVariable final String id,
			@RequestBody(required = false) final List<Test> tests) {
		final Assertion assertion = find(id);

		if (assertion == null) {
			throw new EntityNotFoundException("Could not find assertion " + id);
		}

		assertionService.addTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Delete tests from an assertion", notes = "Delete tests for a given assertion. Note: This doesn't delete the assertion.")
	public Assertion deleteTestsForAssertion(
			@ApiParam(value = "Assertion id or uuid") @PathVariable final String id,
			@RequestParam List<Long> testIds) {
		final Assertion assertion = find(id);
		Collection<Test> tests = assertionService.getTests(assertion);
		List<Test> toDelete = new ArrayList<Test>();

		for (Long testId : testIds) {
			for (Test test : tests) {
				if (test.getId().equals(testId)) {
					toDelete.add(test);
				}
			}
		}

		if (!toDelete.isEmpty()) {
			assertionService.deleteTests(assertion, toDelete);
		}
		return assertion;
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Assertion not found") })
	@ApiOperation(value = "Get an assertion", notes = "Retrieves an assertion identified by the id.")
	public ResponseEntity<Assertion> getAssertion(
			@ApiParam(value = "Assertion id or uuid", required = true) @PathVariable final String id) {
		Assertion assertion = null;
		try {
			assertion = find(id);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<Assertion>((Assertion) null, HttpStatus.BAD_REQUEST);
		}
		if (assertion == null) {
			return new ResponseEntity<Assertion>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Assertion>(assertion, HttpStatus.OK);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Delete an assertion", notes = "Delete an assertion identified by the id.")
	public ResponseEntity<Assertion> deleteAssertion(@ApiParam(value = "Assertion id or uuid") @PathVariable final String id) {
		final Assertion assertion = find(id);
		if (assertion == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		List<AssertionGroup> groups = assertionService.getGroupsForAssertion(assertion);
		if ((groups != null) && !groups.isEmpty()) {
			return new ResponseEntity<Assertion>(assertion, HttpStatus.CONFLICT);
		}
		assertionService.delete(assertion);

		return new ResponseEntity<Assertion>(assertion, HttpStatus.OK);
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation(value = "Create an assertion", notes = "Create an assertion with values provided. The assertion id is not required as it will be auto generated. "
			+ "The uuid field is optional as a random uuid will be assigned when this is not set.")
	public ResponseEntity<Assertion> createAssertion(
			@RequestBody final Assertion assertion) {
		// Firstly, the assertion must have a UUID (otherwise malformed request)
		try {
			if (assertion.getUuid() == null) {
				return new ResponseEntity<Assertion>((Assertion) null, HttpStatus.BAD_REQUEST);
			}
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<Assertion>((Assertion) null, HttpStatus.BAD_REQUEST);
		}

		// Now make sure we don't already have one of those (otherwise conflict)
		Assertion existingAssertion = assertionService.findAssertionByUUID(assertion.getUuid());

		if (existingAssertion != null) {
			return new ResponseEntity<Assertion>((Assertion) null, HttpStatus.CONFLICT);
		}

		Assertion newAssertion = assertionService.create(assertion);

		return new ResponseEntity<Assertion>(newAssertion, HttpStatus.CREATED);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Update an assertion", notes = "Updates the assertion text,keywords and uuid property for the existing assertion identified by the assertion id or uuid.")
	public Assertion updateAssertion(
			@ApiParam(value = "Assertion id or uuid") @PathVariable final String id,
			@RequestBody(required = true) final Assertion assertion) {
		final Assertion existing = find(id);

		if (existing == null) {
			throw new EntityNotFoundException("No assertion found with id:"
					+ id);
		}

		assertion.setAssertionId(existing.getAssertionId());

		return assertionService.save(assertion);
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Update a specific test for a given assertion", notes = "Updates a specific test for a given assertion.")
	public Assertion updateTest(
			@ApiParam(value = "Assertion id or uuid") @PathVariable String id,
			@ApiParam(value="Test to be updated") @RequestBody(required = true) Test test) {
		final Assertion existing = find(id);

		if (existing == null) {
			throw new EntityNotFoundException("No assertion found with id:" + id);
		}
		assertionService.addTest(existing, test);
		return existing;
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Execute tests of an assertion", notes = "Executes tests for the assertion specified by the id (assertion id or uuid).")
	public ResponseEntity<Map<String, Object>> executeTest(
			@ApiParam("Assertion id or uuid") @PathVariable final String id,
			@ApiParam("Unique number")@RequestParam final Long runId,
			@ApiParam("The prospective version to be validated.") @RequestParam final String prospectiveReleaseVersion,
			@ApiParam("The previous release version. Not required when there is no previous release.") @RequestParam(required = false) final String previousReleaseVersion) {
		final Assertion assertion = find(id);
		if (assertion == null) {
			return new ResponseEntity<Map<String, Object>>((Map<String, Object>) null, HttpStatus.NOT_FOUND);
		}

		// Creating a list of 1 here so we can use the same code and receive the
		// same json as response
		final Collection<Assertion> assertions = new ArrayList<Assertion>(Arrays.asList(assertion));

		final MysqlExecutionConfig config = new MysqlExecutionConfig(runId);
		Map<String, Object> failures = new HashMap<String, Object>();

		if (prospectiveReleaseVersion != null && !releaseDataManager.isKnownRelease(prospectiveReleaseVersion)) {
			failures.put("failureMessage", "Release version not found:" + prospectiveReleaseVersion);

			return new ResponseEntity<Map<String, Object>>(failures, HttpStatus.NOT_FOUND);
		}

		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);

		if (previousReleaseVersion != null && !releaseDataManager.isKnownRelease(previousReleaseVersion)) {
			failures.put("failureMessage", "Release version not found:" + previousReleaseVersion);

			return new ResponseEntity<Map<String, Object>>(failures, HttpStatus.NOT_FOUND);
		}

		if (previousReleaseVersion == null) {
			config.setFirstTimeRelease(true);
		}

		return new ResponseEntity<Map<String, Object>>(assertionHelper.assertAssertions(assertions, config), HttpStatus.OK);
	}

	/**
	 * Attempts to look up id first as a UUID and if not, a database integer id
	 * value
	 * 
	 * @param id
	 * @return the referenced assertion
	 */
	private Assertion find(String id) {
		if ((id == null) || id.isEmpty()) {
			throw new InvalidFormatException("Id can't be null or empty");
		}
		if (id.contains("-")) {
			try {
				UUID uuid = UUID.fromString(id);
				return assertionService.findAssertionByUUID(uuid);
			} catch (IllegalArgumentException e) {
				throw new InvalidFormatException("Id is not a valid uuid:" + id);
			}
		} else {
			try {
				Long longId = new Long(id);
				return assertionService.find(longId);
			} catch (IllegalArgumentException e) {
				throw new InvalidFormatException("Id is not a valid assertion id:" + id);
			}
		}
	}
}
