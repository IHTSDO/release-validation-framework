package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.Test;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.rest.helper.AssertionHelper;
import org.ihtsdo.rvf.rest.helper.AssertionLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Write access to the assertion catalogue, and single-assertion execution
 * against release data already loaded into the database.
 *
 * <p>Split out of {@link AssertionController}, which keeps the read endpoints.
 * Everything here either changes the stored catalogue or needs
 * {@link ReleaseDataManager} and {@link AssertionHelper} to run an assertion
 * against a loaded release. The URLs are unchanged.
 */
@RestController
@RequestMapping("/assertions")
@Tag(name = "Assertions")
public class AssertionAdministrationController {

	private final AssertionService assertionService;
	private final AssertionLookup assertionLookup;
	private final AssertionHelper assertionHelper;
	private final ReleaseDataManager releaseDataManager;

	@Autowired
	public AssertionAdministrationController(AssertionService assertionService,
			AssertionLookup assertionLookup,
			AssertionHelper assertionHelper,
			ReleaseDataManager releaseDataManager) {
		this.assertionService = assertionService;
		this.assertionLookup = assertionLookup;
		this.assertionHelper = assertionHelper;
		this.releaseDataManager = releaseDataManager;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Add tests to an assertion", description = "Add one or more tests to an assertion identified by the id which can be the assertion id or uuid.")
	public Assertion addTestsForAssertion(@PathVariable final String id,
			@RequestBody(required = false) final List<Test> tests) {
		final Assertion assertion = assertionLookup.find(id);

		if (assertion == null) {
			throw new EntityNotFoundException("Could not find assertion " + id);
		}

		assertionService.addTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Delete tests from an assertion", description = "Delete tests for a given assertion. Note: This doesn't delete the assertion.")
	public Assertion deleteTestsForAssertion(
			@Parameter(description = "Assertion id or uuid") @PathVariable final String id,
			@RequestParam List<Long> testIds) {
		final Assertion assertion = assertionLookup.find(id);
		Collection<Test> tests = assertionService.getTests(assertion);
		List<Test> toDelete = new ArrayList<>();

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

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Delete an assertion", description = "Delete an assertion identified by the id.")
	public ResponseEntity<Assertion> deleteAssertion(@Parameter(description = "Assertion id or uuid") @PathVariable final String id) {
		final Assertion assertion = assertionLookup.find(id);
		if (assertion == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		List<AssertionGroup> groups = assertionService.getGroupsForAssertion(assertion);
		if ((groups != null) && !groups.isEmpty()) {
			return new ResponseEntity<>(assertion, HttpStatus.CONFLICT);
		}
		assertionService.delete(assertion);

		return new ResponseEntity<>(assertion, HttpStatus.OK);
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an assertion", description = "Create an assertion with values provided. The assertion id is not required as it will be auto generated. "
			+ "The uuid field is optional as a random uuid will be assigned when this is not set.")
	public ResponseEntity<Assertion> createAssertion(
			@RequestBody final Assertion assertion) {
		// Firstly, the assertion must have a UUID (otherwise malformed request)
		try {
			if (assertion.getUuid() == null) {
				return new ResponseEntity<>((Assertion) null, HttpStatus.BAD_REQUEST);
			}
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>((Assertion) null, HttpStatus.BAD_REQUEST);
		}

		// Now make sure we don't already have one of those (otherwise conflict)
		Assertion existingAssertion = assertionService.findAssertionByUUID(assertion.getUuid());

		if (existingAssertion != null) {
			return new ResponseEntity<>((Assertion) null, HttpStatus.CONFLICT);
		}

		Assertion newAssertion = assertionService.create(assertion);

		return new ResponseEntity<>(newAssertion, HttpStatus.CREATED);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Update an assertion", description = "Updates the assertion text,keywords and uuid property for the existing assertion identified by the assertion id or uuid.")
	public Assertion updateAssertion(
			@Parameter(description = "Assertion id or uuid") @PathVariable final String id,
			@RequestBody(required = true) final Assertion assertion) {
		final Assertion existing = assertionLookup.find(id);

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
	@Operation(summary = "Update a specific test for a given assertion", description = "Updates a specific test for a given assertion.")
	public Assertion updateTest(
			@Parameter(description = "Assertion id or uuid") @PathVariable String id,
			@Parameter(description = "Test to be updated") @RequestBody(required = true) Test test) {
		final Assertion existing = assertionLookup.find(id);

		if (existing == null) {
			throw new EntityNotFoundException("No assertion found with id:" + id);
		}
		assertionService.addTest(existing, test);
		return existing;
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Execute tests of an assertion", description = "Executes tests for the assertion specified by the id (assertion id or uuid).")
	public ResponseEntity<Map<String, Object>> executeTest(
			@Parameter(description = "Assertion id or uuid") @PathVariable final String id,
			@Parameter(description = "Unique number") @RequestParam final Long runId,
			@Parameter(description = "The prospective version to be validated.") @RequestParam final String prospectiveReleaseVersion,
			@Parameter(description = "The previous release version. Not required when there is no previous release.") @RequestParam(required = false) final String previousReleaseVersion) {
		final Assertion assertion = assertionLookup.find(id);
		if (assertion == null) {
			return new ResponseEntity<>((Map<String, Object>) null, HttpStatus.NOT_FOUND);
		}

		// Creating a list of 1 here so we can use the same code and receive the
		// same json as response
		final Collection<Assertion> assertions = new ArrayList<>(List.of(assertion));

		final MysqlExecutionConfig config = new MysqlExecutionConfig(runId);
		Map<String, Object> failures = new HashMap<>();

		if (prospectiveReleaseVersion != null && !releaseDataManager.isKnownRelease(prospectiveReleaseVersion)) {
			failures.put("failureMessage", "Release version not found:" + prospectiveReleaseVersion);

			return new ResponseEntity<>(failures, HttpStatus.NOT_FOUND);
		}

		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);

		if (previousReleaseVersion != null && !releaseDataManager.isKnownRelease(previousReleaseVersion)) {
			failures.put("failureMessage", "Release version not found:" + previousReleaseVersion);

			return new ResponseEntity<>(failures, HttpStatus.NOT_FOUND);
		}

		if (previousReleaseVersion == null) {
			config.setFirstTimeRelease(true);
		}

		return new ResponseEntity<>(assertionHelper.assertAssertions(assertions, config), HttpStatus.OK);
	}
}
