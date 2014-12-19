package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.helper.HypermediaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Root controller used to generate HATEOAS style hypermedia links
 */

@Controller
@RequestMapping("/")
public class RootController {

    @Autowired
    private HypermediaGenerator hypermediaGenerator;

    private static final String[] ROOT_LINK = {"assertions", "tests", "groups", "releases", "version"};

    @RequestMapping
    @ResponseBody
    public Map<String, Object> getRoot(HttpServletRequest request) {
        boolean currentResource = true;
        return hypermediaGenerator.getEntityHypermedia(new HashMap<String, String>(), currentResource, request, ROOT_LINK);
    }

}
