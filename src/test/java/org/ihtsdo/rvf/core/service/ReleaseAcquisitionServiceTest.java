package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ReleaseAcquisitionService#createExecutionConfig} is a pure mapping from
 * the run request to the execution config, so it can be pinned without a
 * database, a module store or a Spring context.
 */
class ReleaseAcquisitionServiceTest {

	private final ReleaseAcquisitionService service = new ReleaseAcquisitionService();

	@Test
	void copiesTheRunRequestOntoTheExecutionConfig() {
		ValidationRunConfig run = new ValidationRunConfig();
		run.setRunId(42L);
		run.setGroupsList(List.of("int-authoring"));
		run.setPreviousRelease("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip");
		run.setEffectiveTime("2025-02-01");
		run.setDefaultModuleId("900000000000207008");
		run.setIncludedModules(" 900000000000207008 , 449080006");
		run.addRF2DeltaOnly(true);

		MysqlExecutionConfig config = service.createExecutionConfig(run);

		assertEquals(42L, config.getExecutionId());
		assertEquals(List.of("int-authoring"), config.getGroupNames());
		assertEquals("2025-02-01", config.getEffectiveTime());
		assertEquals("20250101", config.getPreviousEffectiveTime());
		assertEquals("900000000000207008", config.getDefaultModuleId());
		assertEquals(List.of("900000000000207008", "449080006"), config.getIncludedModules());
		assertTrue(config.isRf2DeltaOnly());
		assertFalse(config.isExtensionValidation());
		assertEquals(10, config.getFailureExportMax(), "failure export defaults to 10");
	}

	@Test
	void readsThePreviousEffectiveTimeFromAVersionNameAsWellAsAPackageName() {
		ValidationRunConfig run = new ValidationRunConfig();
		run.setRunId(1L);
		run.setPreviousRelease("int_20240701");

		assertEquals("20240701", service.createExecutionConfig(run).getPreviousEffectiveTime());
	}

	@Test
	void aFirstTimeReleaseHasNoPreviousEffectiveTime() {
		ValidationRunConfig run = new ValidationRunConfig();
		run.setRunId(1L);
		run.setFirstTimeRelease(true);
		run.setPreviousRelease("int_20240701");

		MysqlExecutionConfig config = service.createExecutionConfig(run);

		assertTrue(config.isFirstTimeRelease());
		assertNull(config.getPreviousEffectiveTime());
	}

	@Test
	void anExtensionCarriesEachDependencysPreviousEffectiveTime() {
		ValidationRunConfig run = new ValidationRunConfig();
		run.setPreviousRelease("xx_20240701");
		run.setRunId(1L);
		run.setFailureExportMax(100);
		run.addExtensionDependency("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip");
		run.addCurrentDependencyToIdentifyingModuleMap("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", "900000000000012004");
		run.addPreviousDependencyEffectiveTime("900000000000012004", "20240701");

		MysqlExecutionConfig config = service.createExecutionConfig(run);

		assertTrue(config.isExtensionValidation());
		assertEquals(100, config.getFailureExportMax());
		assertEquals(Map.of("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", "20240701"),
				config.getCurrentDependencyToPreviousEffectiveTimeMap());
	}
}
