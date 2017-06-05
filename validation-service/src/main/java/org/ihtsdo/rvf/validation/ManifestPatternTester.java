package org.ihtsdo.rvf.validation;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.model.FileElement;
import org.ihtsdo.rvf.validation.model.Folder;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;

public class ManifestPatternTester {

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
			report.addError("0", startTime, MANIFEST, MANIFEST, MANIFEST, MANIFEST_STRUCTURE_TEST, "", "No Manifest File Found", MANIFEST,null);
		} else {
			final List<Folder> folders = manifestFile.getListing().getFolders();
			final AtomicInteger folderCounter = new AtomicInteger(0);
			final AtomicInteger fileCounter = new AtomicInteger(0);
			testStructure(folders, folderCounter, fileCounter, startTime);
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
				report.addError(folderCounter + "-" + fileCounter, startTime, name, name, name, MANIFEST_STRUCTURE_TEST, "", "No Folder Found", name,null);
			} else {
				report.addSuccess(folderCounter + "-" + fileCounter, startTime, name, name, "", MANIFEST_STRUCTURE_TEST, "");
			}
			final List<FileElement> files = folder.getFiles();
			for (final FileElement file : files) {
				fileCounter.incrementAndGet();
				final String filename = file.getFileName();
				if (!(resourceManager.match(filename))) {
					validationLog.assertionError("Invalid package structure expected file at {} but found none", filename);
					report.addError(folderCounter + "-" + fileCounter, startTime, filename, filename, filename, MANIFEST_STRUCTURE_TEST, "", "No File Found", filename,null);
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
}
