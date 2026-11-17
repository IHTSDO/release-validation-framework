package org.ihtsdo.rvf.rest.controller;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.repository.AssertionGroupRepository;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.DroolsRulesValidationService;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.ihtsdo.rvf.core.service.TraceabilityComparisonService;
import org.ihtsdo.rvf.rest.helper.AssertionHelper;
import org.ihtsdo.rvf.rest.helper.AssertionLookup;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The assertion and group endpoints are split into read controllers and
 * administration controllers. The split must not add, lose or duplicate an
 * endpoint, and the read controllers must not depend on anything that
 * administers or queries the database directly.
 */
class AssertionControllerSplitTest {

	/** Every assertion endpoint, as served before the split. */
	private static final Set<String> ASSERTION_ENDPOINTS = Set.of(
			"GET /assertions", "POST /assertions",
			"GET /assertions/{id}", "PUT /assertions/{id}", "DELETE /assertions/{id}",
			"GET /assertions/{id}/tests", "POST /assertions/{id}/tests",
			"PUT /assertions/{id}/tests", "DELETE /assertions/{id}/tests",
			"POST /assertions/{id}/run");

	/** Every group endpoint, as served before the split. */
	private static final Set<String> GROUP_ENDPOINTS = Set.of(
			"GET /groups", "POST /groups",
			"GET /groups/{id}", "PUT /groups/{id}", "DELETE /groups/{id}",
			"GET /groups/{id}/assertions", "POST /groups/{id}/assertions",
			"PUT /groups/{id}/assertions", "DELETE /groups/{id}/assertions",
			"PUT /groups/{id}/addAllAssertions", "POST /groups/{id}/run");

	private static final Set<Class<?>> DATABASE_TYPES = Set.of(
			AssertionGroupRepository.class, ReleaseDataManager.class, AssertionHelper.class);

	@Test
	void assertionEndpointsAreSplitWithoutLossOrOverlap() {
		assertSplit(ASSERTION_ENDPOINTS, AssertionController.class, AssertionAdministrationController.class);
	}

	@Test
	void groupEndpointsAreSplitWithoutLossOrOverlap() {
		assertSplit(GROUP_ENDPOINTS, AssertionGroupController.class, AssertionGroupAdministrationController.class);
	}

	@Test
	void readControllersServeOnlyReads() {
		for (Class<?> controller : List.of(AssertionController.class, AssertionGroupController.class)) {
			for (String endpoint : endpoints(controller)) {
				assertTrue(endpoint.startsWith("GET "), controller.getSimpleName() + " serves " + endpoint);
			}
		}
	}

	@Test
	void readControllersDoNotDependOnTheDatabaseDirectly() {
		for (Class<?> controller : List.of(AssertionController.class, AssertionGroupController.class)) {
			for (Constructor<?> constructor : controller.getConstructors()) {
				for (Class<?> parameter : constructor.getParameterTypes()) {
					assertFalse(DATABASE_TYPES.contains(parameter),
							controller.getSimpleName() + " depends on " + parameter.getSimpleName());
				}
			}
			assertTrue(Arrays.stream(controller.getDeclaredFields())
					.noneMatch(field -> DATABASE_TYPES.contains(field.getType())), controller.getSimpleName());
		}
	}

	@Test
	void theAssertionListingJoinsGroupMembershipAndAppendsRequestedRules() {
		Assertion first = assertion("first", "component-centric-validation");
		Assertion resource = assertion("a resource", "resource");
		AssertionService assertions = mock(AssertionService.class);
		when(assertions.findAll()).thenAnswer(invocation -> new ArrayList<>(List.of(first, resource)));
		AssertionGroup group = new AssertionGroup();
		group.setName("int-authoring");
		group.setAssertions(Set.of(first));
		when(assertions.getAllAssertionGroups()).thenReturn(List.of(group));
		DroolsRulesValidationService drools = mock(DroolsRulesValidationService.class);
		when(drools.getAssertions()).thenReturn(List.of(assertion("a Drools rule", "drools")));

		AssertionController controller = new AssertionController(assertions, mock(AssertionLookup.class),
				drools, mock(TraceabilityComparisonService.class));
		List<Assertion> listed = controller.getAssertions(true, false, false, true);

		assertEquals(List.of("first", "a Drools rule"), listed.stream().map(Assertion::getAssertionText).toList());
		assertTrue(listed.get(0).getGroups().contains("int-authoring"), "groups: " + listed.get(0).getGroups());
	}

	@Test
	void theGroupListingIsNeverNull() {
		AssertionService assertions = mock(AssertionService.class);
		when(assertions.getAllAssertionGroups()).thenReturn(null);

		assertEquals(List.of(), new AssertionGroupController(assertions).getGroups());
	}

	private static Assertion assertion(String text, String keywords) {
		Assertion assertion = new Assertion();
		assertion.setUuid(UUID.randomUUID());
		assertion.setAssertionText(text);
		assertion.setKeywords(keywords);
		return assertion;
	}

	private static void assertSplit(Set<String> expected, Class<?> read, Class<?> administration) {
		Set<String> readEndpoints = endpoints(read);
		Set<String> administrationEndpoints = endpoints(administration);
		Set<String> overlap = new TreeSet<>(readEndpoints);
		overlap.retainAll(administrationEndpoints);
		assertEquals(Set.of(), overlap, "served by both controllers");
		Set<String> union = new TreeSet<>(readEndpoints);
		union.addAll(administrationEndpoints);
		assertEquals(new TreeSet<>(expected), union);
	}

	private static Set<String> endpoints(Class<?> controller) {
		String prefix = controller.getAnnotation(RequestMapping.class).value()[0];
		Set<String> endpoints = new TreeSet<>();
		for (Method method : controller.getDeclaredMethods()) {
			RequestMapping mapping = method.getAnnotation(RequestMapping.class);
			if (mapping == null) {
				continue;
			}
			String path = mapping.value()[0];
			String full = path.isEmpty() ? prefix : prefix + (path.startsWith("/") ? path : "/" + path);
			for (var requestMethod : mapping.method()) {
				assertTrue(endpoints.add(requestMethod + " " + full), "duplicate mapping " + full);
			}
		}
		return endpoints;
	}
}
