package org.ihtsdo.rvf.core.service.duck;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;

import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.ReleaseAcquisitionService;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A run can pin the assertion packs it executes, so a report can be reproduced after the
 * deployment's own pins have moved on. Each run is a full validation of one release, and the
 * reports must differ in both the packs they name and the findings.
 *
 * <p>A run pins {@code name@version} only, resolved through a configured channel: anything a
 * caller could use to point the server at a location of its own is refused.
 */
class DuckPerRunPinsTest {

	private static final String UUID_BASE = "11111111-1111-1111-1111-111111111111";
	private static final String UUID_PACK_ONE = "22222222-2222-2222-2222-222222222222";
	private static final String UUID_PACK_TWO = "33333333-3333-3333-3333-333333333333";

	private static final String MODULE_ONE = "900000000000207008";
	private static final String MODULE_TWO = "900000000000012004";

	/** Pack name and version, each served from {@code /<name>.sql}. */
	private static final List<String[]> OFFERED = List.of(
			new String[] {"pack-one", "2026.09.01"}, new String[] {"pack-two", "2026.09.02"},
			new String[] {"pack-1", "1"}, new String[] {"pack-2", "1"}, new String[] {"pack-3", "1"},
			new String[] {"pack-4", "1"}, new String[] {"pack-5", "1"});

	/** One assertion per store, reporting the prospective concepts of one module. */
	private static PackFixtures.Builder store(String uuid, String file, String module) {
		return PackFixtures.store()
				.table("concept_s", "id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT, "
						+ "definitionstatusid BIGINT")
				.assertion(uuid, file, "component-centric-validation",
						"insert into qa_result (run_id, assertion_id, concept_id, details, component_id, table_name) "
								+ "select 424242424242424242, 'rvfph_assertionuuid_', id, '" + file
								+ "', id, 'concept_s' from rvfph_prospective_.concept_s where moduleid = " + module);
	}

	@TempDir
	private Path root;

	private Path storeFile;
	private Path work;
	private Path prospective;
	private HttpServer server;
	/** What the index lists for a version, replaced by a test to serve another index. */
	private String index;
	private final AtomicInteger packFetches = new AtomicInteger();
	private ValidationReportService reportService;
	private WhitelistService whitelistService;

	@BeforeEach
	void setUp() throws Exception {
		storeFile = root.resolve("store.json");
		Files.writeString(storeFile, store(UUID_BASE, "base.sql", "0")
				.identity("international-fixture", "2026.07.27", "abc123").json());

		work = Files.createDirectories(root.resolve("work"));

		// Two concepts in one module and one in the other, so the packs see 2 and 1.
		prospective = root.resolve("prospective");
		writeRf2(prospective, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260831.txt", """
				id	effectiveTime	active	moduleId	definitionStatusId
				1	20260831	1	%1$s	900000000000074008
				2	20260831	1	%1$s	900000000000074008
				3	20260831	1	%2$s	900000000000074008
				""".formatted(MODULE_ONE, MODULE_TWO));

		// Every address, so 127.0.0.2 reaches it too: a second host for the channel host check.
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/", exchange -> {
			String path = exchange.getRequestURI().getPath().substring(1);
			String text;
			if (path.equals("index.json")) {
				text = index;
			} else {
				packFetches.incrementAndGet();
				text = packBody(path);
			}
			byte[] body = text.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		index = index(base());

		reportService = mock(ValidationReportService.class);
		whitelistService = mock(WhitelistService.class);
		when(whitelistService.isWhitelistDisabled()).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	private String base() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	private static String packBody(String file) {
		return file.equals("pack-two.sql")
				? store(UUID_PACK_TWO, file, MODULE_TWO).json()
				: store(UUID_PACK_ONE, file, MODULE_ONE).json();
	}

	/** The channel's index: every offered version, at {@code location}. */
	private static String index(String location) {
		List<String> versions = new ArrayList<>();
		for (String[] offered : OFFERED) {
			String file = offered[0] + ".sql";
			versions.add("{\"version\": \"" + offered[1] + "\", \"uri\": \"" + location + "/" + file
					+ "\", \"sha256\": \"" + PackFixtures.sha256Hex(packBody(file)) + "\", \"pack\": \"" + offered[0] + "\"}");
		}
		return "{\"channel\": \"example\", \"versions\": [" + String.join(", ", versions) + "]}";
	}

	@Test
	void twoRunsOfOneReleaseWithDifferentPinsReportDifferentPacksAndFindings() {
		ValidationReport first = runWith(List.of("pack-one@2026.09.01"));
		ValidationReport second = runWith(List.of("pack-two@2026.09.02"));

		assertEquals(List.of("international-fixture", "pack-one"), packNames(first));
		assertEquals(List.of("international-fixture", "pack-two"), packNames(second));
		assertEquals("abc123", first.getAssertionPacks().get(0).corpusRef());

		assertEquals(2L, failureCount(first, UUID_PACK_ONE), "pack-one counts its module's two concepts");
		assertEquals(1L, failureCount(second, UUID_PACK_TWO), "pack-two counts its module's one concept");
		assertThrows(AssertionError.class, () -> failureCount(first, UUID_PACK_TWO),
				"the first run must not have executed the other pin's assertion");
		assertThrows(AssertionError.class, () -> failureCount(second, UUID_PACK_ONE));
	}

	@Test
	void thePinnedCorpusIsCachedByPinSetAndNotRebuiltPerRun() throws Exception {
		DuckAssertionService service = service();
		List<String> pins = List.of("pack-one@2026.09.01");

		DuckAssertionService.Corpus once = service.corpusFor(pins);
		assertSame(once, service.corpusFor(pins), "the same pins must not re-fetch and re-merge");
		assertEquals(1, packFetches.get());

		DuckAssertionService.Corpus other = service.corpusFor(List.of("pack-two@2026.09.02"));
		assertNotSame(once, other);
		assertEquals(2, packFetches.get());
	}

	@Test
	void theCacheIsBounded() throws Exception {
		DuckAssertionService service = service();
		for (int i = 1; i <= 5; i++) {
			service.corpusFor(List.of("pack-" + i + "@1"));
		}
		assertEquals(5, packFetches.get());

		service.corpusFor(List.of("pack-5@1"));
		assertEquals(5, packFetches.get(), "a recent pin set is still cached");
		service.corpusFor(List.of("pack-1@1"));
		assertEquals(6, packFetches.get(), "the least recently used of five was evicted");
	}

	@Test
	void aRunThatPinsNothingGetsWhatTheDeploymentIsServing() throws Exception {
		DuckAssertionService service = service();
		assertSame(service.currentCorpus(), service.corpusFor(List.of()));
		assertSame(service.currentCorpus(), service.corpusFor(null));
		assertEquals(0, packFetches.get());
	}

	@Test
	void aRunMayNotPinALocationADigestOrACredential() throws Exception {
		DuckAssertionService service = service();
		String body = packBody("pack-two.sql");
		Path packFile = Files.writeString(root.resolve("pack-two.json"), body);
		List<String> refused = List.of(
				"name=pack-two;version=1;uri=" + packFile.toUri() + ";sha256=" + PackFixtures.sha256Hex(body),
				"name=pack-one;version=1;uri=" + base() + "/pack-one.sql;sha256=" + PackFixtures.sha256Hex(packBody("pack-one.sql")),
				"pack-one@2026.09.01;authHeader=Bearer secret",
				packFile.toUri().toString(),
				"uri=" + base() + "/pack-one.sql");
		for (String pin : refused) {
			IOException e = assertThrows(IOException.class, () -> service.corpusFor(List.of(pin)), pin);
			assertTrue(e.getMessage().contains("accepts only name@version"), e.getMessage());
			assertFalse(e.getMessage().contains("secret"), "a credential must not be echoed: " + e.getMessage());
		}
		assertEquals(0, packFetches.get(), "nothing a refused pin names is fetched");

		// The check the endpoints apply before a run is queued, which they answer with 400.
		assertEquals(refused.size(), AssertionPackChannels.invalidRunPins(refused).size());
		assertEquals(List.of(), AssertionPackChannels.invalidRunPins(List.of("pack-one@2026.09.01", " ")));
		assertTrue(AssertionPackChannels.runPinMessage(AssertionPackChannels.invalidRunPins(refused))
				.contains("authHeader=<redacted>"));
	}

	@Test
	void aVersionNoChannelOffersIsRefused() {
		DuckAssertionService service = service();
		IOException unknownVersion = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("pack-one@2099.01.01")));
		assertTrue(unknownVersion.getMessage().contains("offers no version"), unknownVersion.getMessage());
		IOException unknownPack = assertThrows(IOException.class,
				() -> service.corpusFor(List.of("someone-elses-pack@1")));
		assertTrue(unknownPack.getMessage().contains("no configured channel serves"), unknownPack.getMessage());
		assertEquals(0, packFetches.get());
	}

	@Test
	void anIndexPointingAtAnotherHostIsRefused() {
		// 127.0.0.2 reaches the same server, so only the host check can refuse it.
		index = index("http://127.0.0.2:" + server.getAddress().getPort());
		IOException e = assertThrows(IOException.class, () -> service().corpusFor(List.of("pack-one@2026.09.01")));
		assertTrue(e.getMessage().contains("not at its index's scheme and host"), e.getMessage());
		assertEquals(0, packFetches.get());

		// Unless the channel configuration lists that host.
		DuckAssertionService allowing = new DuckAssertionService(new DuckStoreLocator(storeFile.toString()),
				List.of(), List.of(channel() + ";allowedHosts=127.0.0.2"));
		assertEquals(1, assertDoesNotThrow(() -> allowing.corpusFor(List.of("pack-one@2026.09.01"))).packs().size());
	}

	@Test
	void aChannelVersionWhoseBytesAreNotItsDigestIsRefusedBeforeItIsParsed() {
		index = index(base()).replace(PackFixtures.sha256Hex(packBody("pack-one.sql")), "0".repeat(64));
		DuckAssertionService service = service();
		IOException e = assertThrows(IOException.class, () -> service.corpusFor(List.of("pack-one@2026.09.01")));
		assertTrue(e.getMessage().contains("refusing to load SQL that is not what was pinned"), e.getMessage());
		// A pinned run is not a deployment change.
		assertEquals(1, service.currentCorpus().store().assertions().size());
	}

	@Test
	void aCredentialNeverReachesALogLine() {
		AssertionPackChannels.Channel channel = AssertionPackChannels.parse(
				List.of(channel() + ";authHeader=Bearer secret")).get(0);
		assertFalse(channel.toString().contains("secret"), channel.toString());
		AssertionPackFetcher.Source source = new AssertionPackFetcher.Source("p", "1",
				java.net.URI.create(base()), "0".repeat(64), "Bearer secret");
		assertFalse(source.toString().contains("secret"), source.toString());
		ValidationRunConfig config = new ValidationRunConfig().addAssertionPacks(
				List.of("name=p;uri=" + base() + ";sha256=00;authHeader=Bearer secret"));
		assertFalse(config.toString().contains("secret"), config.toString());
	}

	private String channel() {
		return "name=example;index=" + base() + "/index.json;packs=" + String.join("|",
				OFFERED.stream().map(o -> o[0]).toList());
	}

	private static List<String> packNames(ValidationReport report) {
		return report.getAssertionPacks().stream().map(ValidationReport.AssertionPackRecord::name).toList();
	}

	private static long failureCount(ValidationReport report, String uuid) {
		UUID wanted = UUID.fromString(uuid);
		for (List<TestRunItem> bucket : List.of(report.getAssertionsFailed(), report.getAssertionsPassed(),
				report.getAssertionsWarning(), report.getAssertionsSkipped())) {
			for (TestRunItem candidate : bucket) {
				if (wanted.equals(candidate.getAssertionUuid())) {
					return candidate.getFailureCount();
				}
			}
		}
		throw new AssertionError(uuid + " was not reported at all");
	}

	private ValidationReport runWith(List<String> pins) {
		MysqlExecutionConfig config = new MysqlExecutionConfig(11L);
		config.setGroupNames(List.of("component-centric-validation"));
		config.setFailureExportMax(10);
		config.setAssertionPacks(pins);

		ValidationRunConfig runConfig = new ValidationRunConfig();
		runConfig.setRunId(config.getExecutionId());
		ValidationStatusReport status = new ValidationStatusReport(runConfig);
		new DuckDbValidationService(reportService, whitelistService, new ReleaseAcquisitionService(), service(),
				work.toString(), "empty-rf2-snapshot.zip", "qa_result", 0, "", "", 0, 1000, 1000)
				.runValidations(config, new DuckDbValidationService.ReleaseDirectories(prospective, null, null),
						"storage/", status);
		assertEquals(List.of(), status.getFailureMessages());
		return status.getResultReport();
	}

	private DuckAssertionService service() {
		return new DuckAssertionService(new DuckStoreLocator(storeFile.toString()), List.of(), List.of(channel()));
	}

	private static void writeRf2(Path releaseRoot, String relativePath, String content) throws IOException {
		Path file = releaseRoot.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}
}
