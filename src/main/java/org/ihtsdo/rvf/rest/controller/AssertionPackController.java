package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.ihtsdo.rvf.core.service.duck.DuckAssertionService;
import org.ihtsdo.rvf.core.service.duck.DuckStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The assertion corpus the DuckDB engine executes: the store it was loaded from.
 *
 * <p>Registered only for {@code rvf.execution.engine=duckdb}; the MySQL engine has one
 * assertion database.
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
	@Operation(summary = "The assertion store the loaded corpus was read from",
			description = "Name, version, digest, corpus commit and assertion count of the store.")
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
		return ResponseEntity.ok(body);
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
