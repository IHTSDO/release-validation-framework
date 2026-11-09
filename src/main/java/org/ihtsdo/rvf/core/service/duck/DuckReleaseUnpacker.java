package org.ihtsdo.rvf.core.service.duck;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Unzips a release for {@link DuckMaterialiser}, applying the same file
 * selection the MySQL loader applies.
 *
 * <p>The acquisition step hands over ZIPs; the materialiser reads a DIRECTORY.
 * Something has to bridge that, and the temptation is to write three lines of
 * unzip. This class exists because those three lines would quietly drop two
 * behaviours that decide which files a validation sees:
 *
 * <ul>
 * <li><b>One flat folder.</b> {@code ReleaseDataManager} extracts every release
 *     type into a single directory with
 *     {@code ZipFileUtils.extractFilesFromZipToOneFolder}, so delta, snapshot
 *     and full sit side by side. The same call is used here rather than
 *     snomedboot's {@code unzipRelease}, which takes ONE {@code ImportType} and
 *     would silently give the engines different inputs from the same package.
 * <li><b>Exclusions, expanded.</b> {@code excludedRF2Files} is honoured by the
 *     MySQL loader, so it is honoured here: a file a run excludes must not be
 *     validated, or the report would carry findings against content the caller
 *     said to leave out.
 * </ul>
 *
 * <p>The exclusion rule is reproduced exactly, quirks included, because a rule
 * that is nearly the same is worse than one that is different: it agrees on
 * every case anyone tests and diverges on the one nobody does. Specifically
 * ({@code ReleaseDataManager.isExcludedFile} and {@code loadReleaseFilesToDB}):
 *
 * <ol>
 * <li>excluding a DELTA file also excludes the matching Full and Snapshot -
 *     both the {@code Delta_} and {@code Delta-} spellings, the second being the
 *     language-suffixed files;
 * <li>a candidate has any leading {@code x} stripped, then {@code sct2} and
 *     {@code der2} are both normalised to {@code rel2}, so a package's own
 *     naming does not have to match the exclusion's;
 * <li>the exclusion entry has its {@code _YYYYMMDD.txt} suffix removed and is
 *     then matched with {@code contains}, not equality - it is a prefix rule
 *     wearing a substring rule's clothes.
 * </ol>
 */
public final class DuckReleaseUnpacker {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckReleaseUnpacker.class);

	private DuckReleaseUnpacker() {
	}

	/**
	 * Extracts {@code zip} under {@code workDirectory} and returns the directory.
	 *
	 * @param excluded the run's {@code excludedRF2Files}; null or empty means
	 *                 exclude nothing
	 */
	public static Path unpack(File zip, Path workDirectory, String name, Collection<String> excluded)
			throws IOException {
		File target = workDirectory.resolve(name).toFile();
		FileUtils.deleteDirectory(target);
		FileUtils.forceMkdir(target);
		ZipFileUtils.extractFilesFromZipToOneFolder(zip, target.getAbsolutePath());

		List<String> rules = expand(excluded);
		if (!rules.isEmpty()) {
			int removed = 0;
			File[] files = target.listFiles();
			for (File file : files == null ? new File[0] : files) {
				if (file.isFile() && isExcluded(file.getName(), rules)) {
					FileUtils.deleteQuietly(file);
					removed++;
					LOGGER.info("excluded from this validation: {}", file.getName());
				}
			}
			LOGGER.info("unpacked {} into {}, {} file(s) excluded", zip.getName(), target, removed);
		} else {
			LOGGER.info("unpacked {} into {}", zip.getName(), target);
		}
		return target.toPath();
	}

	/** An exclusion of a delta file excludes its full and snapshot too. */
	static List<String> expand(Collection<String> excluded) {
		List<String> all = new ArrayList<>();
		if (excluded == null) {
			return all;
		}
		for (String entry : excluded) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			all.add(entry);
			all.add(entry.replace("Delta_", "Full_").replace("Delta-", "Full-"));
			all.add(entry.replace("Delta_", "Snapshot_").replace("Delta-", "Snapshot-"));
		}
		return all;
	}

	/** {@code ReleaseDataManager.isExcludedFile}, restated. */
	static boolean isExcluded(String name, List<String> rules) {
		if (rules == null || rules.isEmpty()) {
			return false;
		}
		String clean = name.startsWith("x") ? name.substring(1) : name;
		String candidate = clean.replace("sct2", "rel2").replace("der2", "rel2");
		return rules.stream()
				.anyMatch(rule -> candidate.contains(rule.replaceAll("_\\d{8}.txt$", "")));
	}
}
