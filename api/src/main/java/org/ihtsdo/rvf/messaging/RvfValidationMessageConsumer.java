package org.ihtsdo.rvf.messaging;

import java.util.Calendar;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQQueue;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
@Service
public class RvfValidationMessageConsumer {
	private static final long TIME_TO_LIVE = 55*60*1000;
	private static final long DEFAULT_PROCESSING_TIME = 20*60*1000;
	private String queueName;
	@Autowired
	private ValidationRunner runner;
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private ConnectionFactory connectionFactory;
	private boolean isWorker;
	
	private static long timeStart;
	
	public RvfValidationMessageConsumer( String queueName,Boolean isRvfWorker) {
		isWorker = isRvfWorker.booleanValue();
		this.queueName = queueName;
	}
	
	public void start() {
		logger.info("isRvfWorker instance:" + isWorker);
		if (isWorker) {
			Thread thread = new Thread ( new Runnable() {
				
				@Override
				public void run() {
					consumeMessage();
				}
			});
			thread.start();
			timeStart = Calendar.getInstance().getTimeInMillis();
		}
	}
	
	private void consumeMessage() {
	
		Destination destination = new ActiveMQQueue(queueName);
		Connection connection = null;
		MessageConsumer consumer = null;
		try {
			connection =  connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			consumer = session.createConsumer(destination);
			while (!shutDown()) {
				Message msg = consumer.receive(30000);
				if ( msg == null) {
					logger.debug("No message received for destination:" + queueName);
					continue;
				}
				if ( msg instanceof TextMessage) {
					logger.info("Message received.");
					TextMessage message = (TextMessage) msg;
					Gson gson = new Gson();
					ValidationRunConfig config = null;
					try {
						config = gson.fromJson(message.getText(), ValidationRunConfig.class);
						logger.info("validation config from queue:" + config);
					} catch (JsonSyntaxException | JMSException e) {
						logger.error("JMS message listener error:", e);
					}
					if ( config != null) {

						logger.info("Running validaiton:" + config.toString());
						runner.run(config);								
					}
				}
			}

			} catch (JMSException e) {
				logger.error("Error when consuming RVF validaiton message.", e);
			} finally {
				if (connection != null) {
					try {
						connection.close();
					} catch (JMSException e) {
						logger.error("Error when closing message queue connection.", e);
					}
				}
				if (consumer != null) {
					try {
						consumer.close();
					} catch (JMSException e) {
						logger.error("Error when closing message consumer.", e);
					}
				}
			}
	}
	
	public boolean shutDown() {
		if ((Calendar.getInstance().getTimeInMillis() - timeStart + DEFAULT_PROCESSING_TIME  )  >=  TIME_TO_LIVE) {
			logger.info("Shut down as no time left to processing another one");
			return true;
		}
		return false;
	}
}
