package org.ihtsdo.rvf.messaging;

import java.io.File;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RvfValidationMessageConsumer {
	
	
	private String queueUrl = "tcp://localhost:61616";
	private String queueName = "TEST.QUEUE";
	@Autowired
	private ValidationRunner runner;
	private Logger logger = LoggerFactory.getLogger(getClass());
	public void consumeMessage() {

		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(queueUrl);
		
		Destination destination = new ActiveMQQueue(queueName);
		Connection connection = null;
		try {
			connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageConsumer consumer = session.createConsumer(destination);
			TextMessage message = (TextMessage) consumer.receive();
			
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
				runner.run(config);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		

	}

}
