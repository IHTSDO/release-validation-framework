package org.ihtsdo.rvf.importer;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link AssertionGroupImporter#resolveGroups} decides group membership from
 * {@code groups.xml} and {@code policies.xml} without a database, and must decide
 * it exactly as an import into the database does.
 */
class AssertionGroupResolveTest {

	private static final String GROUPS = """
			<?xml version="1.0" encoding="UTF-8"?>
			<assertionGroupingStrategy>
			  <group name="EditionA" excludeByPolicy="stated-relationship"
			         includeStandaloneCategories="file-centric-validation,component-centric-validation,AA"/>
			  <group name="EditionB"
			         includeStandaloneCategories="file-centric-validation,extension-only,BB"/>
			  <group name="aa-authoring" includeByPolicy="category-centre-AA"/>
			</assertionGroupingStrategy>
			""";

	// policies.xml packs each list into one attribute.
	private static final String POLICIES = """
			<?xml version="1.0" encoding="UTF-8"?>
			<policyValues>
			  <policy name="stated-relationship"
			          assertionUuids="11111111-1111-1111-1111-111111111111"/>
			  <policy name="category-centre-AA"
			          categoryWithCentrePairs="release-type-validation,AA|file-centric-validation,AA"/>
			</policyValues>
			""";

	private final Assertion stated = assertion("11111111-1111-1111-1111-111111111111",
			"The stated relationship file is an accurate derivative.", "file-centric-validation,AA");
	private final Assertion editionB = assertion("22222222-2222-2222-2222-222222222222",
			"Every active concept has a preferred term in the B dialect.", "file-centric-validation,BB,extension-only");
	private final Assertion editionA = assertion("33333333-3333-3333-3333-333333333333",
			"Edition A concepts are members of the A refset.", "file-centric-validation,AA");
	private final Assertion resource = assertion("44444444-4444-4444-4444-444444444444",
			"Build a table of concepts edited this cycle.", "resource");

	@Test
	void anotherCentresAssertionIsNotSelected() {
		Map<String, Set<String>> groups = resolve(List.of(editionB, editionA));

		assertEquals(Set.of("EditionB"), groups.get(editionB.getUuid().toString()));
		assertEquals(Set.of("EditionA", "aa-authoring"), groups.get(editionA.getUuid().toString()));
	}

	@Test
	void excludeByPolicyBeatsAnIncludingCategory() {
		Set<String> groups = resolve(List.of(stated)).get(stated.getUuid().toString());

		assertFalse(groups.contains("EditionA"), "excluded by the stated-relationship policy");
		assertTrue(groups.contains("aa-authoring"), "a group without that exclusion still selects it");
	}

	@Test
	void anAssertionMatchingNothingBelongsToNoGroup() {
		assertEquals(Set.of(), resolve(List.of(resource)).get(resource.getUuid().toString()));
	}

	@Test
	void noGroupDefinitionsIsAnErrorAsOnImport() {
		InputStream noGroups = stream("<assertionGroupingStrategy/>");

		assertThrows(IllegalStateException.class,
				() -> AssertionGroupImporter.resolveGroups(noGroups, stream(POLICIES), List.of(editionA)));
	}

	@Test
	void resolvesExactlyTheMembershipAnImportWrites() {
		List<Assertion> corpus = List.of(stated, editionB, editionA, resource);
		AssertionService assertionService = mock(AssertionService.class);
		when(assertionService.getAllAssertionGroups()).thenReturn(new ArrayList<>());
		when(assertionService.createAssertionGroup(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(assertionService.findAll()).thenReturn(corpus);
		Map<String, Set<String>> imported = new HashMap<>();
		doAnswer(invocation -> {
			Assertion assertion = invocation.getArgument(0);
			AssertionGroup group = invocation.getArgument(1);
			imported.computeIfAbsent(assertion.getUuid().toString(), uuid -> new HashSet<>()).add(group.getName());
			return null;
		}).when(assertionService).addAssertionToGroup(any(), any());

		new AssertionGroupImporter(assertionService).importAssertionGroups(stream(GROUPS), stream(POLICIES));

		Map<String, Set<String>> resolved = new HashMap<>(resolve(corpus));
		resolved.values().removeIf(Set::isEmpty);
		assertEquals(imported, resolved);
	}

	private static Map<String, Set<String>> resolve(List<Assertion> assertions) {
		return AssertionGroupImporter.resolveGroups(stream(GROUPS), stream(POLICIES), assertions);
	}

	private static InputStream stream(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	private static Assertion assertion(String uuid, String text, String keywords) {
		Assertion assertion = new Assertion();
		assertion.setUuid(UUID.fromString(uuid));
		assertion.setAssertionText(text);
		assertion.setKeywords(keywords);
		return assertion;
	}
}
