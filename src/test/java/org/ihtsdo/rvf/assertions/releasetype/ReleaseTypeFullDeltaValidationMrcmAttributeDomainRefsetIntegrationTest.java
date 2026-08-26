package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationMrcmAttributeDomainRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-mrcm-attribute-domain-refset.sql
	private static final String ASSERTION_UUID = "7c3fe37c-6ad5-446e-bc82-7c2bc078042a";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_ATTRIBUTE_DOMAIN, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_ID, RF2.Concept.MRCM_ATTRIBUTE_DOMAIN_ALL, RF2.Column.GROUPED, 1, RF2.Column.ATTRIBUTE_CARDINALITY, "0..*", RF2.Column.ATTRIBUTE_IN_GROUP_CARDINALITY, "0..1", RF2.Column.RULE_STRENGTH_ID, RF2.Concept.RULE_STRENGTH_MANDATORY, RF2.Column.CONTENT_TYPE_ID, RF2.Concept.CONTENT_TYPE_ALL);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
