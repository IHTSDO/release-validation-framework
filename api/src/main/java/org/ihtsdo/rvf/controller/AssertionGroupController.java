package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
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
@RequestMapping("/groups")
public class AssertionGroupController {

	@Autowired
	private AssertionService assertionService;
	@Autowired
	private EntityService entityService;
    @Autowired
	private AssertionExecutionService assertionExecutionService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<AssertionGroup> getGroups() {
		return entityService.findAll(AssertionGroup.class);
	}

    @RequestMapping(value = "{id}/assertions", method = RequestMethod.GET)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<Assertion> getAssertionsForGroup(@PathVariable Long id) {

        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        return assertionService.getAssertionsForGroup(group);
	}

    @RequestMapping(value = "{id}/assertions", method = RequestMethod.POST)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup addAssertionsToGroup(@PathVariable Long id, @RequestBody(required = true) List<Assertion> assertions) {

        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        for(Assertion assertion : assertions){
            assertionService.addAssertionToGroup(assertion, group);
        }

        return group;
	}

    @RequestMapping(value = "{id}/assertions", method = RequestMethod.DELETE)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup removeAssertionsFromGroup(@PathVariable Long id, @RequestBody(required = true) List<Assertion> assertions) {

        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        for(Assertion assertion : assertions){
            assertionService.removeAssertionFromGroup(assertion, group);
        }

        return group;
	}

    @RequestMapping(value = "{id}/assertions", method = RequestMethod.PUT)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup setAsAssertionsInGroup(@PathVariable Long id, @RequestBody(required = false) Set<Assertion> assertions) {

        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        // replace all existing assertions with current list
        group.setAssertions(assertions);

        return (AssertionGroup) entityService.update(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup getAssertionGroup(@PathVariable Long id) {
        return (AssertionGroup) entityService.find(AssertionGroup.class, id);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup deleteAssertionGroup(@PathVariable Long id) {
        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        entityService.delete(group);
        return group;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
	public AssertionGroup createAssertionGroupWithName(@RequestParam String name) {
        AssertionGroup group = new AssertionGroup();
        group.setName(name);
		return (AssertionGroup) entityService.create(group);
	}

	@RequestMapping(value = "{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AssertionGroup updateAssertionGroup(@PathVariable Long id,
			@RequestParam String name) {
        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);
        group.setName(name);
        return (AssertionGroup) entityService.update(group);
	}

    @RequestMapping(value = "/{id}/run", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> executeAssertions(@PathVariable Long id,
                                           @RequestParam Long runId, @RequestParam String schemaName) {
        Map<String , Object> responseMap = new HashMap<>();
        Map<Assertion, Collection<TestRunItem>> map = new HashMap<>();
        assertionExecutionService.setSchemaName(schemaName);
        AssertionGroup group = (AssertionGroup) entityService.find(AssertionGroup.class, id);

        int failedAssertionCount = 0;
        for (Assertion assertion: assertionService.getAssertionsForGroup(group)) {
            try
            {
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
