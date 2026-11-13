package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.ihtsdo.rvf.core.service.duck.AssertionPackChannels;
import org.ihtsdo.rvf.core.service.duck.DuckAssertionService;
import org.ihtsdo.rvf.core.service.duck.DuckStore;
import org.ihtsdo.rvf.core.service.duck.DuckStorePacks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The assertion corpus the DuckDB engine executes: what it was assembled from, and the
 * packs it can be extended with.
 *
 * <p>Registered only for {@code rvf.execution.engine=duckdb}; the MySQL engine has one
 * assertion database and no packs.
 */
@RestController
@RequestMapping("/assertions/packs")
@Tag(name = "Assertion packs")
@ConditionalOnDuckDbEngine
public class AssertionPackController {

	private final DuckAssertionService duckAssertionService;

	public AssertionPackController(DuckAssertionService duckAssertionService) {
		this.duckAssertionService = duckAssertionService;
	}

	@GetMapping
	@Operation(summary = "The assertion packs the loaded corpus was assembled from",
			description = "Name, version, digest, corpus commit and assertion count of each pack, the base store "
					+ "included, and whether the configured packs differ from the loaded ones.")
	public ResponseEntity<Map<String, Object>> getPacks() {
		DuckAssertionService.Corpus corpus = duckAssertionService.currentCorpus();
		List<Map<String, Object>> packs;
		try {
			packs = duckAssertionService.loadedProvenance().stream().map(AssertionPackController::describe).toList();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
					"message", "The loaded assertion store cannot state its provenance",
					"reason", String.valueOf(e.getMessage())));
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("assertions", corpus.source().findAll().size());
		body.put("packs", packs);
		List<String> pending = duckAssertionService.pendingPackChanges();
		body.put("pendingRefresh", !pending.isEmpty());
		body.put("pending", pending);
		return ResponseEntity.ok(body);
	}

	@PostMapping("refresh")
	@Operation(summary = "Fetch the configured assertion packs and replace the corpus",
			description = "Every pack in rvf.assertion.packs is fetched, checked against its pinned digest, merged "
					+ "with the base store and executed against empty tables before the corpus is replaced. "
					+ "On any failure the current corpus stays in use. 409 lists every merge conflict.")
	public ResponseEntity<Map<String, Object>> refreshPacks() {
		try {
			return ResponseEntity.ok(Map.of("loaded", duckAssertionService.refreshConfiguredPacks()));
		} catch (DuckStorePacks.ConflictException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
					"message", "the configured packs cannot be merged; the corpus is unchanged",
					"conflicts", e.getConflicts()));
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
					"message", "could not load the configured packs; the corpus is unchanged",
					"reason", String.valueOf(e.getMessage())));
		}
	}

	@GetMapping("channels")
	@Operation(summary = "The configured pack channels and the versions each offers",
			description = "Read-only: a version listed here can be pinned for a run as name@version; it does not "
					+ "change what the deployment uses by default.")
	public ResponseEntity<Map<String, Object>> channels(
			@RequestParam(value = "versions", defaultValue = "true") boolean withVersions) {
		AssertionPackChannels channels = duckAssertionService.channels();
		List<Map<String, Object>> described = new ArrayList<>();
		for (AssertionPackChannels.Channel channel : channels.channels()) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("name", channel.name());
			entry.put("index", channel.index().toString());
			entry.put("packs", channel.packs());
			if (withVersions) {
				try {
					List<Map<String, Object>> versions = new ArrayList<>();
					for (AssertionPackChannels.Version version : channels.versions(channel)) {
						Map<String, Object> v = new LinkedHashMap<>();
						v.put("version", version.version());
						v.put("sha256", version.sha256());
						v.put("assertions", version.assertions());
						v.put("requires", version.requires());
						v.put("published", version.published());
						v.put("draft", version.draft());
						v.put("pin", channel.packs().get(0) + "@" + version.version());
						versions.add(v);
					}
					entry.put("versions", versions);
				} catch (IOException e) {
					// One unreachable index does not hide the channels that answered.
					entry.put("error", "could not read the index: " + e.getMessage());
				}
			}
			described.add(entry);
		}
		return ResponseEntity.ok(Map.of(
				"channels", described,
				"loaded", duckAssertionService.loadedPacks().stream().map(DuckStorePacks.Pack::label).toList()));
	}

	private static Map<String, Object> describe(DuckStore.PackRecord pack) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("name", pack.name());
		entry.put("version", pack.version());
		entry.put("digest", pack.digest());
		entry.put("corpusRef", pack.corpusRef());
		entry.put("assertions", pack.assertions());
		return entry;
	}
}
