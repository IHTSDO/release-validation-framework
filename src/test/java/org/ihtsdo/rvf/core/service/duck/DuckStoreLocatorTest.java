package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckStoreLocatorTest {

	private static final String UUID_ONE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_TWO = "22222222-2222-2222-2222-222222222222";
	private static final String REF = "c3a6fb27b2193925be7bce94ec1c4b4b03c8764f";

	@TempDir
	Path dir;

	@Test
	void loadsTheBundledStoreWhenNothingIsConfigured() throws IOException {
		// A built artefact needs no configuration; this fails if the resource is not in the build.
		DuckStore store = new DuckStoreLocator("").load();
		assertTrue(store.assertions().size() > 300,
				"bundled store looks truncated: " + store.assertions().size() + " assertions");
	}

	@Test
	void aConfiguredStoreWinsOverTheBundledOne() throws IOException {
		Path store = TestStores.builder().pack("international", "1", REF)
				.assertion(UUID_ONE, "one.sql", "select 1 from concept_s;\n", "One", "component-centric-validation")
				.assertion(UUID_TWO, "two.sql", "select 2 from description_s;\n", "Two", "component-centric-validation")
				.write(dir.resolve("store.json"));
		assertEquals(2, new DuckStoreLocator(store.toString()).load().assertions().size());
	}

	@Test
	void describesWhereTheStoreCameFrom() {
		assertEquals("bundled /duck/store.json", new DuckStoreLocator("").description());
		assertEquals("/x/store.json", new DuckStoreLocator("/x/store.json").description());
		assertEquals("bundled /duck/store.json", new DuckStoreLocator("   ").description());
		assertEquals("bundled /duck/store.json", new DuckStoreLocator(null).description());
	}
}
