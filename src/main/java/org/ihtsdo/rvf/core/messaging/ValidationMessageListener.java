package org.ihtsdo.rvf.core.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.ValidationRunner;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusResponse;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "rvf.execution.isWorker", havingValue = "true")
public class ValidationMessageListener implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationMessageListener.class);

	static final String SHADOW_STORAGE_SUFFIX = "-duck";
	
	private static final AtomicBoolean isRunning = new AtomicBoolean(false);

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
	
	@Autowired
	private ValidationRunner runner;

	@Autowired
	private MessagingHelper messagingHelper;

	@Value("${rvf.validation.shadow:false}")
	private boolean shadow;

	@JmsListener(destination = "${rvf.validation.queue.name}")
	public void processMessage(TextMessage incomingMessage) {
		isRunning.set(true);
		LOGGER.info("Validation message received {}", incomingMessage);
		runValidation(incomingMessage);
		isRunning.set(false);
	}
	
	private void runValidation(final TextMessage incomingMessage) {
		Gson gson = new Gson();
		ValidationRunConfig config;
		try {
			config = gson.fromJson(incomingMessage.getText(), ValidationRunConfig.class);
			LOGGER.info("validation config:" + config);
		} catch (JsonSyntaxException | JMSException e) {
			throw new RuntimeException("Error occurred while trying to convert the incoming message.", e);
		}
		if (config != null) {
			if (shadow) {
				shadow(config);
			}
			updateRvfStateToRunningAsync(config);
			runner.run(config);
		} else {
			LOGGER.error("Null validation config found for message: {}", incomingMessage);
		}
	}

	// Must run before anything reads responseQueue or storageLocation: the caller is owed status
	// only by the worker that owns the run, and its reports must not be overwritten.
	private static void shadow(ValidationRunConfig config) {
		config.addResponseQueue(null);
		config.setStorageLocation(config.getStorageLocation() + SHADOW_STORAGE_SUFFIX);
		LOGGER.info("Shadow run {}: no status to the response queue, reports to {}", config.getRunId(), config.getStorageLocation());
	}

	private void updateRvfStateToRunningAsync(final ValidationRunConfig config) {
		EXECUTOR_SERVICE.execute(() -> {
			final String responseQueue = config.getResponseQueue();
			if (responseQueue != null) {
				try {
					LOGGER.info("Updating RVF state to running: {}", responseQueue);
					messagingHelper.send(responseQueue, new ValidationStatusResponse(config, ValidationReportService.State.RUNNING));
				} catch (JMSException e) {
					throw new RuntimeException("Error occurred while trying to update the RVF state to running.", e);
				}
			}
		});
	}

	public static boolean isValidationRunning() {
		return isRunning.get();
	}

	@Override
	public void close() {
		EXECUTOR_SERVICE.shutdown();
	}
}
