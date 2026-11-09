package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.ihtsdo.rvf.config.ExecutionEngine;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.AssertionTest;
import org.ihtsdo.rvf.core.data.model.Test;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

/**
 * {@link AssertionService} over the assertion store, so the assertion endpoints and the
 * validation submission endpoints work without the assertion tables.
 *
 * <p>The corpus is the bundled store (or {@code rvf.duck.store}) merged with the assertion
 * packs {@code rvf.assertion.packs} pins; see {@code docs/duckdb-engine.md}. This class owns
 * the corpus a validation executes: {@link DuckDbValidationService} asks it for one at the
 * start of a run and keeps it for the whole run.
 *
 * <p>The store is the published corpus, not a database, so:
 * <ul>
 * <li>mutation ({@code create}, {@code save}, {@code addTest}, group edits) throws;
 * <li>lookup by numeric id returns nothing. The numeric ids are assigned by the MySQL import;
 *     a synthesised one would address a different assertion on each engine;
 * <li>{@code getAssertionTests} returns nothing: the AssertionTest and Test rows are a MySQL
 *     normalisation of the statements the store holds.
 * </ul>
 * None of these is on the path a validation takes, which selects groups by name.
 */
@Service
@ConditionalOnDuckDbEngine
public class DuckAssertionService implements AssertionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckAssertionService.class);

	/** How many per-run pin sets keep their corpus cached; a store is under 1 MB. */
	private static final int PINNED_CORPORA = 4;

	private final DuckStoreLocator storeLocator;

	/** {@code rvf.assertion.packs}: the packs merged with the base store by default. */
	private final List<String> packSpecs;

	/** {@code rvf.assertion.channels}: the indexes a pack may be named from by version alone. */
	private final AssertionPackChannels channels;

	private final AssertionPackFetcher fetcher;

	/** What a run uses by default. Replaced as a whole by {@link #reload}. */
	private volatile Corpus current;

	/** Corpora built for per-run pins, keyed by the resolved pin set, least recently used evicted. */
	private final Map<String, Corpus> pinnedCorpora = Collections.synchronizedMap(
			new LinkedHashMap<>(8, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, Corpus> eldest) {
					return size() > PINNED_CORPORA;
				}
			});

	@Autowired
	public DuckAssertionService(DuckStoreLocator storeLocator,
			@Value("${rvf.assertion.packs:}") List<String> packSpecs,
			@Value("${rvf.assertion.channels:}") List<String> channelSpecs) {
		this(storeLocator, packSpecs, channelSpecs, new AssertionPackFetcher());
	}

	DuckAssertionService(DuckStoreLocator storeLocator, List<String> packSpecs, List<String> channelSpecs,
			AssertionPackFetcher fetcher) {
		this.storeLocator = storeLocator;
		this.packSpecs = packSpecs == null ? List.of() : packSpecs.stream().filter(s -> !s.isBlank()).toList();
		this.fetcher = fetcher;
		this.channels = new AssertionPackChannels(AssertionPackChannels.parse(channelSpecs), fetcher);
	}

	/** A corpus with no locator behind it, for tests. */
	DuckAssertionService(Corpus corpus) {
		this.storeLocator = null;
		this.packSpecs = List.of();
		this.fetcher = new AssertionPackFetcher();
		this.channels = new AssertionPackChannels(List.of(), fetcher);
		this.current = corpus;
	}

	/**
	 * A corpus a run executes: the store, its assertions and groups, and the packs merged into
	 * it (empty when the base store runs alone).
	 */
	public record Corpus(DuckStore store, DuckAssertionSource source, List<DuckStorePacks.Pack> packs) {
	}

	/**
	 * Loads the corpus when the application is ready, so the store's identity, and any pack
	 * that will not load, are in the startup log. A failure is logged, not fatal: the service
	 * starts, and the next use tries again and reports the failure on the run that needed it.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void loadAtStartup() {
		try {
			Corpus corpus = currentCorpus();
			LOGGER.info("DuckDB assertion corpus: {} assertions in {} groups",
					corpus.source().findAll().size(), corpus.source().populatedGroupNames().size());
		} catch (RuntimeException e) {
			LOGGER.error("The DuckDB assertion corpus could not be loaded at startup; it will be "
					+ "loaded again when first used", e);
		}
	}

	/**
	 * The corpus a run uses when it pins no packs: the base store merged with the configured
	 * packs. Loaded on first use.
	 *
	 * <p>Configured packs that cannot be loaded fail the load rather than falling back to the
	 * base store: a run over fewer assertions than configured would read as a clean pass.
	 */
	public Corpus currentCorpus() {
		Corpus corpus = current;
		if (corpus != null) {
			return corpus;
		}
		synchronized (this) {
			if (current == null) {
				try {
					List<DuckStorePacks.Pack> packs = packSpecs.isEmpty()
							? List.of() : fetcher.fetch(configuredPackSources());
					current = build(packs);
				} catch (IOException e) {
					throw new UncheckedIOException("The DuckDB assertion corpus could not be loaded from "
							+ storeLocator.description()
							+ (packSpecs.isEmpty() ? "" : " with the packs in rvf.assertion.packs"), e);
				}
			}
			return current;
		}
	}

	/**
	 * The corpus for one run's pins, or {@link #currentCorpus()} when it names none.
	 *
	 * <p>A report is reproducible only if the assertions that produced it can be named and
	 * obtained again; a deployment's own pins move. A run may pin only {@code name@version}
	 * of a pack a configured channel offers (see {@link AssertionPackChannels#resolveRunPins}).
	 * Built exactly as a reload is (fetch, verify each digest, merge, prove it executes), and
	 * it does not change what other runs use.
	 *
	 * @throws IOException if a pin is not {@code name@version} of a pack a channel offers, or
	 *                     the corpus cannot be built
	 */
	public Corpus corpusFor(List<String> pinSpecs) throws IOException {
		if (pinSpecs == null || pinSpecs.stream().allMatch(String::isBlank)) {
			return currentCorpus();
		}
		List<String> specs = channels.resolveRunPins(pinSpecs);
		String key = String.join("\n", new TreeSet<>(specs));
		Corpus cached = pinnedCorpora.get(key);
		if (cached != null) {
			return cached;
		}
		List<DuckStorePacks.Pack> pinned = fetcher.fetch(sourcesFrom(specs));
		Corpus corpus = build(pinned);
		pinnedCorpora.put(key, corpus);
		LOGGER.info("DuckDB corpus for a run's pins: {} assertions from {}",
				corpus.source().findAll().size(), pinned.stream().map(DuckStorePacks.Pack::label).toList());
		return corpus;
	}

	/**
	 * Replaces the default corpus with the base store merged with these packs.
	 *
	 * <p>The new corpus is built and proven executable before it is published with one
	 * volatile write, so a failure leaves the current corpus serving. A run already in
	 * progress keeps the corpus it started with.
	 *
	 * @return a description of what is now loaded
	 */
	public synchronized String reload(List<DuckStorePacks.Pack> incoming) throws IOException {
		Corpus candidate = build(incoming);
		current = candidate;
		String description = candidate.source().findAll().size() + " assertions in "
				+ candidate.source().populatedGroupNames().size() + " groups from "
				+ (incoming.isEmpty() ? "the base store only"
						: incoming.size() + " pack(s): " + incoming.stream().map(DuckStorePacks.Pack::label).toList());
		LOGGER.info("DuckDB assertion corpus reloaded: {}", description);
		return description;
	}

	/** Fetches the configured packs and {@link #reload reloads}. */
	public String refreshConfiguredPacks() throws IOException {
		List<AssertionPackFetcher.Source> sources = configuredPackSources();
		return reload(sources.isEmpty() ? List.of() : fetcher.fetch(sources));
	}

	/**
	 * The base store, merged with {@code packs} if there are any, and its source.
	 *
	 * <p>The base is always included: a pack set that replaced it could drop the international
	 * assertions without any report looking different. A merged corpus is executed against
	 * empty tables before it is used; see {@link #verifyExecutable}.
	 */
	private Corpus build(List<DuckStorePacks.Pack> packs) throws IOException {
		DuckStore base = storeLocator.load();
		if (packs.isEmpty()) {
			return new Corpus(base, DuckAssertionSource.from(base), List.of());
		}
		List<DuckStorePacks.Pack> all = new ArrayList<>();
		all.add(basePack(base));
		all.addAll(packs);
		DuckStore merged = DuckStorePacks.merge(all);
		DuckAssertionSource source = DuckAssertionSource.from(merged);
		verifyExecutable(merged, source);
		return new Corpus(merged, source, List.copyOf(packs));
	}

	/** The base store as a pack, under its own identity. */
	private DuckStorePacks.Pack basePack(DuckStore base) throws IOException {
		DuckStore.PackRecord identity = base.identity().orElseThrow(() -> new IOException(
				"The base assertion store " + storeLocator.description() + " has no pack identity, so packs "
						+ "cannot be merged with it. Republish it with tools/duck-publisher."));
		return new DuckStorePacks.Pack(identity.name(), identity.version(), identity.digest(), base);
	}

	/**
	 * Executes every assertion of a merged corpus against empty tables of the declared shapes.
	 *
	 * <p>A digest proves a pack is the one that was approved, and a merge proves two packs
	 * can be combined; neither proves the combination runs. A pack can call a macro its base
	 * no longer defines. DuckDB binds macros, tables and columns at execution, so empty tables
	 * catch that here instead of in a validation. Data-dependent errors are not caught; that
	 * is what {@code AssertionCorpusDigestTest} does for the bundled store, against a fixture.
	 *
	 * <p>All three release schemas exist, so the check asks only whether the packs cohere,
	 * not whether a run supplied a previous release.
	 */
	private void verifyExecutable(DuckStore merged, DuckAssertionSource source) throws IOException {
		long start = System.currentTimeMillis();
		try (Connection con = DriverManager.getConnection("jdbc:duckdb:")) {
			try (Statement st = con.createStatement()) {
				st.execute("SET old_implicit_casting=true");
			}
			// Before anything a pack supplied reaches the connection, the table shapes
			// included: none of it gets any file access.
			DuckConnections.lockDown(con, List.of());
			try (Statement st = con.createStatement()) {
				for (String schema : List.of("prospective", "previous", "dependency")) {
					st.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
					for (Map.Entry<String, String> table : merged.tableColumns().entrySet()) {
						st.execute("CREATE TABLE IF NOT EXISTS " + schema + "." + table.getKey()
								+ " (" + table.getValue() + ")");
					}
				}
				st.execute("SET search_path='prospective'");
				st.execute("CREATE SCHEMA IF NOT EXISTS rvf_results");
				st.execute("CREATE TABLE rvf_results.qa_result (" + DuckDbValidationService.QA_RESULT_COLUMNS + ")");
			}
			DuckBinder binder = new DuckBinder(merged.sentinels(), new DuckBinder.Config(
					1L, "prospective", "previous", "dependency", "rvf_results.qa_result",
					null, List.of(), "20000101"));
			DuckDbAssertionExecutionService service = new DuckDbAssertionExecutionService(merged, binder, con);
			service.prepareSchema();

			// Resource assertions first, as a run executes them: they build tables others read.
			List<Assertion> ordered = new ArrayList<>();
			List<Assertion> rest = new ArrayList<>();
			for (Assertion assertion : source.findAll()) {
				boolean isResource = assertion.getKeywords() != null
						&& Arrays.stream(assertion.getKeywords().split(",")).map(String::trim)
								.anyMatch(DuckDbValidationService.RESOURCE_KEYWORD::equals);
				(isResource ? ordered : rest).add(assertion);
			}
			ordered.addAll(rest);

			List<String> broken = new ArrayList<>();
			for (TestRunItem item : service.execute(ordered)) {
				String message = item.getFailureMessage();
				if (message != null && !message.isBlank() && !message.startsWith("Not run: requires")) {
					broken.add(merged.assertions().get(String.valueOf(item.getAssertionUuid())).file() + ": " + message);
				}
			}
			if (!broken.isEmpty()) {
				throw new IOException("the merged corpus does not execute: " + broken.size()
						+ " assertion(s) failed against empty tables:\n  "
						+ String.join("\n  ", broken.subList(0, Math.min(10, broken.size()))));
			}
			LOGGER.info("Merged corpus verified executable: {} assertions in {}ms",
					source.findAll().size(), System.currentTimeMillis() - start);
		} catch (SQLException e) {
			throw new IOException("could not verify the merged corpus: " + e.getMessage(), e);
		} catch (DuckDbAssertionExecutionService.SetupFailedException e) {
			throw new IOException("the merged corpus's setup failed on " + e.getFailures().size()
					+ " statement(s): " + String.join("; ", e.getFailures().subList(0,
							Math.min(5, e.getFailures().size()))), e);
		}
	}

	/** The packs merged into the default corpus; empty before it is loaded. Never forces a load. */
	public List<DuckStorePacks.Pack> loadedPacks() {
		Corpus corpus = current;
		return corpus == null ? List.of() : corpus.packs();
	}

	/**
	 * The provenance of the default corpus, base included; empty before it is loaded. Never
	 * forces a load, so it is safe on an inspection endpoint.
	 *
	 * @throws IOException if the store's declared digest does not match its content
	 */
	public List<DuckStore.PackRecord> loadedProvenance() throws IOException {
		Corpus corpus = current;
		return corpus == null ? List.of() : corpus.store().provenance();
	}

	/** The configured channels, for the discovery endpoints. */
	public AssertionPackChannels channels() {
		return channels;
	}

	/**
	 * Differences between the configured packs and the loaded ones, for an operator to see
	 * that {@code POST /assertions/packs/refresh} is due.
	 */
	public List<String> pendingPackChanges() {
		Map<String, String> loadedByName = new LinkedHashMap<>();
		for (DuckStorePacks.Pack pack : loadedPacks()) {
			loadedByName.put(pack.name(), pack.digest());
		}
		List<AssertionPackFetcher.Source> configured;
		try {
			configured = configuredPackSources();
		} catch (IOException e) {
			return List.of("cannot tell: the configured packs could not be resolved from their channel - "
					+ e.getMessage());
		}
		List<String> pending = new ArrayList<>();
		for (AssertionPackFetcher.Source source : configured) {
			String have = loadedByName.remove(source.name());
			String want = "sha256:" + source.sha256().replaceFirst("^sha256:", "");
			if (have == null) {
				pending.add(source.name() + " " + source.version() + " is configured and not loaded");
			} else if (!have.equalsIgnoreCase(want)) {
				pending.add(source.name() + " is loaded at " + have + " and configured as " + want);
			}
		}
		loadedByName.forEach((name, digest) -> pending.add(name + " is loaded and no longer configured"));
		return pending;
	}

	List<AssertionPackFetcher.Source> configuredPackSources() throws IOException {
		return sourcesFrom(channels.resolveAll(packSpecs));
	}

	/**
	 * Pin specs, {@code name=<n>;version=<v>;uri=<u>;sha256=<hex>[;authHeader=<value>]}, to
	 * sources: the configured pins, and a run's pins once a channel has resolved them.
	 * Semicolons, because Spring splits list properties on commas and URLs contain commas.
	 */
	private static List<AssertionPackFetcher.Source> sourcesFrom(List<String> specs) {
		List<AssertionPackFetcher.Source> sources = new ArrayList<>();
		for (String spec : specs) {
			if (spec == null || spec.isBlank()) {
				continue;
			}
			Map<String, String> fields = new LinkedHashMap<>();
			for (String part : spec.split(";")) {
				String[] kv = part.split("=", 2);
				if (kv.length == 2) {
					fields.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
				}
			}
			sources.add(new AssertionPackFetcher.Source(
					fields.getOrDefault("name", ""),
					fields.getOrDefault("version", ""),
					URI.create(fields.getOrDefault("uri", "")),
					fields.getOrDefault("sha256", ""),
					fields.get("authheader")));
		}
		return sources;
	}

	/** What an assertion executes, from the default corpus; empty for one it does not hold. */
	public Optional<DuckStore.StoredAssertion> storedAssertion(String uuid) {
		return Optional.ofNullable(currentCorpus().store().assertions().get(uuid));
	}

	private DuckAssertionSource source() {
		return currentCorpus().source();
	}

	@Override
	public List<Assertion> findAll() {
		return source().findAll();
	}

	@Override
	public Assertion findAssertionByUUID(UUID uuid) {
		return source().getAssertionByUuid(uuid);
	}

	@Override
	public Assertion getAssertionByUuid(UUID assertionUUID) {
		return source().getAssertionByUuid(assertionUUID);
	}

	@Override
	public List<Assertion> getAssertionsByKeyWords(String keyWord, boolean exactMatch) {
		return source().getAssertionsByKeyWords(keyWord, exactMatch);
	}

	@Override
	public Long count() {
		return (long) source().findAll().size();
	}

	@Override
	public AssertionGroup getAssertionGroupByName(String groupName) {
		DuckAssertionSource source = source();
		return source.populatedGroupNames().contains(groupName) ? group(source, groupName) : null;
	}

	@Override
	public List<AssertionGroup> getAssertionGroupsByNames(List<String> groupNames) {
		List<AssertionGroup> groups = new ArrayList<>();
		for (String name : groupNames) {
			AssertionGroup group = getAssertionGroupByName(name);
			if (group != null) {
				groups.add(group);
			}
		}
		return groups;
	}

	@Override
	public List<AssertionGroup> getAllAssertionGroups() {
		DuckAssertionSource source = source();
		List<AssertionGroup> groups = new ArrayList<>();
		for (String name : source.populatedGroupNames()) {
			groups.add(group(source, name));
		}
		return groups;
	}

	@Override
	public List<AssertionGroup> getGroupsForAssertion(Assertion assertion) {
		List<AssertionGroup> groups = new ArrayList<>();
		if (assertion != null && assertion.getUuid() != null) {
			DuckAssertionSource source = source();
			for (String name : source.groupsOf(assertion.getUuid())) {
				groups.add(group(source, name));
			}
		}
		return groups;
	}

	private static AssertionGroup group(DuckAssertionSource source, String name) {
		AssertionGroup group = new AssertionGroup();
		group.setName(name);
		group.setAssertions(new HashSet<>(source.getAssertionsInGroups(List.of(name))));
		return group;
	}

	// ---- keyed on a numeric id the store does not have

	@Override
	public Assertion find(Long id) {
		return null;
	}

	@Override
	public List<Test> getTestsByAssertionId(Long assertionId) {
		return List.of();
	}

	@Override
	public List<AssertionGroup> getGroupsForAssertion(Long assertionId) {
		return List.of();
	}

	@Override
	public List<AssertionTest> getAssertionTests(Assertion assertion) {
		return List.of();
	}

	@Override
	public List<Test> getTests(Assertion assertion) {
		return List.of();
	}

	// ---- mutation: the store is read-only

	@Override
	public Assertion create(Assertion assertion) {
		throw readOnly("create an assertion");
	}

	@Override
	public Assertion save(Assertion assertion) {
		throw readOnly("save an assertion");
	}

	@Override
	public void delete(Assertion assertion) {
		throw readOnly("delete an assertion");
	}

	@Override
	public Assertion addTest(Assertion assertion, Test test) {
		throw readOnly("add a test");
	}

	@Override
	public Assertion addTest(Long assertionId, Test test) {
		throw readOnly("add a test");
	}

	@Override
	public Assertion addTests(Assertion assertion, Collection<Test> tests) {
		throw readOnly("add tests");
	}

	@Override
	public Assertion deleteTest(Assertion assertion, Test test) {
		throw readOnly("delete a test");
	}

	@Override
	public Assertion deleteTests(Assertion assertion, Collection<Test> tests) {
		throw readOnly("delete tests");
	}

	@Override
	public AssertionGroup addAssertionToGroup(Assertion assertion, AssertionGroup group) {
		throw readOnly("add an assertion to a group");
	}

	@Override
	public AssertionGroup removeAssertionFromGroup(Assertion assertion, AssertionGroup group) {
		throw readOnly("remove an assertion from a group");
	}

	@Override
	public AssertionGroup createAssertionGroup(AssertionGroup group) {
		throw readOnly("create an assertion group");
	}

	private static UnsupportedOperationException readOnly(String what) {
		return new UnsupportedOperationException("Cannot " + what + ": with "
				+ ExecutionEngine.PROPERTY + "=" + ExecutionEngine.DUCKDB + " the assertion corpus "
				+ "is the published store, not a database. Change the assertions repository and "
				+ "republish the store.");
	}
}
