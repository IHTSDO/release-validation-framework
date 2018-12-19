//package org.ihtsdo.rvf.messaging;
//
//import javax.jms.JMSException;
//import javax.jms.TextMessage;
//import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
//import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jms.annotation.JmsListener;
//import org.springframework.stereotype.Service;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonSyntaxException;
//
//@Service
//public class ReleaseValidationMessageListener {
//	@Autowired
//	private ValidationRunner runner;
//	private Logger logger = LoggerFactory.getLogger(getClass());
//
//	@JmsListener(destination = "${rvf.validation.queue.name}")
//	public void triggerValidation(final TextMessage incomingMessage) {
//		logger.info("Received message {}", incomingMessage);
//		runValidation(incomingMessage);
//	}
//
//	private void runValidation(final TextMessage incomingMessage) {
//		Gson gson = new Gson();
//		ValidationRunConfig config = null;
//		try {
//			config = gson.fromJson(incomingMessage.getText(),
//					ValidationRunConfig.class);
//			logger.info("validation config from queue:" + config);
//		} catch (JsonSyntaxException | JMSException e) {
//			logger.error("JMS message listener error:", e);
//		}
//		if (config != null) {
//			runner.run(config);
//		} else {
//			logger.error("Null validation config found for message:" + incomingMessage);
//		}
//	}
//}
