package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exclusion rule, pinned case by case.
 *
 * <p>Each of these is a way the rule could be simplified into something that
 * agrees with MySQL on the obvious inputs and diverges on a real one. The point
 * of the test is not that exclusion works - it is that it works the SAME, since
 * the whole engine swap is judged by whether the two report alike.
 */
class DuckReleaseUnpackerTest {

	/**
	 * Exclusions are written with the GENERIC rel2 prefix, not the package's own
	 * sct2/der2. That is not a convention this code chose - MySQL's
	 * isExcludedFile normalises the CANDIDATE to rel2 and then matches the rule
	 * against it, so a rule spelled sct2_... can never match anything. See
	 * anEntrySpelledWithThePackagesOwnPrefixMatchesNothing.
	 */
	private static final String EXCLUDE_DESCRIPTION_DELTA =
			"rel2_Description_Delta-en_INT_20260831.txt";

	private static final String DELTA = "sct2_Description_Delta-en_INT_20260831.txt";
	private static final String FULL = "sct2_Description_Full-en_INT_20260831.txt";
	private static final String SNAPSHOT = "sct2_Description_Snapshot-en_INT_20260831.txt";
	private static final String CONCEPT = "sct2_Concept_Snapshot_INT_20260831.txt";

	@Test
	void excludingADeltaExcludesItsFullAndSnapshotToo() {
		// The reason the rule expands: a run excludes the delta it does not
		// trust, and validating the full and snapshot built FROM that delta
		// would report the same content it was told to leave out.
		List<String> rules = DuckReleaseUnpacker.expand(List.of(EXCLUDE_DESCRIPTION_DELTA));
		assertTrue(DuckReleaseUnpacker.isExcluded(DELTA, rules));
		assertTrue(DuckReleaseUnpacker.isExcluded(FULL, rules), FULL);
		assertTrue(DuckReleaseUnpacker.isExcluded(SNAPSHOT, rules), SNAPSHOT);
		assertFalse(DuckReleaseUnpacker.isExcluded(CONCEPT, rules), "an unrelated file survives");
	}

	@Test
	void bothTheUnderscoreAndHyphenSpellingsExpand() {
		// Delta_ for unsuffixed files, Delta- for the language-suffixed ones.
		// Handling only one silently leaves every en/en-us file in.
		List<String> hyphen = DuckReleaseUnpacker.expand(List.of("rel2_Description_Delta-en_INT_20260831.txt"));
		assertTrue(DuckReleaseUnpacker.isExcluded("sct2_Description_Snapshot-en_INT_20260831.txt", hyphen));

		List<String> underscore = DuckReleaseUnpacker.expand(List.of("rel2_Concept_Delta_INT_20260831.txt"));
		assertTrue(DuckReleaseUnpacker.isExcluded("sct2_Concept_Snapshot_INT_20260831.txt", underscore));
	}

	@Test
	void sct2AndDer2BothNormaliseToRel2AndALeadingXIsStripped() {
		// So an exclusion written against one prefix matches a package that
		// ships another, and the x-prefixed extension variants are covered.
		List<String> rules = DuckReleaseUnpacker.expand(List.of("rel2_Refset_SimpleDelta_INT_20260831.txt"));
		assertTrue(DuckReleaseUnpacker.isExcluded("der2_Refset_SimpleSnapshot_INT_20260831.txt", rules),
				"der2 normalises to rel2");
		assertTrue(DuckReleaseUnpacker.isExcluded("xder2_Refset_SimpleSnapshot_INT_20260831.txt", rules),
				"a leading x is stripped before matching");
	}

	@Test
	void theExclusionEntryLosesItsDateAndMatchesBySubstring() {
		// The date suffix is stripped from the RULE, so an exclusion naming one
		// release's file also excludes another release's - which is what makes
		// the rule usable across effectiveTimes, and is worth knowing about.
		List<String> rules = DuckReleaseUnpacker.expand(List.of(EXCLUDE_DESCRIPTION_DELTA));
		assertTrue(DuckReleaseUnpacker.isExcluded(
				"sct2_Description_Delta-en_INT_20250131.txt", rules),
				"a different effectiveTime still matches");
	}

	/**
	 * The trap, pinned rather than fixed.
	 *
	 * <p>Because the candidate is normalised to rel2 before matching, an
	 * exclusion written with the package's own sct2/der2 prefix matches NOTHING
	 * - the run reports on a file it was told to leave out, and says nothing
	 * about it. Reproduced rather than corrected: making it work here and not on
	 * MySQL would mean the two engines validate different files from the same
	 * request, which is a worse failure than the one being fixed.
	 */
	@Test
	void anEntrySpelledWithThePackagesOwnPrefixMatchesNothing() {
		List<String> rules = DuckReleaseUnpacker.expand(List.of(DELTA));
		assertFalse(DuckReleaseUnpacker.isExcluded(DELTA, rules),
				"an sct2-spelled exclusion silently matches nothing - as on MySQL");
	}

	@Test
	void nothingIsExcludedWhenTheRunExcludesNothing() {
		assertEquals(List.of(), DuckReleaseUnpacker.expand(null));
		assertFalse(DuckReleaseUnpacker.isExcluded(CONCEPT, List.of()));
	}

	@Test
	void unpackFlattensEveryReleaseTypeIntoOneDirectoryAndDropsTheExcluded(@TempDir Path tmp)
			throws Exception {
		// Flat, and all three release types together: the materialiser maps each
		// file to its own _d/_s/_f table, so they must arrive side by side.
		File zip = tmp.resolve("release.zip").toFile();
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			for (String entry : List.of("Delta/Terminology/" + DELTA,
					"Full/Terminology/" + FULL,
					"Snapshot/Terminology/" + SNAPSHOT,
					"Snapshot/Terminology/" + CONCEPT)) {
				out.putNextEntry(new ZipEntry(entry));
				out.write("id\teffectiveTime\n".getBytes());
				out.closeEntry();
			}
		}

		Path unpacked = DuckReleaseUnpacker.unpack(zip, tmp, "prospective",
				List.of(EXCLUDE_DESCRIPTION_DELTA));

		assertTrue(Files.exists(unpacked.resolve(CONCEPT)), "an unrelated file is kept");
		assertFalse(Files.exists(unpacked.resolve(DELTA)), "the excluded delta is gone");
		assertFalse(Files.exists(unpacked.resolve(FULL)), "and so is its full");
		assertFalse(Files.exists(unpacked.resolve(SNAPSHOT)), "and its snapshot");
		assertEquals(1, Files.list(unpacked).count(), "everything lands in one flat directory");
	}
}
