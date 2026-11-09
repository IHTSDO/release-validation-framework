package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lookups on {@link DuckAssertionService}, the store-backed assertion service: what
 * {@code GET /assertions/{uuid}/source} returns, and the questions the store cannot answer.
 */
class DuckAssertionSourceLookupTest {

	private static final String PRESENT = "11111111-2222-3333-4444-555555555555";
	private static final String ABSENT = "99999999-8888-7777-6666-555555555555";

	@TempDir
	Path temp;

	/** Through the production constructor: a locator over a store file. */
	private DuckAssertionService service() {
		Path store = TestStores.builder()
				.assertion(PRESENT, "a-check.sql", "SELECT 1;\n", "A check that something holds",
						"component-centric-validation",
						"INSERT INTO qa_result SELECT 1", "SELECT 2")
				.write(temp.resolve("store.json"));
		return new DuckAssertionService(new DuckStoreLocator(store.toString()));
	}

	@Test
	void returnsTheStatementsTheEngineWouldExecute() {
		Optional<DuckStore.StoredAssertion> found = service().storedAssertion(PRESENT);

		assertTrue(found.isPresent(), "the store carries this assertion");
		DuckStore.StoredAssertion stored = found.orElseThrow();
		assertEquals("a-check.sql", stored.file());
		assertEquals("A check that something holds", stored.text());
		assertEquals(List.of("INSERT INTO qa_result SELECT 1", "SELECT 2"), stored.statements(),
				"every statement, in order: an assertion is not always one query");
	}

	@Test
	void anAssertionWithNoSqlIsAbsentRatherThanEmpty() {
		// A Drools rule or MRCM check is real in a report and never in the store; absent,
		// so the endpoint can say why rather than return an empty body.
		assertFalse(service().storedAssertion(ABSENT).isPresent());
	}

	@Test
	void assertionsAndGroupsAreLookedUpByUuidAndName() {
		DuckAssertionService service = service();
		Assertion assertion = service.findAssertionByUUID(UUID.fromString(PRESENT));
		assertEquals("A check that something holds", assertion.getAssertionText());
		assertEquals(assertion, service.getAssertionByUuid(UUID.fromString(PRESENT)));
		assertNull(service.getAssertionByUuid(UUID.fromString(ABSENT)));
		assertEquals(1L, service.count());

		AssertionGroup group = service.getAssertionGroupByName("component-centric-validation");
		assertEquals(1, group.getAssertions().size());
		assertNull(service.getAssertionGroupByName("NoSuchGroup"));
		assertEquals(1, service.getAssertionGroupsByNames(List.of("component-centric-validation", "NoSuchGroup")).size());
		assertEquals(List.of("component-centric-validation"),
				service.getGroupsForAssertion(assertion).stream().map(AssertionGroup::getName).toList());
	}

	@Test
	void numericIdLookupsFindNothing() {
		// Numeric ids are assigned by the MySQL import; a synthesised one would address a
		// different assertion on each engine.
		DuckAssertionService service = service();
		assertNull(service.find(1L));
		assertEquals(List.of(), service.getTestsByAssertionId(1L));
		assertEquals(List.of(), service.getGroupsForAssertion(1L));
	}

	@Test
	void mutationIsRefused() {
		// The corpus is the published store, not a database.
		DuckAssertionService service = service();
		Assertion assertion = service.findAssertionByUUID(UUID.fromString(PRESENT));
		assertThrows(UnsupportedOperationException.class, () -> service.create(new Assertion()));
		assertThrows(UnsupportedOperationException.class, () -> service.save(assertion));
		assertThrows(UnsupportedOperationException.class, () -> service.delete(assertion));
		assertThrows(UnsupportedOperationException.class,
				() -> service.addAssertionToGroup(assertion, new AssertionGroup()));
		assertThrows(UnsupportedOperationException.class,
				() -> service.createAssertionGroup(new AssertionGroup()));
	}
}
