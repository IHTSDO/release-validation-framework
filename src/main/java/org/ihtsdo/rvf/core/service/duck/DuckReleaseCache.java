package org.ihtsdo.rvf.core.service.duck;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Materialised copies of the previous and dependency releases, which do not change
 * between runs. Off unless {@code rvf.duck.cache.max-gb} is set.
 *
 * <p>Daily validations of one product compare against the same published release for a
 * whole release cycle. Measured on the SNOMED CT-AU edition, materialising that previous
 * release costs 21-25s of a 257s run. The prospective release is never cached: it differs
 * every run.
 *
 * <h2>Read-only ATTACH, and why the layout matters</h2>
 *
 * <p>A cached release is attached, not copied in:
 * {@code ATTACH '<file>' AS previous (READ_ONLY)}. That works because the store's
 * sentinels resolve {@code <PREVIOUS>} to the bare literal {@code previous}, and
 * DuckDB resolves a two-part {@code previous.concept_s} against an attached
 * catalogue's default schema. So a cached file MUST hold its tables in
 * {@code main}: with them in a schema called {@code previous},
 * {@code previous.concept_s} does not resolve and only
 * {@code previous.previous.concept_s} does - verified, and the reason
 * {@link #MAIN} is what gets materialised here.
 *
 * <p>Safe for a pool of workers because nothing writes to a cached file after it
 * is built. DuckDB permits many readers OR one writer and refuses everything
 * else - a process holding a file read-write locks out even read-only openers -
 * so the build happens on a temporary name and is published by an atomic rename.
 * A reader therefore only ever sees a complete file.
 *
 * <h2>The fingerprint is not the filename</h2>
 *
 * <p>Keying on a release name would be wrong twice over. A release republished
 * under the same name would hit a stale cache; and a cached file is only valid
 * for the {@code tableColumns} map that built it, because
 * {@link DuckMaterialiser} loads some tables POSITIONALLY where the shipped
 * columns disagree with the declared ones. Change the store and the same bytes
 * load differently. So the key is a digest of the release's own shape - every
 * RF2 filename and its size - together with a digest of the column map. A
 * mismatch misses and rebuilds instead of loading silently wrong data.
 */
public final class DuckReleaseCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckReleaseCache.class);

	/**
	 * Tables go in the DEFAULT schema so that {@code ATTACH ... AS previous}
	 * makes the store's compiled {@code previous.concept_s} resolve.
	 */
	static final String MAIN = "main";

	private static final String SUFFIX = ".duckdb";

	/**
	 * Files younger than this are never evicted. Eviction of an attached file is
	 * safe on POSIX - the inode survives until every reader closes it - but a
	 * file being BUILT by another worker right now is a different matter, and
	 * this keeps the two apart without needing a lock.
	 */
	private static final long EVICTION_GRACE_MILLIS = 3_600_000L;

	private final Path cacheDir;
	private final long maxBytes;
	private final DuckConnections.Limits limits;

	/**
	 * @param limits the CPUs and memory a build may use: the same as a run's, since a build
	 *               happens inside one
	 */
	public DuckReleaseCache(Path cacheDir, long maxBytes, DuckConnections.Limits limits) {
		this.cacheDir = cacheDir;
		this.maxBytes = maxBytes;
		this.limits = limits;
	}

	public boolean isEnabled() {
		return maxBytes > 0;
	}

	/**
	 * The cached materialisation of {@code releaseDir}, building it if absent.
	 *
	 * @return the file to ATTACH, or {@code null} if the cache could not supply
	 *         one - in which case the caller must materialise as before. Null
	 *         rather than an exception because a cache is an optimisation: a
	 *         validation must not fail because a disk was full or a directory
	 *         unreadable.
	 */
	public Path get(Path releaseDir, Map<String, String> tableColumns) {
		if (!isEnabled()) {
			return null;
		}
		try {
			String key = fingerprint(releaseDir, tableColumns);
			Path cached = cacheDir.resolve(key + SUFFIX);
			if (Files.isRegularFile(cached)) {
				// Touch it so eviction sees recency of USE, not of creation.
				try {
					Files.setLastModifiedTime(cached, java.nio.file.attribute.FileTime.fromMillis(
							System.currentTimeMillis()));
				} catch (IOException ignored) {
					// Recency is a heuristic; failing to record it is not fatal.
				}
				LOGGER.info("Release cache HIT {} ({} MB)", cached.getFileName(),
						Files.size(cached) / 1048576);
				return cached;
			}
			return build(releaseDir, tableColumns, cached);
		} catch (Exception e) {
			LOGGER.warn("Release cache unavailable for {}, materialising instead: {}",
					releaseDir, e.toString());
			return null;
		}
	}

	private Path build(Path releaseDir, Map<String, String> tableColumns, Path target)
			throws IOException, SQLException, ClassNotFoundException {
		Files.createDirectories(cacheDir);
		// A unique temporary name per builder: two workers may miss on the same
		// release at the same moment, and both must be able to build without
		// colliding on the single-writer lock. Both then rename onto the same
		// target, and the last rename wins with identical content.
		Path temp = cacheDir.resolve(target.getFileName() + ".building-" + ProcessHandle.current().pid()
				+ "-" + System.nanoTime());
		long t0 = System.currentTimeMillis();
		try {
			Class.forName("org.duckdb.DuckDBDriver");
			try (Connection con = DriverManager.getConnection("jdbc:duckdb:" + temp)) {
				limits.apply(con);
				// The release's files to read, and the cache to build into and spill beside.
				DuckConnections.lockDown(con, List.of(releaseDir, cacheDir));
				DuckMaterialiser.Result result =
						DuckMaterialiser.materialise(con, releaseDir, MAIN, tableColumns);
				try (Statement st = con.createStatement()) {
					// Without this the rows are still in the write-ahead log, so a reader
					// attaching the renamed file finds an empty catalogue.
					st.execute("CHECKPOINT");
				}
				LOGGER.info("Release cache BUILD {}: {} tables, {} rows in {}ms",
						target.getFileName(), result.tablesLoaded(), result.rows(), result.millis());
			}
			Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Release cache published {} ({} MB) in {}ms", target.getFileName(),
					Files.size(target) / 1048576, System.currentTimeMillis() - t0);
			evict();
			return target;
		} catch (Exception e) {
			FileUtils.deleteQuietly(temp.toFile());
			FileUtils.deleteQuietly(Path.of(temp + ".wal").toFile());
			throw e;
		} finally {
			FileUtils.deleteQuietly(Path.of(temp + ".wal").toFile());
		}
	}

	/**
	 * Digest of what actually determines the contents: every RF2 file's name and
	 * size, plus the column map that decides how they are loaded.
	 *
	 * <p>Names and sizes rather than a content hash because a full edition is
	 * 5.4GB unpacked and hashing it would cost more than the materialisation this
	 * saves. Two different releases sharing every filename AND every byte count
	 * is the accepted risk, and it is not a silent one: the alternative on offer
	 * was keying on a release name, which collides whenever a release is
	 * republished.
	 */
	static String fingerprint(Path releaseDir, Map<String, String> tableColumns) throws IOException {
		List<String> parts = new ArrayList<>();
		try (Stream<Path> tree = Files.walk(releaseDir)) {
			for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
				parts.add(releaseDir.relativize(file) + ":" + Files.size(file));
			}
		}
		// Sorted, so an unordered map cannot change the key between two runs of
		// the same release.
		for (Map.Entry<String, String> e : new TreeMap<>(tableColumns).entrySet()) {
			parts.add(e.getKey() + "=" + e.getValue());
		}
		return digest(String.join("\n", parts));
	}

	private static String digest(String s) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] out = sha.digest(s.getBytes(StandardCharsets.UTF_8));
			// 16 hex characters: enough that a collision is not a practical
			// concern, short enough that a human can compare two filenames.
			return HexFormat.of().formatHex(out).substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required by the platform", e);
		}
	}

	/**
	 * Keeps the cache under its size budget, oldest use first.
	 *
	 * <p>Deleting a file a worker has attached is safe here: on POSIX the inode
	 * outlives the unlink and the reader carries on against it. What is NOT safe
	 * is deleting one mid-build, which is why anything younger than the grace
	 * window is left alone.
	 */
	void evict() {
		try (Stream<Path> files = Files.list(cacheDir)) {
			List<Path> cached = files.filter(p -> p.getFileName().toString().endsWith(SUFFIX))
					.sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
					.toList();
			long total = 0;
			for (Path p : cached) {
				total += Files.size(p);
			}
			long now = System.currentTimeMillis();
			for (Path p : cached) {
				if (total <= maxBytes) {
					return;
				}
				if (now - p.toFile().lastModified() < EVICTION_GRACE_MILLIS) {
					continue;
				}
				long size = Files.size(p);
				if (Files.deleteIfExists(p)) {
					total -= size;
					LOGGER.info("Release cache evicted {} ({} MB), now {} MB of {} MB budget",
							p.getFileName(), size / 1048576, total / 1048576, maxBytes / 1048576);
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Could not evict from the release cache: {}", e.toString());
		}
	}

	/** Attaches a cached release under the schema literal the store expects. */
	static void attach(Connection connection, Path cached, String schema) throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.execute("ATTACH '" + cached + "' AS " + schema + " (READ_ONLY)");
		}
	}
}
