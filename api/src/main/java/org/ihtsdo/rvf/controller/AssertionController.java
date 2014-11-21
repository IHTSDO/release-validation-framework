package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;
import org.ihtsdo.rvf.helper.MissingEntityException;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.EntityService;
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
	private EntityService entityService;

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

//        // verify if IHTSDO exists as a relese centre, otherwise create it
//        ReleaseCenter releaseCenter = null;
//        try {
//            releaseCenter = (ReleaseCenter) entityService.find(ReleaseCenter.class, assertionService.getIhtsdo().getId());
//        }
//        catch (MissingEntityException e) {
//            e.printStackTrace();
//        }
//
//        if(releaseCenter == null){
//            releaseCenter = (ReleaseCenter) entityService.create(assertionService.getIhtsdo());
//        }
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

    @RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> executeTest(@PathVariable Long id,
                                           @RequestParam Long runId, @RequestParam String schemaName) {
        Map<String, Object> responseMap = new HashMap<>();
        Assertion assertion = assertionService.find(id);
        assertionExecutionService.setSchemaName(schemaName);
        List<TestRunItem> items = new ArrayList<>(assertionExecutionService.executeAssertion(assertion, runId));
        // get only first since we have 1:1 correspondence between Assertion and Test
        if(items.size() == 1){
            TestRunItem runItem = items.get(0);
            if(runItem.isFailure()){
                responseMap.put("failureMessage", runItem.getFailureMessage());
            }
        }
        responseMap.put("assertion", assertion);
        responseMap.put("result", items);

        return responseMap;
    }

    @RequestMapping(value = "/run", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> executeTest(@PathVariable List<Long> ids,
                                           @RequestParam Long runId, @RequestParam String schemaName) {
        Map<String , Object> responseMap = new HashMap<>();
        Map<Assertion, Collection<TestRunItem>> map = new HashMap<>();
        assertionExecutionService.setSchemaName(schemaName);
        int failedAssertionCount = 0;
        for (Long id: ids) {
            try {
                Assertion assertion = assertionService.find(id);
                List<TestRunItem> items = new ArrayList<>(assertionExecutionService.executeAssertion(assertion, runId));
                // get only first since we have 1:1 correspondence between Assertion and Test
                if(items.size() == 1){
                    TestRunItem runItem = items.get(0);
                    if(runItem.isFailure()){
                        failedAssertionCount++;
                    }
                }
                map.put(assertion, items);
            }
            catch (MissingEntityException e) {
                failedAssertionCount++;
            }
        }

        responseMap.put("assertions", map);
        responseMap.put("assertionsRun", map.keySet().size());
        responseMap.put("assertionsFailed", failedAssertionCount);

        return responseMap;
    }
}
