package org.ihtsdo.rvf.autoscaling;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoScalingManager {
	
	
	private static final String SIZE = "size";
	private String brokerUrl;
	private boolean shutDown;
	private Logger logger = Logger.getLogger(AutoScalingManager.class);
	@Autowired
	InstanceManager instanceManager;
	private boolean isAutoScalling;
	private ConnectionFactory connectionFactory;
	private String queueName;
	private long lastPolledQueueSize;
	
	public AutoScalingManager( String jmsBrokerUrl, Boolean isAutoScalling, String destinationQueueName) {
		this.isAutoScalling = isAutoScalling.booleanValue();
		brokerUrl = jmsBrokerUrl;
		connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		queueName = "ActiveMQ.Statistics.Destination."+ destinationQueueName; 
	}
	public void startUp() {
		logger.info("isAutoScalingEnabled:" + isAutoScalling);
		if ( isAutoScalling) {
			Thread thread = new Thread( new Runnable() {
				@Override
				public void run() {
					while (!shutDown) {
						long current = getQueueSize();
						if (current > lastPolledQueueSize ){
							//will add logic later in terms how many instances need to create for certain size
							// the current approach is to create one instance per message.
							logger.info("Messages have been increated by:" + (lastPolledQueueSize-current) + " since last poll.");
							lastPolledQueueSize = current;
							logger.info("Start creating new worker instance...");
							instanceManager.createInstance();
						}
						try {
							Thread.sleep(2*60000);
						} catch (InterruptedException e) {
							logger.error("AutoScalingManager delay is interrupted.", e);
						}
					}
				}
			});
			thread.start();
		}
	}
	
	/**
	 * monitoring the queue size and create new instance when there is message
	 */
	private long getQueueSize() {
		logger.debug("Retrieving queue size....");
		long result = 0;
		
		Connection connection = null;
		try {
			connection = connectionFactory.createConnection();
			connection.start();
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        Queue replyTo = session.createTemporaryQueue();
	        MessageConsumer consumer = session.createConsumer(replyTo);
	        Queue testQueue = session.createQueue(queueName);
	        MessageProducer producer = session.createProducer(testQueue);
	        Message msg = session.createMessage();
	        msg.setJMSReplyTo(replyTo);
	        producer.send(msg);
	        MapMessage reply = (MapMessage) consumer.receive(10000);
	        if (reply != null) {
	        	result = reply.getLong(SIZE);
	 	        logger.info("Queue size:" + result);
	        }
		} catch (JMSException e) {
			logger.error("Error in sending statistics message", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					logger.error("Error in closing queue connection", e);
				}
			}
		}
		 return result;
	}
		
	public void shutDown() {
		this.shutDown = true;
	}
	
}
