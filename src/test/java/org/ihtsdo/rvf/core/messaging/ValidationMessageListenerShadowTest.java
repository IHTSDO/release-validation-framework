package org.ihtsdo.rvf.core.messaging;

import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.rvf.core.service.ValidationRunner;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.jms.TextMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidationMessageListenerShadowTest {

	private static final String MESSAGE = """
			{"runId": 1763467200000, "storageLocation": "international/20261118/run-1",
			 "responseQueue": "dev-termserver-release-validation.response",
			 "prospectiveFileFullPath": "international/20261118/run-1/files-to-validate/release.zip"}""";

	private final ValidationRunner runner = mock(ValidationRunner.class);
	private final MessagingHelper messagingHelper = mock(MessagingHelper.class);
	private final ValidationMessageListener listener = new ValidationMessageListener();

	@BeforeEach
	void wire() {
		ReflectionTestUtils.setField(listener, "runner", runner);
		ReflectionTestUtils.setField(listener, "messagingHelper", messagingHelper);
	}

	private ValidationRunConfig receive(boolean shadow) throws Exception {
		ReflectionTestUtils.setField(listener, "shadow", shadow);
		TextMessage message = mock(TextMessage.class);
		when(message.getText()).thenReturn(MESSAGE);
		listener.processMessage(message);
		ArgumentCaptor<ValidationRunConfig> run = ArgumentCaptor.forClass(ValidationRunConfig.class);
		verify(runner).run(run.capture());
		return run.getValue();
	}

	@Test
	void aShadowRunSendsNoStatusAndReportsToItsOwnLocation() throws Exception {
		ValidationRunConfig config = receive(true);

		assertNull(config.getResponseQueue());
		assertEquals("international/20261118/run-1-duck", config.getStorageLocation());
		assertEquals(1763467200000L, config.getRunId());
		assertEquals("international/20261118/run-1/files-to-validate/release.zip", config.getProspectiveFileFullPath());
		// RUNNING is sent asynchronously, so wait long enough for it to have been sent
		verify(messagingHelper, after(500).never()).send(anyString(), any());
	}

	@Test
	void anOwnedRunIsUnchanged() throws Exception {
		ValidationRunConfig config = receive(false);

		assertEquals("dev-termserver-release-validation.response", config.getResponseQueue());
		assertEquals("international/20261118/run-1", config.getStorageLocation());
		verify(messagingHelper, timeout(2000)).send(eq("dev-termserver-release-validation.response"), any(ValidationStatusResponse.class));
	}
}
