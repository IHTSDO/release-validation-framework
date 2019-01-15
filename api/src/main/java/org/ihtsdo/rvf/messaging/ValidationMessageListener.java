package org.ihtsdo.rvf.messaging;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.ihtsdo.rvf.execution.service.ValidationRunner;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@Service
@ConditionalOnProperty(name = "rvf.execution.isWorker", havingValue = "true")
public class ValidationMessageListener {
	
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	
	@Autowired
	private ValidationRunner runner;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@JmsListener(destination = "${rvf.validation.queue.name}")
	public void processMessage(TextMessage incomingMessage) {
		isRunning.set(true);
		logger.info("Validation message received {}", incomingMessage);
		runValidation(incomingMessage);
		isRunning.set(false);
	}
	
	private void runValidation(final TextMessage incomingMessage) {
		Gson gson = new Gson();
		ValidationRunConfig config = null;
		try {
			config = gson.fromJson(incomingMessage.getText(), ValidationRunConfig.class);
			logger.info("validation config:" + config);
		} catch (JsonSyntaxException | JMSException e) {
			logger.error("JMS message listener error:", e);
		}
		if (config != null) {
			runner.run(config);
		} else {
			logger.error("Null validation config found for message:" + incomingMessage);
		}
	}
	
	public static boolean isValidationRunning() {
		return isRunning.get();
	}
}
