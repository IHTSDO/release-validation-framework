package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Read access to the assertion groups.
 *
 * <p>Only the whole-collection listing is here: it goes through
 * {@link AssertionService}. Every other group endpoint addresses a group by its
 * database id through {@code AssertionGroupRepository}, and is in
 * {@link AssertionGroupAdministrationController} under the same URLs.
 */
@RestController
@RequestMapping("/groups")
@Tag(name = "Assertions Groups")
public class AssertionGroupController {

	private final AssertionService assertionService;

	@Autowired
	public AssertionGroupController(AssertionService assertionService) {
		this.assertionService = assertionService;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all assertion groups", description = "Retrieves all assertion groups defined in the system.")
	public List<AssertionGroup> getGroups() {
		List<AssertionGroup> result = assertionService.getAllAssertionGroups();
		if (result == null) {
			result = new ArrayList<>();
		}
		return result;
	}
}
