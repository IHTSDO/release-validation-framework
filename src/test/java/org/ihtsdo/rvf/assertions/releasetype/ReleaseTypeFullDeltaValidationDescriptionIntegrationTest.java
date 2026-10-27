package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationDescriptionIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-description.sql
	private static final String ASSERTION_UUID = "7c4165b0-bff2-4b62-b70c-f68d5a147053";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		insertDescription(current, Schema.Full.Component.DESCRIPTION, 100000011L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);
		insertDescription(current, Schema.Full.Component.DESCRIPTION, 100000012L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);
		insertDescription(current, Schema.Delta.Component.DESCRIPTION, 100000012L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(1, failureCount);
	}

	@Test
	void shouldPass_When_CurrentFullRowPresentInPreviousFull() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");
		insertDescription(previous, Schema.Full.Component.DESCRIPTION, 100000011L, "20260101", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);
		insertDescription(current, Schema.Full.Component.DESCRIPTION, 100000011L, "20260101", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}

	@Test
	void shouldPass_When_CurrentFullRowPresentInCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");
		insertDescription(current, Schema.Full.Component.DESCRIPTION, 100000011L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);
		insertDescription(current, Schema.Delta.Component.DESCRIPTION, 100000011L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}

	@Test
	void shouldPass_When_DeltaIsEmpty() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");
		insertDescription(current, Schema.Full.Component.DESCRIPTION, 100000011L, "20260201", 1, RF2.Module.CORE, 100000001L, "en", RF2.Concept.DESCRIPTION_TYPE_FSN, "Test description", RF2.Concept.CASE_SIGNIFICANCE_CASE_INSENSITIVE);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
