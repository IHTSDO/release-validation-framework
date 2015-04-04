package org.ihtsdo.rvf.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
import org.ihtsdo.rvf.helper.AssertionHelper;
import org.ihtsdo.rvf.service.AssertionService;
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
	public List<Test> getTestsForAssertion(@PathVariable final Long id) {

		final Assertion assertion = assertionService.find(id);
		return assertionService.getTests(assertion.getId());
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion setTestsForAssertion(@PathVariable final Long id, @RequestBody(required = false) final List<Test> tests) {

		final Assertion assertion = assertionService.find(id);
		assertionService.addTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}/tests", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion deleteTestsForAssertion(@PathVariable final Long id, @RequestBody(required = false) final List<Test> tests) {

		final Assertion assertion = assertionService.find(id);
		assertionService.deleteTests(assertion, tests);

		return assertion;
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion getAssertion(@PathVariable final Long id) {
		return assertionService.find(id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion deleteAssertion(@PathVariable final Long id) {
		final Assertion assertion = assertionService.find(id);
		assertionService.delete(assertion);
		return assertion;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public Assertion createAssertion(@RequestBody final Assertion assertion) {
		return assertionService.create(assertion);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Assertion updateAssertion(@PathVariable final Long id,
			@RequestBody(required = false) final Assertion assertion) {
		final Assertion assertion1 = assertionService.find(id);
		assertion.setId(assertion1.getId());
		return assertionService.update(assertion);
	}

	@RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> executeTest(@PathVariable final Long id,
										   @RequestParam final Long runId, @RequestParam final String prospectiveReleaseVersion,
										   @RequestParam final String previousReleaseVersion) {

		final Assertion assertion = assertionService.find(id);
		//Creating a list of 1 here so we can use the same code and receive the same json as response
		final Collection<Assertion> assertions = new ArrayList<Assertion>(Arrays.asList(assertion));
		final ExecutionConfig config = new ExecutionConfig(runId);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);
		return assertionHelper.assertAssertions(assertions, config);
	}

	@RequestMapping(value = "/run", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> executeTest(@PathVariable final List<Long> ids,
										   @RequestParam final Long runId, @RequestParam final String prospectiveReleaseVersion,
										   @RequestParam final String previousReleaseVersion) {

		final ExecutionConfig config = new ExecutionConfig(runId);
		config.setProspectiveVersion(prospectiveReleaseVersion);
		config.setPreviousVersion(previousReleaseVersion);
		return assertionHelper.assertAssertions(assertionService.find(ids), config);
	}
	

}
