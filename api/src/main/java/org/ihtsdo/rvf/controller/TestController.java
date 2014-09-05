package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.helper.JsonEntityGenerator;
import org.ihtsdo.rvf.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tests")
public class TestController {

	@Autowired
	private AssertionService assertionService;

	@Autowired
	private JsonEntityGenerator entityGenerator;

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getAssertions(HttpServletRequest request) {
		List<Test> tests = new ArrayList<>();//assertionService.findAll();
		// temporary
		tests.add(new Test(1L, "First"));
		tests.add(new Test(2L, "Second"));
		tests.add(new Test(3L, "Third"));
		return entityGenerator.getEntityCollection(tests, request);
	}

}
