package org.ihtsdo.rvf.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ihtsdo.rvf.helper.HypermediaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import springfox.documentation.annotations.ApiIgnore;

/**
 * Root controller used to generate HATEOAS style hypermedia links
 */

@Controller
@RequestMapping("/")
@ApiIgnore
public class RootController {

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] ROOT_LINK = { "assertions", "tests",
			"groups", "releases", "version" };

	@RequestMapping
	@ResponseBody
	public Map<String, Object> getRoot(HttpServletRequest request) {
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(
				new HashMap<String, String>(), currentResource, request,
				ROOT_LINK);
	}

	
	@RequestMapping(path = "/", method = RequestMethod.GET)
	public void getRoot(HttpServletResponse response) throws IOException {
		response.sendRedirect("swagger-ui.html");
	}

}
