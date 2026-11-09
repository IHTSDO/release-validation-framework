package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.ihtsdo.rvf.config.ExecutionEngine;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.AssertionTest;
import org.ihtsdo.rvf.core.data.model.Test;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link AssertionService} over the assertion store, so the assertion endpoints and the
 * validation submission endpoints work without the assertion tables.
 *
 * <p>The corpus is the bundled store, or {@code rvf.duck.store}; see
 * {@code docs/duckdb-engine.md}. This class owns the corpus a validation executes:
 * {@link DuckDbValidationService} asks it for one at the start of a run and keeps it for the
 * whole run.
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

	private final DuckStoreLocator storeLocator;

	/** Loaded on first use. */
	private volatile Corpus current;

	@Autowired
	public DuckAssertionService(DuckStoreLocator storeLocator) {
		this.storeLocator = storeLocator;
	}

	/** A corpus with no locator behind it, for tests. */
	DuckAssertionService(Corpus corpus) {
		this.storeLocator = null;
		this.current = corpus;
	}

	/** A corpus a run executes: the store, and its assertions and groups. */
	public record Corpus(DuckStore store, DuckAssertionSource source) {
	}

	/**
	 * Loads the corpus when the application is ready, so the store's identity is in the
	 * startup log. A failure is logged, not fatal: the service
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

	/** The corpus a run uses: the store and its assertions. Loaded on first use. */
	public Corpus currentCorpus() {
		Corpus corpus = current;
		if (corpus != null) {
			return corpus;
		}
		synchronized (this) {
			if (current == null) {
				try {
					DuckStore store = storeLocator.load();
					current = new Corpus(store, DuckAssertionSource.from(store));
				} catch (IOException e) {
					throw new UncheckedIOException("The DuckDB assertion corpus could not be loaded from "
							+ storeLocator.description(), e);
				}
			}
			return current;
		}
	}

	/**
	 * The provenance of the loaded store; empty before it is loaded. Never forces a load, so
	 * it is safe on an inspection endpoint.
	 *
	 * @throws IOException if the store's declared digest does not match its content
	 */
	public List<DuckStore.PackRecord> loadedProvenance() throws IOException {
		Corpus corpus = current;
		return corpus == null ? List.of() : corpus.store().provenance();
	}

	/** What an assertion executes; empty for one the corpus does not hold. */
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
