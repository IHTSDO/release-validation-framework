package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Finds the assertion store.
 *
 * <p>The store is bundled: the build runs {@code tools/duck-publisher} over the assertion
 * corpus it checks out and puts the result in the artefact as {@code duck/store.json}, so an
 * artefact always has one, built from the same corpus commit the MySQL engine imports.
 * {@code rvf.duck.store} overrides it with a store file.
 *
 * <p>The store is self-contained: statements, assertion metadata and group rules all come
 * from it, and its pack identity (name, version, digest, corpus commit) is what a report
 * records. The corpus checkout ({@code rvf.assertion.resource.local.path}) is not read in
 * this mode.
 */
@Component
@ConditionalOnDuckDbEngine
public class DuckStoreLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckStoreLocator.class);

	/** The store baked into the artefact, on the classpath. */
	static final String BUNDLED_STORE = "/duck/store.json";

	private final String configuredStore;

	public DuckStoreLocator(@Value("${rvf.duck.store:}") String configuredStore) {
		this.configuredStore = configuredStore;
	}

	/** Where the store came from, for logging. */
	public String description() {
		return isConfigured() ? configuredStore : "bundled " + BUNDLED_STORE;
	}

	private boolean isConfigured() {
		return configuredStore != null && !configuredStore.isBlank();
	}

	/**
	 * Reads the store.
	 *
	 * @throws IOException if neither a configured nor a bundled store can be read
	 */
	public DuckStore load() throws IOException {
		DuckStore store = isConfigured()
				? DuckStore.read(Path.of(configuredStore))
				: readBundled();
		LOGGER.info("Assertion store {}@{} ({}, corpus commit {}).",
				store.tree().path("pack").path("name").asString("?"),
				store.tree().path("pack").path("version").asString("?"),
				description(), store.corpusRef().isEmpty() ? "unknown" : store.corpusRef());
		return store;
	}

	private DuckStore readBundled() throws IOException {
		try (InputStream in = DuckStoreLocator.class.getResourceAsStream(BUNDLED_STORE)) {
			if (in == null) {
				throw new IOException("No assertion store: " + BUNDLED_STORE
						+ " is missing from the artefact and rvf.duck.store is not set.");
			}
			return DuckStore.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}
}
