package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.rest.controller.AssertionPackController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link AssertionPackController} over a real {@link DuckAssertionService} and a store file. */
class AssertionPackControllerTest {

	private static final String UUID_BASE = "11111111-1111-1111-1111-111111111111";

	@TempDir
	private Path dir;

	@Test
	@SuppressWarnings("unchecked")
	void getReportsTheStoresIdentity() {
		Path store = TestStores.builder()
				.assertion(UUID_BASE, "base.sql", "select 1;", "Base", "component-centric-validation",
						"INSERT INTO qa_result SELECT 1")
				.pack("international", "2026.07.27", "abc123")
				.write(dir.resolve("store.json"));
		AssertionPackController controller = new AssertionPackController(
				new DuckAssertionService(new DuckStoreLocator(store.toString())));

		ResponseEntity<Map<String, Object>> response = controller.getPacks();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		Map<String, Object> body = response.getBody();
		assertEquals(1, body.get("assertions"));
		List<Map<String, Object>> packs = (List<Map<String, Object>>) body.get("packs");
		assertEquals(List.of("international"), packs.stream().map(p -> p.get("name")).toList());
		assertEquals("2026.07.27", packs.get(0).get("version"));
		assertEquals("abc123", packs.get(0).get("corpusRef"));
		assertEquals(1, packs.get(0).get("assertions"));
	}
}
