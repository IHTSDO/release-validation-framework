package org.ihtsdo.rvf.core.service;

import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A deployment with no module store must still validate an edition that names
 * its own modules.
 *
 * <p>Dependency resolution asks the module store for packages matching the
 * release's MDRS, and the store answers by listing its directory. When that
 * directory does not exist, {@code File.listFiles()} returns null and the
 * listing dies with {@code NullPointerException: Cannot read the array length
 * because "array" is null}. {@code getDependencies} only lists when
 * {@code includedModules} is supplied, and {@code getAllReleases} always lists,
 * so a run fails in acquisition before any assertion executes.
 *
 * <p>Both paths are exercised against a REAL coordinator over a missing
 * directory rather than a stub, because the defect is in how the real one
 * reports an absent store.
 */
class ValidationVersionLoaderModuleStoreTest {

	private static final String INCLUDED_MODULES = "32506021000036107,351000168100";

	/*
	 * Relative, under target/: ResourceConfiguration strips a leading '/', so an
	 * absolute path would silently resolve relative to the working directory.
	 */
	private final Path root = Path.of("target", "module-store-test-" + System.nanoTime());

	@AfterEach
	void cleanUp() throws IOException {
		if (Files.isDirectory(root)) {
			try (var paths = Files.walk(root)) {
				paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
	}

	@Test
	void prospectiveDependenciesResolveToNoneWhenTheModuleStoreIsAbsent() throws IOException {
		ValidationVersionLoader service = loaderWithAbsentModuleStore();
		ValidationRunConfig config = editionRun();
		config.setLocalProspectiveFile(editionRelease(root.resolve("prospective.zip")).toFile());

		assertDoesNotThrow(() -> service.downloadDependencyReleases(config));
		assertEquals(null, config.getLocalReleaseFiles(),
				"an unreadable module store must resolve no dependencies, not invent any");
	}

	@Test
	void aPreviousReleaseFromReleaseStorageLoadsWhenTheModuleStoreIsAbsent() throws IOException {
		ValidationVersionLoader service = loaderWithAbsentModuleStore();
		String previous = "SnomedCT_Previous_AU1000036_20260831.zip";
		editionRelease(root.resolve("releases").resolve(previous));
		ValidationRunConfig config = editionRun();
		config.setPreviousRelease(previous);

		assertDoesNotThrow(() -> service.downloadPreviousRelease(config));
		assertFalse(config.getLocalReleaseFiles() == null || config.getLocalReleaseFiles().isEmpty(),
				"the previous release must still be fetched from release storage");
	}

	private ValidationVersionLoader loaderWithAbsentModuleStore() {
		Path missing = root.resolve("no-module-store");
		ResourceConfiguration moduleStore = new ManualResourceConfiguration(true, false,
				new ResourceConfiguration.Local(missing + "/"), null);
		ModuleStorageCoordinator coordinator = ModuleStorageCoordinator.initDev(
				new ResourceManager(moduleStore, new DefaultResourceLoader()));

		ValidationReleaseStorageConfig releaseStorage = new ValidationReleaseStorageConfig();
		releaseStorage.setReadonly(true);
		releaseStorage.setUseCloud(false);
		releaseStorage.setLocal(new ResourceConfiguration.Local(root.resolve("releases") + "/"));

		ValidationVersionLoader service = new ValidationVersionLoader();
		ReflectionTestUtils.setField(service, "moduleStorageCoordinator", coordinator);
		ReflectionTestUtils.setField(service, "releaseStorageConfig", releaseStorage);
		ReflectionTestUtils.setField(service, "cloudResourceLoader", new DefaultResourceLoader());
		ReflectionTestUtils.setField(service, "emptyRf2Filename", "empty-rf2-snapshot.zip");
		service.init();
		return service;
	}

	private static ValidationRunConfig editionRun() {
		ValidationRunConfig config = new ValidationRunConfig();
		config.setRunId(System.nanoTime());
		config.setReleaseAsAnEdition(true);
		config.setIncludedModules(INCLUDED_MODULES);
		return config;
	}

	/** An AU edition's MDRS: the extension depends on itself and on the international core. */
	private static Path editionRelease(Path zip) throws IOException {
		Files.createDirectories(zip.getParent());
		String header = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tsourceEffectiveTime\ttargetEffectiveTime\n";
		String rows = "294d2491-c6ec-44e6-80a7-fca084ca3285\t20260930\t1\t32506021000036107\t900000000000534007\t32506021000036107\t20260930\t20260930\n"
				+ "b7200801-f4f9-4dd0-90af-ab220b56f344\t20260930\t1\t32506021000036107\t900000000000534007\t900000000000207008\t20260930\t20260901\n";
		try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
			zipOut.putNextEntry(new ZipEntry("SnomedCT_Test_AU1000036_20260930/Snapshot/Refset/Metadata/"
					+ "der2_ssRefset_ModuleDependencySnapshot_AU1000036_20260930.txt"));
			zipOut.write((header + rows).getBytes(StandardCharsets.UTF_8));
			zipOut.closeEntry();
		}
		return zip;
	}
}
