package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationMrcmAttributeRangeRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-mrcm-attribute-range-refset.sql
	private static final String ASSERTION_UUID = "ad0f8978-c8a7-4d91-8e3e-a887e4830fec";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		String memberId = UUID.randomUUID().toString();
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		String memberId = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_RANGE, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.RANGE_CONSTRAINT, "<<404684003", RF2.Column.ATTRIBUTE_RULE, "rule1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
