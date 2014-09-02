package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.helper.JsonEntityGenerator;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * temporary it will eventually map to /index and will load a view of the specific release centers
 * assertion test combinations. Don't want it loaded yet as i want to be able to create the assertions
 * and create the tests then we can discuss the business behind shared assertions.
 * Each release center has it's own combinations of assertionTests
 */

@Controller
@RequestMapping("/")
public class RootController {

	@Autowired
	private AssertionService assertionService;

	@Autowired
	private JsonEntityGenerator entityGenerator;

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getAssertions(HttpServletRequest request) {
		// todo pass through the release center should be part of security
		List<AssertionTest> assertions = new ArrayList<>();//assertionService.findAll();

		return entityGenerator.getEntityCollection(assertions, request);
	}

	@RequestMapping("/{id}")
	@ResponseBody
	public Map<String, Object> getBuild(@PathVariable Long id) {
		AssertionTest assertionTest = new AssertionTest(id, "some label"); //assertionService.find(id);
		return entityGenerator.getEntity(assertionTest);
	}

}
