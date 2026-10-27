package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationStatedRelationshipIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-stated-relationship.sql
	private static final String ASSERTION_UUID = "b1d75e1c-ea12-4567-a8b9-f69923a57cdf";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		insertRelationship(current, Schema.Full.Component.STATED_RELATIONSHIP, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationship(current, Schema.Full.Component.STATED_RELATIONSHIP, 100000022L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationship(current, Schema.Delta.Component.STATED_RELATIONSHIP, 100000022L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

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
		insertRelationship(previous, Schema.Full.Component.STATED_RELATIONSHIP, 100000021L, "20260101", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationship(current, Schema.Full.Component.STATED_RELATIONSHIP, 100000021L, "20260101", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

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
		insertRelationship(current, Schema.Full.Component.STATED_RELATIONSHIP, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationship(current, Schema.Delta.Component.STATED_RELATIONSHIP, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

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
		insertRelationship(current, Schema.Full.Component.STATED_RELATIONSHIP, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, 100000002L, 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
