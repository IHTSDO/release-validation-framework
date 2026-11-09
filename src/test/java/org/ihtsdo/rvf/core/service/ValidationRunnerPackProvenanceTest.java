package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The assertion packs a phase recorded reach the merged report: the SQL phase records them,
 * the other phases record none and must not erase them.
 */
class ValidationRunnerPackProvenanceTest {

	private static final ValidationReport.AssertionPackRecord INTERNATIONAL = new ValidationReport.AssertionPackRecord(
			"international", "2026.10.13", "sha256:aaaa", "c3a6fb27", 361);
	private static final ValidationReport.AssertionPackRecord EXTENSION = new ValidationReport.AssertionPackRecord(
			"extension-pack", "2026.10.14", "sha256:bbbb", "def456", 12);

	private final ValidationRunner runner = new ValidationRunner();

	private static ValidationStatusReport phase(List<ValidationReport.AssertionPackRecord> packs) {
		ValidationStatusReport status = new ValidationStatusReport(new ValidationRunConfig());
		status.setResultReport(new ValidationReport());
		status.getResultReport().setAssertionPacks(packs);
		return status;
	}

	private void merge(ValidationStatusReport main, ValidationStatusReport phase) {
		ReflectionTestUtils.invokeMethod(runner, "mergeValidationStatusReports", main, phase);
	}

	@Test
	void theSqlPhasesPacksSurviveTheMergeWhicheverOrderPhasesFinish() {
		ValidationStatusReport main = phase(List.of());
		merge(main, phase(List.of()));
		merge(main, phase(List.of(INTERNATIONAL, EXTENSION)));
		merge(main, phase(List.of()));

		assertEquals(List.of(INTERNATIONAL, EXTENSION), main.getResultReport().getAssertionPacks());
	}

	@Test
	void phasesRecordingTheSamePacksAgree() {
		ValidationStatusReport main = phase(List.of());
		merge(main, phase(List.of(INTERNATIONAL)));
		merge(main, phase(List.of(INTERNATIONAL)));

		assertEquals(List.of(INTERNATIONAL), main.getResultReport().getAssertionPacks());
	}

	@Test
	void phasesRecordingDifferentPacksAreRefused() {
		// Either set would misstate what produced the report.
		ValidationStatusReport main = phase(List.of());
		merge(main, phase(List.of(INTERNATIONAL)));

		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> merge(main, phase(List.of(INTERNATIONAL, EXTENSION))));
		assertTrue(e.getMessage().contains("different assertion packs"), e.getMessage());
	}
}
