package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The assertions a DuckDB run executes and their group membership, from the store alone.
 *
 * <p>Synthetic grouping files pin this class's behaviour.
 */
class DuckAssertionSourceTest {

	private static final String STATED = "994b5ff0-79b9-11e1-b0c4-0800200c9a66";
	private static final String EXAMPLE = "33333333-3333-3333-3333-333333333333";
	private static final String OTHER = "22222222-2222-2222-2222-222222222222";
	private static final String RESOURCE = "44444444-4444-4444-4444-444444444444";
	private static final String INT_RESOURCE = "55555555-5555-5555-5555-555555555555";

	private static final String GROUPS = """
			<?xml version="1.0" encoding="UTF-8"?>
			<assertionGroupingStrategy>
			  <group name="ExampleEdition" excludeByPolicy="stated-relationship"
			         includeStandaloneCategories="file-centric-validation,XX"/>
			  <group name="OtherEdition"
			         includeStandaloneCategories="file-centric-validation,extension-only,YY"/>
			  <group name="xx-authoring" includeByPolicy="category-centre-XX"/>
			</assertionGroupingStrategy>
			""";

	private static final String POLICIES = """
			<?xml version="1.0" encoding="UTF-8"?>
			<policyValues>
			  <policy name="stated-relationship" assertionUuids="994b5ff0-79b9-11e1-b0c4-0800200c9a66"/>
			  <policy name="category-centre-XX"
			          categoryWithCentrePairs="release-type-validation,XX|file-centric-validation,XX"/>
			</policyValues>
			""";

	/**
	 * Keywords in the corpus's shapes, since group resolution matches on them: "resource"
	 * alone, a default category with a centre, and an extension-only category.
	 */
	private static TestStores.Builder corpus() {
		return TestStores.builder()
				.assertion(STATED, "stated.sql", "select 1;",
						"The stated relationship file is an accurate derivative of the release.",
						"file-centric-validation,XX")
				.severity(STATED, "ERROR")
				.assertion(EXAMPLE, "example.sql", "select 2;",
						"Every active concept has a preferred term in the example language refset.",
						"file-centric-validation,XX")
				.severity(EXAMPLE, "ERROR")
				.assertion(OTHER, "other.sql", "select 3;",
						"Every active concept has a preferred term in the other language refset.",
						"file-centric-validation,extension-only,YY")
				.severity(OTHER, "WARNING")
				.assertion(RESOURCE, "pre.sql", "select 4;",
						"Build the tables the other assertions need.", "resource")
				.assertion(INT_RESOURCE, "intres.sql", "select 5;", "Build the INT-only tables.", "resource,INT")
				.grouping(GROUPS, POLICIES);
	}

	private static DuckAssertionSource source() throws IOException {
		return DuckAssertionSource.from(corpus().store());
	}

	@Test
	void assertionsCarryTheMetadataTheReportNeeds() throws IOException {
		// Each lands in a TestRunItem; a null would silently empty a report column.
		Assertion example = source().getAssertionByUuid(UUID.fromString(EXAMPLE));
		assertEquals("Every active concept has a preferred term in the example language refset.",
				example.getAssertionText());
		assertEquals("file-centric-validation,XX", example.getKeywords());
		assertEquals("ERROR", example.getSeverity());
	}

	@Test
	void theNumericAssertionIdIsLeftNullRatherThanInvented() throws IOException {
		// Assigned by the MySQL import, which does not happen in this mode; a synthesised
		// one would join to nothing and differ between two runs of the same store.
		assertNull(source().findAll().get(0).getAssertionId());
	}

	@Test
	void unknownUuidsAreNull() throws IOException {
		assertNull(source().getAssertionByUuid(UUID.fromString("99999999-9999-9999-9999-999999999999")));
	}

	@Test
	void assertionsAreSelectedByGroupName() throws IOException {
		List<Assertion> selected = source().getAssertionsInGroups(List.of("ExampleEdition"));
		assertEquals(List.of(EXAMPLE), uuids(selected),
				"the other edition's assertion is not selected, and the stated-relationship one is "
						+ "excluded by policy despite matching a category");
	}

	@Test
	void severalGroupsAreUnionedAndDeduplicatedInStoreOrder() throws IOException {
		// A run names several groups; an assertion in two of them is executed once.
		List<Assertion> both = source().getAssertionsInGroups(List.of("xx-authoring", "ExampleEdition"));
		assertEquals(List.of(STATED, EXAMPLE), uuids(both));
	}

	@Test
	void anUnknownGroupNameSelectsNothingAndDoesNotThrow() throws IOException {
		// As getAssertionGroupsByNames on the MySQL path; populatedGroupNames makes a
		// misspelt group diagnosable.
		assertEquals(List.of(), source().getAssertionsInGroups(List.of("NoSuchEdition")));
		assertEquals(Set.of("ExampleEdition", "OtherEdition", "xx-authoring"), source().populatedGroupNames());
	}

	@Test
	void resourceAssertionsMatchTheWholeKeywordsFieldNotOneToken() throws IOException {
		// The repository's derived query compares the column, so ("resource", true) does
		// not select "resource,INT"; a token match would run extra setup on every run.
		assertEquals(List.of(RESOURCE), uuids(source().getAssertionsByKeyWords("resource", true)));
		assertEquals(List.of(RESOURCE, INT_RESOURCE), uuids(source().getAssertionsByKeyWords("resource", false)),
				"the inexact form is a substring match over the field");
	}

	@Test
	void groupMembershipIsAlsoCarriedOnTheAssertion() throws IOException {
		// The failures extractor reads Assertion.getGroups() to decide whitelist eligibility.
		DuckAssertionSource source = source();
		assertEquals(Set.of("ExampleEdition", "xx-authoring"),
				source.getAssertionByUuid(UUID.fromString(EXAMPLE)).getGroups());
		assertEquals(Set.of(), source.getAssertionByUuid(UUID.fromString(RESOURCE)).getGroups(),
				"resource assertions sit in no group");
		assertEquals(Set.of("ExampleEdition", "xx-authoring"), source.groupsOf(UUID.fromString(EXAMPLE)));
	}

	@Test
	void aStoreWithoutGroupingFilesIsRefused() {
		// Otherwise no group would select anything and every run would pass.
		DuckStore store = TestStores.builder().noGrouping()
				.assertion(EXAMPLE, "example.sql", "select 2;", "Example", "file-centric-validation")
				.store();
		IOException e = assertThrows(IOException.class, () -> DuckAssertionSource.from(store));
		assertTrue(e.getMessage().contains("groups.xml"), e.getMessage());
	}

	@Test
	void groupingFilesWithNoGroupsAreRefused() {
		DuckStore store = corpus().grouping("<assertionGroupingStrategy/>", POLICIES).store();
		assertThrows(IOException.class, () -> DuckAssertionSource.from(store));
	}

	private static List<String> uuids(List<Assertion> assertions) {
		return assertions.stream().map(a -> a.getUuid().toString()).toList();
	}
}
