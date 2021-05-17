package org.ihtsdo.rvf.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.rvf.execution.service.ValidationReportService;
import org.ihtsdo.rvf.execution.service.ValidationRunner;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "rvf.execution.isWorker", havingValue = "true")
public class ValidationMessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationMessageListener.class);
	
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	
	@Autowired
	private ValidationRunner runner;

	@Autowired
	private MessagingHelper messagingHelper;
	
	@JmsListener(destination = "${rvf.validation.queue.name}")
	public void processMessage(TextMessage incomingMessage) {
		isRunning.set(true);
		LOGGER.info("Validation message received {}", incomingMessage);
		runValidation(incomingMessage);
		isRunning.set(false);
	}
	
	private void runValidation(final TextMessage incomingMessage) {
		Gson gson = new Gson();
		ValidationRunConfig config = null;
		try {
			config = gson.fromJson(incomingMessage.getText(), ValidationRunConfig.class);
			LOGGER.info("validation config:" + config);
		} catch (JsonSyntaxException | JMSException e) {
			LOGGER.error("JMS message listener error:", e);
		}
		if (config != null) {
			updateRvfStateToRunning(config);
			runner.run(config);
		} else {
			LOGGER.error("Null validation config found for message: {}", incomingMessage);
		}
	}

	private void updateRvfStateToRunning(final ValidationRunConfig config) {
		try {
			messagingHelper.send(config.getResponseQueue(),
					ImmutableMap.of("runId", config.getRunId(),
							"state", ValidationReportService.State.RUNNING.name()));
		} catch (JsonProcessingException | JMSException e) {
			LOGGER.error("Error occurred while trying to update the RVF state to running.", e);
		}
	}

	public static boolean isValidationRunning() {
		return isRunning.get();
	}
}
