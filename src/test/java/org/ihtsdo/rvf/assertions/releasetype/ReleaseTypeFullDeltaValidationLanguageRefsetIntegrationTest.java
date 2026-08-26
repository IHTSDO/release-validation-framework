package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationLanguageRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-language-refset.sql
	private static final String ASSERTION_UUID = "28c3a31d-7a9d-48f6-8eb9-18c25b6306fb";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.LANGUAGE, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.LANGUAGE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.LANGUAGE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);

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
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.LANGUAGE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.LANGUAGE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.LANGUAGE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.LANGUAGE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.LANGUAGE, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.ACCEPTABILITY_ID, RF2.Concept.ACCEPTABILITY_PREFERRED);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
