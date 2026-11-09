package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.rest.controller.AssertionPackController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link AssertionPackController} over a real {@link DuckAssertionService} and file packs. */
class AssertionPackControllerTest {

	private static final String UUID_BASE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_PACK = "22222222-2222-2222-2222-222222222222";
	private static final String STATEMENT = "INSERT INTO qa_result (run_id, assertion_id, concept_id) "
			+ "SELECT 424242424242424242, 'rvfph_assertionuuid_', c.id FROM concept_s c";

	@TempDir
	private Path dir;

	private static PackFixtures.Builder store(String uuid, String file, String macro) {
		return PackFixtures.store()
				.table("concept_s", "id BIGINT")
				.port(macro)
				.assertion(uuid, file, "component-centric-validation", STATEMENT);
	}

	private AssertionPackController controller(String packJson) throws IOException {
		Path base = Files.writeString(dir.resolve("store.json"),
				store(UUID_BASE, "base.sql", "CREATE OR REPLACE MACRO m(x) AS x")
						.identity("international", "2026.07.27", "abc123").json());
		List<String> specs = List.of();
		if (packJson != null) {
			Path pack = Files.writeString(dir.resolve("pack.json"), packJson);
			specs = List.of("name=extension-pack;version=2026.09.01;uri=" + pack.toUri()
					+ ";sha256=" + PackFixtures.sha256Hex(packJson));
		}
		return new AssertionPackController(new DuckAssertionService(
				new DuckStoreLocator(base.toString()), specs, List.of()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getReportsProvenanceAndPendingState() throws Exception {
		AssertionPackController controller = controller(store(UUID_PACK, "pack.sql",
				"CREATE OR REPLACE MACRO m(x) AS x").json());

		Map<String, Object> body = controller.getPacks().getBody();

		assertEquals(2, body.get("assertions"));
		List<Map<String, Object>> packs = (List<Map<String, Object>>) body.get("packs");
		assertEquals(List.of("international", "extension-pack"), packs.stream().map(p -> p.get("name")).toList());
		assertEquals("abc123", packs.get(0).get("corpusRef"));
		assertEquals(false, body.get("pendingRefresh"));
		assertEquals(List.of(), body.get("pending"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void aConflictingRefreshAnswers409WithEveryConflict() throws Exception {
		AssertionPackController controller = controller(store(UUID_PACK, "pack.sql",
				"CREATE OR REPLACE MACRO m(x) AS 1").transpiler("31.0.0").json());

		ResponseEntity<Map<String, Object>> response = controller.refreshPacks();

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		List<String> conflicts = (List<String>) response.getBody().get("conflicts");
		assertEquals(2, conflicts.size(), conflicts.toString());
	}

	@Test
	void aPackThatCannotBeLoadedAnswers502() throws Exception {
		AssertionPackController controller = controller(store(UUID_PACK, "pack.sql",
				"CREATE OR REPLACE MACRO m(x) AS x").json());
		Files.writeString(dir.resolve("pack.json"), "changed after pinning");

		ResponseEntity<Map<String, Object>> response = controller.refreshPacks();

		assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
		assertTrue(String.valueOf(response.getBody().get("reason")).contains("pinned"), response.getBody().toString());
	}

	@Test
	void aSuccessfulRefreshSaysWhatIsLoaded() throws Exception {
		AssertionPackController controller = controller(null);

		ResponseEntity<Map<String, Object>> response = controller.refreshPacks();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(String.valueOf(response.getBody().get("loaded")).contains("base store only"),
				response.getBody().toString());
	}
}
