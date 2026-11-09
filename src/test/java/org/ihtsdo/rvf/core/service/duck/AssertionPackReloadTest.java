package org.ihtsdo.rvf.core.service.duck;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fetching a pack, verifying its digest and swapping the corpus - or, on any failure, leaving
 * the old corpus serving. Served by a real {@link HttpServer}, so redirects, HTTP errors and
 * wrong bodies go through the actual client.
 */
class AssertionPackReloadTest {

	private static final String UUID_BASE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_PACK = "22222222-2222-2222-2222-222222222222";

	private static final String MACRO = "CREATE OR REPLACE MACRO m(x) AS x";
	private static final String MACRO_CONFLICTING = "CREATE OR REPLACE MACRO m(x) AS 1";

	private static final String STATEMENT = "INSERT INTO qa_result (run_id, assertion_id, concept_id) "
			+ "SELECT 424242424242424242, 'rvfph_assertionuuid_', c.id FROM concept_s c";

	private HttpServer server;
	private byte[] served = new byte[0];
	private byte[] servedIndex = new byte[0];
	private int status = 200;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/pack.json", exchange -> {
			exchange.sendResponseHeaders(status, status == 200 ? served.length : -1);
			if (status == 200) {
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(served);
				}
			}
			exchange.close();
		});
		// The channel index, on the pack's own host: an index may only list URLs there.
		server.createContext("/index.json", exchange -> {
			exchange.sendResponseHeaders(200, servedIndex.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(servedIndex);
			}
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private URI uri() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/pack.json");
	}

	private void serve(String body) {
		served = body.getBytes(StandardCharsets.UTF_8);
	}

	private void serveIndex(String body) {
		servedIndex = body.getBytes(StandardCharsets.UTF_8);
	}

	/** The channel that serves extension-pack, with its index on the test server. */
	private String channel() {
		return "name=extension;index=http://127.0.0.1:" + server.getAddress().getPort() + "/index.json;packs=extension-pack";
	}

	/** A store with one assertion that the default grouping puts in component-centric-validation. */
	private static PackFixtures.Builder store(String uuid, String file, String macro, String statement) {
		return PackFixtures.store()
				.table("concept_s", "id BIGINT")
				.port(macro)
				.assertion(uuid, file, "component-centric-validation", statement);
	}

	private static String pack(String file, String macro) {
		return store(UUID_PACK, file, macro, STATEMENT).json();
	}

	/** The base store, with the identity a merge needs. */
	private static void writeBase(Path dir) throws IOException {
		Files.writeString(dir.resolve("store.json"), store(UUID_BASE, "base.sql", MACRO, STATEMENT)
				.identity("international", "2026.07.27", "abc123").json());
	}

	private DuckAssertionService serviceWith(Path dir, String packSpec) throws IOException {
		return serviceWith(dir, packSpec, null);
	}

	/** As above, plus a channel a pin may be named from. */
	private DuckAssertionService serviceWith(Path dir, String packSpec, String channelSpec) throws IOException {
		writeBase(dir);
		DuckStoreLocator locator = new DuckStoreLocator(dir.resolve("store.json").toString());
		return new DuckAssertionService(locator,
				packSpec == null ? List.of() : List.of(packSpec),
				channelSpec == null ? List.of() : List.of(channelSpec));
	}

	/** A channel index: version, uri and the byte digest. */
	private String index(String channel, String version, String digest) {
		return """
				{"formatVersion": 1,
				 "channel": "%s",
				 "versions": [
				  {"version": "%s", "uri": "%s", "sha256": "%s", "assertions": 1}
				 ]}
				""".formatted(channel, version, uri(), digest);
	}

	private String spec(String digest) {
		return "name=extension-pack;version=2026.09.01;uri=" + uri() + ";sha256=" + digest;
	}

	private static String sha256(String body) {
		return PackFixtures.sha256Hex(body);
	}

	@Test
	void aDeploymentWithNoPacksStillReportsWhatItIsRunning(@TempDir Path dir) throws Exception {
		// Provenance comes from the store, so the normal no-packs case still names its assertions.
		DuckAssertionService service = serviceWith(dir, null);

		assertEquals(1, service.findAll().size(), "first use loads the base store");
		List<DuckStore.PackRecord> provenance = service.loadedProvenance();
		assertEquals(1, provenance.size(), "the base is a pack of its own");
		assertEquals("international", provenance.get(0).name());
		assertEquals("2026.07.27", provenance.get(0).version());
		assertEquals("abc123", provenance.get(0).corpusRef());
		assertTrue(provenance.get(0).digest().startsWith("sha256:"), provenance.get(0).digest());
		assertTrue(service.loadedPacks().isEmpty(), "and no packs were configured");
	}

	@Test
	void provenanceBeforeAnythingIsLoadedIsEmptyRatherThanAFetch(@TempDir Path dir) throws Exception {
		// Called from inspection endpoints and error paths, so it must never load.
		DuckAssertionService service = serviceWith(dir, null);
		assertTrue(service.loadedProvenance().isEmpty());
		assertTrue(service.loadedPacks().isEmpty());
	}

	@Test
	void aVerifiedPackIsMergedAndSwappedIn(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		// Configured packs load on first use, not only after a refresh.
		assertEquals(2, service.findAll().size(), "configured packs load on first use");

		String description = service.refreshConfiguredPacks();

		assertEquals(2, service.findAll().size(), "base plus pack");
		assertTrue(description.contains("extension-pack@2026.09.01"), description);
		assertEquals(1, service.loadedPacks().size());
		assertEquals("extension-pack", service.loadedPacks().get(0).name());
		assertTrue(service.loadedPacks().get(0).digest().startsWith("sha256:"));
		assertTrue(service.storedAssertion(UUID_PACK).isPresent());
		assertEquals(List.of("international", "extension-pack"),
				service.loadedProvenance().stream().map(DuckStore.PackRecord::name).toList());
		assertEquals(2, service.getAssertionGroupByName("component-centric-validation").getAssertions().size(),
				"the pack's assertion is grouped by its own grouping files");
	}

	@Test
	void aBodyThatIsNotWhatWasPinnedIsRefusedAndNothingIsServed(@TempDir Path dir) throws Exception {
		serve(pack("pack.sql", MACRO));
		String wrong = sha256(pack("something-else.sql", MACRO));
		DuckAssertionService service = serviceWith(dir, spec(wrong));

		IOException e = assertThrows(IOException.class, service::refreshConfiguredPacks);
		assertTrue(e.getMessage().contains("pinned"), e.getMessage());
		// No silent fallback to the base store: fewer assertions would read as a clean pass.
		assertThrows(UncheckedIOException.class, service::findAll);
		assertTrue(service.loadedPacks().isEmpty());
	}

	@Test
	void anUnreachablePackLeavesTheCorpusAlone(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));
		service.refreshConfiguredPacks();
		assertEquals(2, service.findAll().size(), "loaded once, successfully");

		status = 404;
		IOException e = assertThrows(IOException.class, service::refreshConfiguredPacks);
		assertTrue(e.getMessage().contains("404"), e.getMessage());
		assertEquals(2, service.findAll().size(), "the last good corpus keeps serving");
		assertEquals(1, service.loadedPacks().size());
	}

	@Test
	void aRunKeepsTheCorpusItCapturedAcrossAReload(@TempDir Path dir) throws Exception {
		DuckAssertionService service = serviceWith(dir, null);
		DuckAssertionService.Corpus captured = service.currentCorpus();

		String pack = pack("pack.sql", MACRO);
		serve(pack);
		service.reload(new AssertionPackFetcher().fetch(List.of(
				new AssertionPackFetcher.Source("extension-pack", "1", uri(), sha256(pack), null))));

		assertEquals(2, service.currentCorpus().source().findAll().size());
		assertEquals(1, captured.source().findAll().size(), "the swap does not touch a captured corpus");
		assertEquals(1, captured.store().assertions().size());
	}

	@Test
	void aPackBehindARedirectIsFetchedAskingForTheAssetOnly(@TempDir Path dir) throws Exception {
		// Release assets are served by redirect, and an API picks JSON metadata over the asset
		// when Accept allows it.
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		StringBuilder sawAccept = new StringBuilder();
		server.createContext("/asset", exchange -> {
			sawAccept.append(exchange.getRequestHeaders().getFirst("Accept"));
			exchange.getResponseHeaders().add("Location", uri().toString());
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		URI asset = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/asset");

		DuckAssertionService service = serviceWith(dir,
				"name=extension-pack;version=2026.09.01;uri=" + asset + ";sha256=" + sha256(pack));

		assertEquals(2, service.findAll().size(), "the redirect was followed");
		assertEquals(1, service.loadedPacks().size());
		assertEquals("application/octet-stream", sawAccept.toString());
	}

	@Test
	void aPackThatCannotBeMergedIsRefusedAndNothingChanges(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO_CONFLICTING);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		var conflict = assertThrows(DuckStorePacks.ConflictException.class, service::refreshConfiguredPacks);
		assertTrue(conflict.getMessage().contains("redefines"), conflict.getMessage());
		assertThrows(DuckStorePacks.ConflictException.class, service::findAll);
		assertTrue(service.loadedPacks().isEmpty());
	}

	@Test
	void aPackWithoutGroupingIsRefused(@TempDir Path dir) throws Exception {
		String pack = store(UUID_PACK, "pack.sql", MACRO, STATEMENT).noGrouping().json();
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		var conflict = assertThrows(DuckStorePacks.ConflictException.class, service::refreshConfiguredPacks);
		assertTrue(conflict.getMessage().contains("extension-pack@2026.09.01"), conflict.getMessage());
	}

	@Test
	void aBaseStoreWithoutIdentityCannotBeMergedWithPacks(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));
		Files.writeString(dir.resolve("store.json"), store(UUID_BASE, "base.sql", MACRO, STATEMENT).json());

		IOException e = assertThrows(IOException.class, service::refreshConfiguredPacks);
		assertTrue(e.getMessage().contains("no pack identity"), e.getMessage());
	}

	@Test
	void anUnpinnedPackIsRejectedByConfigurationNotAtFetchTime(@TempDir Path dir) throws Exception {
		DuckAssertionService service = serviceWith(dir, "name=extension-pack;version=1;uri=" + uri());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, service::configuredPackSources);
		assertTrue(e.getMessage().contains("pinned"), e.getMessage());
	}

	@Test
	void aPackThatDoesNotExecuteIsRefusedAndNothingChanges(@TempDir Path dir) throws Exception {
		// Digest and merge both pass, but the SQL calls a macro nothing defines.
		String pack = store(UUID_PACK, "pack.sql", MACRO,
				STATEMENT.replace("c.id FROM concept_s c", "no_such_macro(c.id) FROM concept_s c")).json();
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		IOException e = assertThrows(IOException.class, service::refreshConfiguredPacks);
		assertTrue(e.getMessage().contains("does not execute"), e.getMessage());
		assertTrue(e.getMessage().contains("pack.sql"), e.getMessage());
		assertThrows(UncheckedIOException.class, service::findAll);
		assertTrue(service.loadedPacks().isEmpty());
	}

	@Test
	void aPackNeedingAReleaseTheCheckCannotSupplyIsNotAFailure(@TempDir Path dir) throws Exception {
		// The binder skips an assertion that needs a previous release, as a run without one does.
		String pack = store(UUID_PACK, "pack.sql", MACRO,
				STATEMENT.replace("FROM concept_s c", "FROM rvfph_previous_.concept_s c")).json();
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		service.refreshConfiguredPacks();
		assertEquals(2, service.findAll().size(), "loaded, with the skip left to the run");
	}

	@Test
	void noConfiguredPacksMeansTheBaseStoreAndThatIsNotAnError(@TempDir Path dir) throws Exception {
		DuckAssertionService service = serviceWith(dir, null);
		String description = service.refreshConfiguredPacks();
		assertTrue(description.contains("base store only"), description);
		assertEquals(1, service.findAll().size());
	}

	@Test
	void aFilePackIsFetched(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		Path packFile = dir.resolve("extension-pack.json");
		Files.writeString(packFile, pack);
		DuckAssertionService service = serviceWith(dir,
				"name=extension-pack;version=1;uri=" + packFile.toUri() + ";sha256=" + sha256(pack));

		service.refreshConfiguredPacks();
		assertEquals(2, service.findAll().size());
	}

	@Test
	void pendingChangesNameWhatARefreshWouldChange(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)));

		assertEquals(List.of("extension-pack 2026.09.01 is configured and not loaded"),
				service.pendingPackChanges());
		service.refreshConfiguredPacks();
		assertEquals(List.of(), service.pendingPackChanges());

		DuckAssertionService other = serviceWith(dir, spec("0".repeat(64)));
		other.reload(service.loadedPacks());
		List<String> pending = other.pendingPackChanges();
		assertEquals(1, pending.size(), pending.toString());
		assertTrue(pending.get(0).contains("is loaded at sha256:" + sha256(pack)), pending.get(0));

		DuckAssertionService unconfigured = serviceWith(dir, null);
		unconfigured.reload(service.loadedPacks());
		assertEquals(List.of("extension-pack is loaded and no longer configured"),
				unconfigured.pendingPackChanges());
	}

	/** The digest of a name@version pin comes from the channel's index, and is still verified. */
	@Test
	void aPinMayNameAVersionAndTheDigestComesFromTheIndex(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		serveIndex(index("extension", "2026.09.02", "sha256:" + sha256(pack)));
		DuckAssertionService service = serviceWith(dir, null, channel());

		DuckAssertionService.Corpus corpus = service.corpusFor(List.of("extension-pack@2026.09.02"));

		assertEquals(2, corpus.source().findAll().size(), "base plus the named version");
		assertEquals(1, corpus.packs().size());
		assertEquals("2026.09.02", corpus.packs().get(0).version());
		assertEquals("sha256:" + sha256(pack), corpus.packs().get(0).digest());
	}

	@Test
	void anIndexDigestThatDoesNotMatchIsStillRefused(@TempDir Path dir) throws Exception {
		serve(pack("pack.sql", MACRO));
		serveIndex(index("extension", "2026.09.02", "sha256:" + sha256(pack("something-else.sql", MACRO))));
		DuckAssertionService service = serviceWith(dir, null, channel());

		IOException e = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("extension-pack@2026.09.02")));
		assertTrue(e.getMessage().contains("pinned"), e.getMessage());
	}

	@Test
	void anUnknownVersionSaysWhichExist(@TempDir Path dir) throws Exception {
		serveIndex(index("extension", "2026.09.02", "sha256:whatever"));
		DuckAssertionService service = serviceWith(dir, null, channel());

		IOException e = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("extension-pack@2026.09.09")));
		assertTrue(e.getMessage().contains("2026.09.02"), "says what it has: " + e.getMessage());
	}

	@Test
	void aPackNoChannelServesIsRefused(@TempDir Path dir) throws Exception {
		// The channel list is the trust boundary for shorthand pins.
		serveIndex(index("extension", "2026.09.02", "sha256:whatever"));
		DuckAssertionService service = serviceWith(dir, null, channel());

		IOException e = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("unlisted-pack@1.0.0")));
		assertTrue(e.getMessage().contains("no configured channel"), e.getMessage());
	}

	@Test
	void anIndexNamingAnotherChannelIsRefused(@TempDir Path dir) throws Exception {
		serveIndex(index("another-channel", "2026.09.02", "sha256:x"));
		DuckAssertionService service = serviceWith(dir, null, channel());

		IOException e = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("extension-pack@2026.09.02")));
		assertTrue(e.getMessage().contains("declares itself"), e.getMessage());
	}

	@Test
	void aConfiguredFullSpecNeverTouchesTheIndex(@TempDir Path dir) throws Exception {
		String pack = pack("pack.sql", MACRO);
		serve(pack);
		DuckAssertionService service = serviceWith(dir, spec(sha256(pack)),
				"name=extension;index=file:///nonexistent-index.json;packs=extension-pack");

		service.refreshConfiguredPacks();

		assertEquals(2, service.findAll().size());
	}
}
