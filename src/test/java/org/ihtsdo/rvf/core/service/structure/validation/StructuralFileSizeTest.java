package org.ihtsdo.rvf.core.service.structure.validation;

import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The file-size test reads sizes from the archive instead of extracting it.
 *
 * <p>It used to extract four whole release directories - prospective and
 * previous, snapshot and full - through snomedboot's {@code ReleaseImporter},
 * single-threaded, purely to call {@link java.io.File#length()}. On the AU
 * edition that was about 100 seconds of a 224 second validation, and it happened
 * only when a previous release was supplied, which is exactly the nightly case.
 *
 * <p>{@code ZipEntry.getSize()} is the uncompressed size, and against a full
 * extraction of that release it matched on all 76 entries with none unknown. The
 * tests here pin the selection rules that decide WHICH entries are compared,
 * because those are what would silently change a finding.
 */
class StructuralFileSizeTest {

	private final StructuralTestRunner runner = new StructuralTestRunner();

	private Path zip(Path dir, String name, Map<String, Integer> entries) throws IOException {
		Path zip = dir.resolve(name);
		try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out)) {
			for (Map.Entry<String, Integer> e : entries.entrySet()) {
				zos.putNextEntry(new ZipEntry(e.getKey()));
				zos.write("x".repeat(e.getValue()).getBytes(StandardCharsets.UTF_8));
				zos.closeEntry();
			}
		}
		return zip;
	}

	@Test
	void sizesComeBackExactWithoutExtracting(@TempDir Path dir) throws Exception {
		Path archive = zip(dir, "release.zip", Map.of(
				"Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260101.txt", 500,
				"Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20260101.txt", 1500));

		Map<String, Long> sizes = runner.entrySizes(archive.toString(), "Snapshot");

		assertEquals(2, sizes.size());
		assertEquals(500L, sizes.get("sct2_Concept_Snapshot_INT_20260101.txt"));
		assertEquals(1500L, sizes.get("sct2_Description_Snapshot-en_INT_20260101.txt"));
	}

	@Test
	void onlyTheRequestedReleaseTypeIsReturned(@TempDir Path dir) throws Exception {
		Path archive = zip(dir, "release.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260101.txt", 100,
				"Full/sct2_Concept_Full_INT_20260101.txt", 900,
				"Delta/sct2_Concept_Delta_INT_20260101.txt", 10));

		assertEquals(Map.of("sct2_Concept_Snapshot_INT_20260101.txt", 100L),
				runner.entrySizes(archive.toString(), "Snapshot"));
		assertEquals(Map.of("sct2_Concept_Full_INT_20260101.txt", 900L),
				runner.entrySizes(archive.toString(), "Full"),
				"Full and Snapshot are separate tests and must not see each other's files");
	}

	@Test
	void entriesAreKeyedOnBaseNameAsTheExtractedFilesWere(@TempDir Path dir) throws Exception {
		// ReleaseImporter flattened everything into one directory and the old
		// code compared File.getName(), so a nested entry must key the same way.
		Path archive = zip(dir, "release.zip", Map.of(
				"SnomedCT_Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260101.txt", 42));

		assertTrue(runner.entrySizes(archive.toString(), "Snapshot")
				.containsKey("sct2_Concept_Snapshot_INT_20260101.txt"));
	}

	@Test
	void nonTxtEntriesAreIgnored(@TempDir Path dir) throws Exception {
		Path archive = zip(dir, "release.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260101.txt", 100,
				"Snapshot/readme_Snapshot_notes.pdf", 5000,
				"Snapshot/manifest_Snapshot.xml", 200));

		assertEquals(1, runner.entrySizes(archive.toString(), "Snapshot").size(),
				"the pairing rule only ever compared .txt files");
	}

	@Test
	void anEntryWithNoUnderscoreIsSkippedRatherThanCrashing(@TempDir Path dir) throws Exception {
		// The pairing rule indexes on lastIndexOf("_"); an entry without one
		// would have thrown StringIndexOutOfBoundsException.
		Path archive = zip(dir, "release.zip", Map.of(
				"Snapshot/Snapshot.txt", 10,
				"Snapshot/sct2_Concept_Snapshot_INT_20260101.txt", 100));

		Map<String, Long> sizes = runner.entrySizes(archive.toString(), "Snapshot");

		assertFalse(sizes.containsKey("Snapshot.txt"));
		assertEquals(1, sizes.size());
	}

	@Test
	void directoriesAreNotEntries(@TempDir Path dir) throws Exception {
		Path zip = dir.resolve("release.zip");
		try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out)) {
			zos.putNextEntry(new ZipEntry("Snapshot_dir/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("Snapshot_dir/sct2_Concept_Snapshot_INT_20260101.txt"));
			zos.write("data".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}

		assertEquals(1, runner.entrySizes(zip.toString(), "Snapshot").size());
	}

	@Test
	void anEmptyReleaseTypeYieldsNothingRatherThanFailing(@TempDir Path dir) throws Exception {
		Path archive = zip(dir, "release.zip", Map.of(
				"Delta/sct2_Concept_Delta_INT_20260101.txt", 10));

		assertTrue(runner.entrySizes(archive.toString(), "Full").isEmpty(),
				"a package with no Full files is a warning, not an error");
	}

	/** Only getFilePath is reached by the size comparison. */
	private ResourceProvider at(Path zip) {
		return new ResourceProvider() {
			@Override
			public BufferedReader getReader(String name, Charset charset) {
				throw new UnsupportedOperationException("the size test never reads content");
			}

			@Override
			public String getFilePath() {
				return zip.toString();
			}

			@Override
			public List<String> getFileNames() {
				throw new UnsupportedOperationException("the size test never lists names");
			}

			@Override
			public boolean match(String name) {
				throw new UnsupportedOperationException("the size test never matches names");
			}
		};
	}

	@Test
	void aSmallerProspectiveFileIsStillReported(@TempDir Path dir) throws Exception {
		Path prospective = zip(dir, "prospective.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260201.txt", 100));
		Path previous = zip(dir, "previous.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260101.txt", 5000));
		StreamTestReport report = new StreamTestReport(new CsvResultFormatter(),
				new ByteArrayOutputStream(), false);

		runner.compareFileSizes(at(prospective), at(previous), report, "Snapshot",
				"Snapshot files must be equal to or greater in size than previous release");

		// getNumErrors, not the stream: StreamTestReport buffers until its
		// summary is written, so the count is the contract a caller sees.
		assertEquals(1, report.getNumErrors(),
				"a shrinking snapshot is the whole point of this test");
	}

	@Test
	void anEqualOrLargerProspectiveFilePasses(@TempDir Path dir) throws Exception {
		Path prospective = zip(dir, "prospective.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260201.txt", 5000));
		Path previous = zip(dir, "previous.zip", Map.of(
				"Snapshot/sct2_Concept_Snapshot_INT_20260101.txt", 5000));
		StreamTestReport report = new StreamTestReport(new CsvResultFormatter(),
				new ByteArrayOutputStream(), false);

		runner.compareFileSizes(at(prospective), at(previous), report, "Snapshot", "irrelevant");

		assertEquals(0, report.getNumErrors(), "a release that grew is not a finding");
	}

	@Test
	void filesArePairedIgnoringTheDateSuffix(@TempDir Path dir) throws Exception {
		// The pairing rule drops everything after the last underscore, which is
		// how a 20260201 file is compared against its 20260101 predecessor.
		Path prospective = zip(dir, "prospective.zip", Map.of(
				"Snapshot/sct2_Description_Snapshot-en_INT_20260201.txt", 10));
		Path previous = zip(dir, "previous.zip", Map.of(
				"Snapshot/sct2_Description_Snapshot-en_INT_20260101.txt", 900));
		StreamTestReport report = new StreamTestReport(new CsvResultFormatter(),
				new ByteArrayOutputStream(), false);

		runner.compareFileSizes(at(prospective), at(previous), report, "Snapshot", "irrelevant");

		assertEquals(1, report.getNumErrors(), "dates differ but the files are the same file");
	}
}
