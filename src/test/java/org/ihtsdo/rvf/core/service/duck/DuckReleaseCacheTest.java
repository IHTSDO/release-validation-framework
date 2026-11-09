package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cached release must be usable under the name the store compiled against, and
 * must never be reused when reusing it would be wrong.
 *
 * <p>Two failure modes are worth more than the speed this buys, so they are what
 * these tests are about:
 *
 * <ul>
 * <li><b>Resolving under the wrong name.</b> The store's sentinels produce the
 *     bare literal {@code previous.concept_s}. A cached file whose tables sit in
 *     a schema called {@code previous} does NOT satisfy that - only
 *     {@code previous.previous.concept_s} would - so the tables have to be in
 *     {@code main}. Silent, and it would look like an empty previous release.
 * <li><b>Reusing a file the column map has invalidated.</b>
 *     {@link DuckMaterialiser} loads some tables positionally where the shipped
 *     columns disagree with the declared ones, so the same bytes load
 *     differently under a different {@code tableColumns}. A key that ignored the
 *     map would serve wrongly-loaded data.
 * </ul>
 */
class DuckReleaseCacheTest {

	private static final Map<String, String> COLUMNS = columns("id BIGINT, active INTEGER");

	/** The JVM's CPUs and DuckDB's default memory limit, as with nothing configured. */
	private static final DuckConnections.Limits NO_LIMITS = new DuckConnections.Limits(0, "");

	private static Map<String, String> columns(String spec) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("concept_s", spec);
		return m;
	}

	/** A minimal but real RF2 snapshot file the materialiser will load. */
	private Path release(Path dir, String name, int rows) throws IOException {
		Path snapshot = dir.resolve("Snapshot").resolve("Terminology");
		Files.createDirectories(snapshot);
		StringBuilder sb = new StringBuilder("id\tactive\n");
		for (int i = 0; i < rows; i++) {
			sb.append(100000000 + i).append("\t1\n");
		}
		Files.writeString(snapshot.resolve(name), sb.toString());
		return dir;
	}

	@Test
	void aCachedReleaseResolvesUnderTheStoresBareLiteral(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 5);
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 1073741824L, NO_LIMITS);

		Path cached = cache.get(releaseDir, COLUMNS);
		assertNotNull(cached, "a miss must build rather than give up");

		Class.forName("org.duckdb.DuckDBDriver");
		try (Connection con = DriverManager.getConnection("jdbc:duckdb:" + root.resolve("run.duckdb"))) {
			DuckReleaseCache.attach(con, cached, "previous");
			try (Statement st = con.createStatement();
				 ResultSet r = st.executeQuery("SELECT count(*) FROM previous.concept_s")) {
				r.next();
				assertEquals(5, r.getInt(1),
						"previous.concept_s is exactly what DuckBinder produces from <PREVIOUS>");
			}
		}
	}

	@Test
	void aSecondRequestHitsRatherThanRebuilding(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 3);
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 1073741824L, NO_LIMITS);

		Path first = cache.get(releaseDir, COLUMNS);
		long builtAt = Files.getLastModifiedTime(first).toMillis();
		Path second = cache.get(releaseDir, COLUMNS);

		assertEquals(first, second);
		assertEquals(1, Files.list(root.resolve("cache")).filter(p -> p.toString().endsWith(".duckdb")).count(),
				"one release, one cached file");
		assertTrue(Files.getLastModifiedTime(second).toMillis() >= builtAt,
				"a hit records recency of use, so eviction is LRU rather than oldest-built");
	}

	@Test
	void aDifferentColumnMapMissesRatherThanServingWronglyLoadedData(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 3);
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 1073741824L, NO_LIMITS);

		Path a = cache.get(releaseDir, COLUMNS);
		Path b = cache.get(releaseDir, columns("id BIGINT, moduleid BIGINT"));

		assertNotEquals(a, b,
				"the materialiser loads positionally where columns disagree, so the map is part of the key");
	}

	@Test
	void aChangedReleaseMissesEvenUnderTheSameName(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 3);
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 1073741824L, NO_LIMITS);
		Path before = cache.get(releaseDir, COLUMNS);

		// Republished with more content under exactly the same filename.
		release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 9);
		Path after = cache.get(releaseDir, COLUMNS);

		assertNotEquals(before, after,
				"keying on a name alone would have served last month's rows");
	}

	@Test
	void theFingerprintIsStableAcrossMapIterationOrder(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 2);
		Map<String, String> one = new LinkedHashMap<>();
		one.put("concept_s", "id BIGINT");
		one.put("description_s", "id BIGINT");
		Map<String, String> other = new LinkedHashMap<>();
		other.put("description_s", "id BIGINT");
		other.put("concept_s", "id BIGINT");

		assertEquals(DuckReleaseCache.fingerprint(releaseDir, one),
				DuckReleaseCache.fingerprint(releaseDir, other),
				"an unordered map must not change the key between two runs of the same release");
	}

	@Test
	void aDisabledCacheSuppliesNothingAtAll(@TempDir Path root) throws Exception {
		Path releaseDir = release(root.resolve("release"), "sct2_Concept_Snapshot_INT_20260101.txt", 2);
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 0, NO_LIMITS);

		assertFalse(cache.isEnabled());
		assertNull(cache.get(releaseDir, COLUMNS), "off by default means no file and no directory");
		assertFalse(Files.exists(root.resolve("cache")));
	}

	@Test
	void anUnreadableReleaseFallsBackInsteadOfFailingTheValidation(@TempDir Path root) {
		DuckReleaseCache cache = new DuckReleaseCache(root.resolve("cache"), 1073741824L, NO_LIMITS);

		assertNull(cache.get(root.resolve("no-such-release"), COLUMNS),
				"a cache is an optimisation; null tells the caller to materialise as before");
	}

	@Test
	void evictionRemovesTheLeastRecentlyUsedOnceOverBudget(@TempDir Path root) throws Exception {
		Path cacheDir = root.resolve("cache");
		Files.createDirectories(cacheDir);
		// Three files, aged past the grace window so they are eligible.
		long old = System.currentTimeMillis() - 7_200_000L;
		for (int i = 0; i < 3; i++) {
			Path f = cacheDir.resolve("release-" + i + ".duckdb");
			Files.write(f, new byte[400]);
			Files.setLastModifiedTime(f, FileTime.fromMillis(old + i * 1000));
		}

		new DuckReleaseCache(cacheDir, 900, NO_LIMITS).evict();

		assertFalse(Files.exists(cacheDir.resolve("release-0.duckdb")), "least recently used goes first");
		assertTrue(Files.exists(cacheDir.resolve("release-2.duckdb")), "most recently used survives");
	}

	@Test
	void evictionSparesFilesInsideTheGraceWindow(@TempDir Path root) throws Exception {
		Path cacheDir = root.resolve("cache");
		Files.createDirectories(cacheDir);
		for (int i = 0; i < 3; i++) {
			Files.write(cacheDir.resolve("release-" + i + ".duckdb"), new byte[400]);
		}

		new DuckReleaseCache(cacheDir, 100, NO_LIMITS).evict();

		assertEquals(3, Files.list(cacheDir).count(),
				"a file being built by another worker right now must not be deleted under it");
	}
}
