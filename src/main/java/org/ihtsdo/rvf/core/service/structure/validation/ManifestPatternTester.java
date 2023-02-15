package org.ihtsdo.rvf.core.service.structure.validation;

import org.ihtsdo.rvf.core.service.structure.listing.FileElement;
import org.ihtsdo.rvf.core.service.structure.listing.Folder;
import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ManifestPatternTester {
	public static final String FULL = "Full";
	public static final String SNAPSHOT = "Snapshot";
	private static final String MANIFEST_STRUCTURE_TEST = "ManifestPackageStructureTest";
	private static final String MANIFEST = "manifest.xml";
	private final ValidationLog validationLog;
	private final ResourceProvider resourceManager;
	private final ManifestFile manifestFile;
	private final TestReportable report;

	public ManifestPatternTester(final ValidationLog validationLog, final ResourceProvider resourceManager, final ManifestFile manifestFile,
								 final TestReportable report) {
		this.validationLog = validationLog;
		this.resourceManager = resourceManager;
		this.manifestFile = manifestFile;
		this.report = report;
	}

	public void runTests() {
		final Date startTime = new Date();
		if (manifestFile == null || manifestFile.getListing() == null) {
			validationLog.assertionError("Manifest file expected but not found.");
			report.addError("0", startTime, MANIFEST, MANIFEST, MANIFEST, MANIFEST_STRUCTURE_TEST, "", "No Manifest File Found", MANIFEST, null);
		} else {
			final List<Folder> folders = manifestFile.getListing().getFolders();
			final AtomicInteger folderCounter = new AtomicInteger(0);
			final AtomicInteger fileCounter = new AtomicInteger(0);
			testStructure(folders, folderCounter, fileCounter, startTime);
			testManifestStructure(folders, startTime);
			validationLog.info("Manifest file structure testing for {} files and {} folders completed in {} milliseconds.", fileCounter, folderCounter, (new Date().getTime() - startTime.getTime()));
		}
	}


	private void testStructure(final List<Folder> folders, final AtomicInteger folderCounter, final AtomicInteger fileCounter, final Date startTime) {
		if (folders.isEmpty()) return;
		for (final Folder folder : folders) {
			folderCounter.incrementAndGet();
			final String name = folder.getFolderName();
			final boolean match = resourceManager.match(name);
			if (!match) {
				validationLog.assertionError("Invalid package structure expected directory at {} but found none", name);
				report.addError(folderCounter + "-" + fileCounter, startTime, name, name, name, MANIFEST_STRUCTURE_TEST, "", "No Folder Found", name, null);
			} else {
				report.addSuccess(folderCounter + "-" + fileCounter, startTime, name, name, "", MANIFEST_STRUCTURE_TEST, "");
			}
			final List<FileElement> files = folder.getFiles();
			for (final FileElement file : files) {
				fileCounter.incrementAndGet();
				String filename = file.getFileName();
				if (!Normalizer.isNormalized(filename, Form.NFC)) {
					filename = Normalizer.normalize(filename, Form.NFC);
				}
				if (!(resourceManager.match(filename))) {
					validationLog.assertionError("Invalid package structure expected file at {} but found none", filename);
					report.addError(folderCounter + "-" + fileCounter, startTime, filename, filename, filename, MANIFEST_STRUCTURE_TEST, "", "No File Found", filename, null);
				} else {
					report.addSuccess(folderCounter + "-" + fileCounter, startTime, filename, filename, "", MANIFEST_STRUCTURE_TEST, "");
				}
			}
			final List<Folder> children = folder.getFolders();
			if (!children.isEmpty()) {
				testStructure(children, folderCounter, fileCounter, startTime);
			}
		}
	}

	private void testManifestStructure(final List<Folder> folders, final Date startTime) {
		List<Folder> folderTypes = folders.get(0).getFolders();
		List<String> snapshotFiles = new ArrayList<>();
		List<String> fullFiles = new ArrayList<>();
		for (Folder folderType : folderTypes) {
			String folderName = folderType.getName();
			if (SNAPSHOT.equals(folderName)) {
				getFilesByFolder(folderType, snapshotFiles);
			} else if (FULL.equals(folderName)) {
				getFilesByFolder(folderType, fullFiles);
			}
		}
		if (!snapshotFiles.isEmpty() && !fullFiles.isEmpty()) {
			List<String> missingFilesInFullFolder = snapshotFiles.stream()
					.filter(file -> !fullFiles.contains(file.replace("Snapshot_", "Full_").replace("Snapshot-", "Full-")))
					.collect(Collectors.toList());
			List<String> missingFilesInSnapshotFolder = fullFiles.stream()
					.filter(file -> !snapshotFiles.contains(file.replace("Full_", "Snapshot_").replace("Full-", "Snapshot-")))
					.collect(Collectors.toList());
			for (String filename : missingFilesInFullFolder) {
				validationLog.assertionError("Invalid file expected file {} in Snapshot folder but not found in Full folder", filename);
				report.addError("0-0", startTime, filename, filename, filename, MANIFEST_STRUCTURE_TEST, "", "No File Found in Full folder", filename, null);
			}
			for (String filename : missingFilesInSnapshotFolder) {
				validationLog.assertionError("Invalid file expected file {} in Full folder but not found in Snapshot folder", filename);
				report.addError("0-0", startTime, filename, filename, filename, MANIFEST_STRUCTURE_TEST, "", "No File Found in Snapshot folder", filename, null);
			}
		}
	}

	public void getFilesByFolder(Folder folder, List<String> filesList) {
		if (folder.getFiles() != null) {
			for (FileElement fileType : folder.getFiles()) {
				filesList.add(fileType.getName());
			}
		}
		if (folder.getFolders() != null) {
			for (Folder subFolder : folder.getFolders()) {
				getFilesByFolder(subFolder, filesList);
			}
		}
	}
}
