package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.helper.AssertionHelper;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/assertions")
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
	public List<Assertion> getAssertions() {
		return assertionService.findAll();
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<Test> getTestsForAssertion(@PathVariable Long id) {

		Assertion assertion = assertionService.find(id);
		return assertionService.getTests(assertion.getId());
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion setTestsForAssertion(@PathVariable Long id, @RequestBody(required = false) List<Test> tests) {

		Assertion assertion = assertionService.find(id);
		assertionService.addTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion deleteTestsForAssertion(@PathVariable Long id, @RequestBody(required = false) List<Test> tests) {

		Assertion assertion = assertionService.find(id);
		assertionService.deleteTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion getAssertion(@PathVariable Long id) {
		return assertionService.find(id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion deleteAssertion(@PathVariable Long id) {
		Assertion assertion = assertionService.find(id);
		assertionService.delete(assertion);
		return assertion;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public Assertion createAssertion(@RequestBody Assertion assertion) {
		return assertionService.create(assertion);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion updateAssertion(@PathVariable Long id,
			@RequestBody(required = false) Assertion assertion) {
		Assertion assertion1 = assertionService.find(id);
		assertion.setId(assertion1.getId());
		return assertionService.update(assertion);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> executeTest(@PathVariable Long id,
										   @RequestParam Long runId, @RequestParam String prospectiveReleaseVersion,
										   @RequestParam String previousReleaseVersion) {

		Assertion assertion = assertionService.find(id);
		//Creating a list of 1 here so we can use the same code and receive the same json as response
		Collection<Assertion> assertions = new ArrayList<Assertion>(Arrays.asList(assertion));
		return assertionHelper.assertAssertions(assertions, runId, prospectiveReleaseVersion, previousReleaseVersion);
	}

	@RequestMapping(value = "/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> executeTest(@PathVariable List<Long> ids,
										   @RequestParam Long runId, @RequestParam String prospectiveReleaseVersion,
										   @RequestParam String previousReleaseVersion) {

		return assertionHelper.assertAssertions(assertionService.find(ids), runId, prospectiveReleaseVersion, previousReleaseVersion);
	}
	

}
