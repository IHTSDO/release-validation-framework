package org.ihtsdo.rvf.messaging;

import java.io.File;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
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
	
	
	private String brokerUrl;
	private String queueName;
	@Autowired
	private ValidationRunner runner;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean shutDown;
	private ConnectionFactory connectionFactory;
	private boolean isWorker;
	
	public RvfValidationMessageConsumer( String brokerUrl, String queueName,Boolean isRvfWorker) {
		this.brokerUrl = brokerUrl;
		isWorker = isRvfWorker.booleanValue();
		connectionFactory = new ActiveMQConnectionFactory(this.brokerUrl);
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
		}
	}
	
	private void consumeMessage() {
	
		Destination destination = new ActiveMQQueue(queueName);
		Connection connection = null;
		while ( !shutDown) {
			try {
				connection = connectionFactory.createConnection();
				connection.start();
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				MessageConsumer consumer = session.createConsumer(destination);
				
				Message msg = consumer.receive(10000);
				if ( msg == null) {
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
								if (config.getProspectiveFilePath() != null) {
									config.setProspectiveFile(new File(config.getProspectiveFilePath()));
								}
								logger.info("Running validaiton:" + config.toString());
//								runner.run(config);
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
			}
		}
	}
	
	public void shutDown() {
		this.shutDown = true;
	}
}
