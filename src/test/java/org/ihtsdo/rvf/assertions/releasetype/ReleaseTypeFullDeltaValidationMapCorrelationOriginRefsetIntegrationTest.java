package org.ihtsdo.rvf.assertions.releasetype;

import org.ihtsdo.rvf.configuration.MySQLAssertionIntegrationTest;
import org.ihtsdo.rvf.assertions.Schema;
import org.ihtsdo.rvf.assertions.RF2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTypeFullDeltaValidationMapCorrelationOriginRefsetIntegrationTest extends MySQLAssertionIntegrationTest {
	// release-type-full-delta-validation-mapCorrelationOrigin-refset.sql
	private static final String ASSERTION_UUID = "7c37bee7-62ad-41f9-93e3-12eb4803e617";

	@Test
	void shouldFail_When_CurrentFullRowMissingFromPreviousFullAndCurrentDelta() throws Exception {
		// given
		String previous = createCodeSystemVersion("SNOMEDCT/2026-01-01");
		String current = createCodeSystemVersion("SNOMEDCT/2026-02-01");

		String referenceSetMemberA = UUID.randomUUID().toString();
		String referenceSetMemberB = UUID.randomUUID().toString();
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, referenceSetMemberA, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MAP_CORRELATION_ORIGIN, referenceSetMemberB, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000002L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);

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
		insertReferenceSetMember(previous, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, memberId, "20260101", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);
		insertReferenceSetMember(current, Schema.Delta.ReferenceSetMember.MAP_CORRELATION_ORIGIN, memberId, "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);

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
		insertReferenceSetMember(current, Schema.Full.ReferenceSetMember.MAP_CORRELATION_ORIGIN, UUID.randomUUID().toString(), "20260201", 1, RF2.Module.CORE, RF2.Refset.SAME_AS, 100000001L, RF2.Column.MAP_TARGET, "A01", RF2.Column.ATTRIBUTE_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CORRELATION_ID, RF2.Concept.CORRELATION_NOT_SPECIFIED, RF2.Column.CONTENT_ORIGIN_ID, RF2.Concept.CONTENT_ORIGIN);

		// when
		long failureCount = validate(ASSERTION_UUID, current, previous);

		// then
		assertEquals(0, failureCount);
	}
}
