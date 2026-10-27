package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationConceptIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-concept.sql
	private static final String ASSERTION_UUID = "bbfbeae4-4b58-465a-9c16-1e8c4454384c";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		insertConcept(current, Schema.Full.Component.CONCEPT, 100000001L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);
		insertConcept(current, Schema.Full.Component.CONCEPT, 100000002L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);
		insertConcept(current, Schema.Delta.Component.CONCEPT, 100000002L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);

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
		insertConcept(previous, Schema.Full.Component.CONCEPT, 100000001L, "20260101", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);
		insertConcept(current, Schema.Full.Component.CONCEPT, 100000001L, "20260101", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);

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
		insertConcept(current, Schema.Full.Component.CONCEPT, 100000001L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);
		insertConcept(current, Schema.Delta.Component.CONCEPT, 100000001L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);

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
		insertConcept(current, Schema.Full.Component.CONCEPT, 100000001L, "20260201", 1, RF2.Module.CORE, RF2.Concept.DEFINITION_STATUS_PRIMITIVE);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
