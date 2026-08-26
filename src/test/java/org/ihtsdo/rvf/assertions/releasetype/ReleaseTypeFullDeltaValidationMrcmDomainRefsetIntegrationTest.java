package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationMrcmDomainRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-mrcm-domain-refset.sql
	private static final String ASSERTION_UUID = "43bdeea9-be7b-4235-b58e-8842a8e47793";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_DOMAIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");

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
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MRCM_DOMAIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MRCM_DOMAIN, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.DOMAIN_CONSTRAINT, "<<404684003", RF2.Column.PARENT_DOMAIN, "<<138875005", RF2.Column.PROXIMAL_PRIMITIVE_CONSTRAINT, "<<71388002", RF2.Column.PROXIMAL_PRIMITIVE_REFINEMENT, "none", RF2.Column.DOMAIN_TEMPLATE_FOR_PRECOORDINATION, "pre", RF2.Column.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION, "post", RF2.Column.GUIDE_URL, "https://snomed.info/doc");

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
