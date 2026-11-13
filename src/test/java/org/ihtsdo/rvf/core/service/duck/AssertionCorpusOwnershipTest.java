package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who owns the list {@code findAll()} returns, and the objects in it.
 *
 * <p>The DuckDB corpus is held in memory and shared by every run, while callers such as
 * {@code GET /assertions?includeDroolsRules=true} append to the list they are given and call
 * {@code addGroup} on its assertions, as they may with the JPA implementation. Neither may
 * change what a later validation executes.
 */
class AssertionCorpusOwnershipTest {

	private static final String UUID_A = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_B = "22222222-2222-2222-2222-222222222222";

	private static DuckAssertionService service() throws IOException {
		DuckStore store = TestStores.builder()
				.assertion(UUID_A, "a.sql", "insert into qa_result select 1;\n", "first",
						"component-centric-validation", "INSERT INTO qa_result SELECT 1")
				.assertion(UUID_B, "b.sql", "insert into qa_result select 2;\n", "second",
						"component-centric-validation", "INSERT INTO qa_result SELECT 2")
				.store();
		return new DuckAssertionService(
				new DuckAssertionService.Corpus(store, DuckAssertionSource.from(store), List.of()));
	}

	@Test
	void findAllHandsBackAMutableCopy() throws IOException {
		// The JPA implementation returns a fresh list per call and callers append to it;
		// an unmodifiable view would fail those callers, the shared list would grow the corpus.
		DuckAssertionService service = service();
		List<Assertion> assertions = service.findAll();
		assertEquals(2, assertions.size());

		assertions.add(assertion());
		assertEquals(3, assertions.size(), "the caller owns what it was given");
		assertEquals(2, service.findAll().size(), "and the corpus is unchanged");
	}

	@Test
	void joiningGroupsOnSharedAssertionsIsSafeAndIdempotent() throws IOException {
		// The objects are shared and callers add groups to them, possibly concurrently;
		// the corpus hands out concurrent sets so no caller has to know.
		DuckAssertionService service = service();
		Assertion assertion = service.findAll().get(0);
		int before = assertion.getGroups().size();

		assertion.addGroup("component-centric-validation");
		assertion.addGroup("a-group-it-did-not-have");

		assertEquals(before + 1, assertion.getGroups().size(),
				"re-adding an existing group changes nothing, and a new one is accepted");
		assertTrue(service.findAll().get(0).getGroups().contains("a-group-it-did-not-have"),
				"the objects are shared, which is why the set must be thread safe");
	}

	@Test
	void groupSetsTolerateConcurrentAdds() throws Exception {
		Assertion assertion = service().findAll().get(0);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < 8; t++) {
			int offset = t;
			threads.add(Thread.ofPlatform().start(() -> {
				for (int i = 0; i < 500; i++) {
					assertion.addGroup("group-" + (offset * 500 + i));
				}
			}));
		}
		for (Thread thread : threads) {
			thread.join();
		}
		assertEquals(1 + 8 * 500, assertion.getGroups().size());
	}

	@Test
	void theCorpusCarriesItsGroupsWithoutBeingToldThem() throws IOException {
		// Resolved from the store when it loads, so GET /assertions is right without any
		// caller mutating the shared objects.
		for (Assertion assertion : service().findAll()) {
			assertTrue(assertion.getGroups() != null
							&& assertion.getGroups().contains("component-centric-validation"),
					assertion.getUuid() + " should carry its group: " + assertion.getGroups());
		}
	}

	@Test
	void repeatedBrowsesDoNotGrowTheCorpus() throws IOException {
		DuckAssertionService service = service();
		for (int browse = 0; browse < 2; browse++) {
			List<Assertion> forThisRequest = service.findAll();
			forThisRequest.add(assertion());
			assertEquals(3, forThisRequest.size());
		}
		assertEquals(2, service.findAll().size(), "two browses, corpus still the corpus");
		assertEquals(2, service.getAssertionsByKeyWords("component-centric-validation", true).size());
	}

	private static Assertion assertion() {
		Assertion droolsRule = new Assertion();
		droolsRule.setUuid(UUID.randomUUID());
		return droolsRule;
	}
}
