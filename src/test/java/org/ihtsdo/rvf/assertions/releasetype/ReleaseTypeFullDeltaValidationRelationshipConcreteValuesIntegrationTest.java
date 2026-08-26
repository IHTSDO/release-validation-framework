package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationRelationshipConcreteValuesIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-relationship-concrete-values.sql
	private static final String ASSERTION_UUID = "1b9e2371-1b0a-46d4-ac9d-fd222329ad9d";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		insertRelationshipConcreteValues(current, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationshipConcreteValues(current, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000022L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationshipConcreteValues(current, Schema.Delta.Component.RELATIONSHIP_CONCRETE_VALUES, 100000022L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(2, failureCount);
	}

	@Test
	void shouldPass_When_CurrentFullRowPresentInPreviousFull() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");
		insertRelationshipConcreteValues(previous, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260101", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationshipConcreteValues(current, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260101", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

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
		insertRelationshipConcreteValues(current, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);
		insertRelationshipConcreteValues(current, Schema.Delta.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

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
		insertRelationshipConcreteValues(current, Schema.Full.Component.RELATIONSHIP_CONCRETE_VALUES, 100000021L, "20260201", 1, RF2.Module.CORE, 100000001L, "#5", 0, RF2.Concept.IS_A, RF2.Concept.CHARACTERISTIC_TYPE_STATED, RF2.Concept.MODIFIER_EXISTENTIAL);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
