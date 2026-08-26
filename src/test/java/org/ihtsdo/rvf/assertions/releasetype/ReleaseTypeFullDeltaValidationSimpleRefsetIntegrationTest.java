package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationSimpleRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-simple-refset.sql
	private static final String ASSERTION_UUID = "b6926fdf-e24a-49ae-a50a-c7c972944804";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.SIMPLE, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.SIMPLE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.SIMPLE, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L);

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
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.SIMPLE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.SIMPLE, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.SIMPLE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.SIMPLE, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.SIMPLE, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
