package org.ihtsdo.rvf.autoscaling;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoScalingManager {
	
	
	private static final String SIZE = "size";
	private boolean shutDown;
	private Logger logger = Logger.getLogger(AutoScalingManager.class);
	@Autowired
	private InstanceManager instanceManager;
	private boolean isAutoScalling;
	@Autowired
	private ConnectionFactory connectionFactory;
	private String queueName;
	private static int lastPolledQueueSize;
	
	private final int MAX_INSTANCE = 1;
	
	private static int totalInstanceCreated;
	
	public AutoScalingManager( Boolean isAutoScalling, String destinationQueueName) {
		this.isAutoScalling = isAutoScalling.booleanValue();
		queueName = destinationQueueName;
	}
	public void startUp() throws Exception {
		logger.info("isAutoScalingEnabled:" + isAutoScalling);
		if (isAutoScalling) {
			Thread thread = new Thread( new Runnable() {
				@Override
				public void run() {
					while (!shutDown) {
						int current = getQueueSize();
						if (current > lastPolledQueueSize ){
							//will add logic later in terms how many instances need to create for certain size
							// the current approach is to create one instance per message.
							logger.info("Messages have been increated by:" + (current - lastPolledQueueSize) + " since last poll.");
							lastPolledQueueSize = current;
							if (totalInstanceCreated < MAX_INSTANCE) {
								logger.info("Start creating new worker instance...");
								long start = System.currentTimeMillis();
								instanceManager.createInstance();
								logger.info("Time taken to create new intance in seconds:" + (System.currentTimeMillis() - start)/1000);
								totalInstanceCreated++;
							} else {
								//limit to 1 for the time being while testing.
								logger.info("No new instance will be created as total instance created:" + totalInstanceCreated + " has reached max:" + MAX_INSTANCE);
							}
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
	
	
	
	private int getQueueSize() {
		logger.debug("Retrieving message size in queue:" + queueName);
		int counter =0;
		Connection connection = null;
		try {
			connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue tempQueue = session.createQueue(queueName);
			QueueBrowser browser = session.createBrowser(tempQueue);
			Enumeration<?> enumerator = browser.getEnumeration();
			while (enumerator.hasMoreElements()) {
				enumerator.nextElement();
				counter++;
			}
		} catch (JMSException e) {
			logger.error("Error when checking message size in queue:" + queueName, e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					logger.error("Error in closing queue connection", e);
				}
			}
		}
		logger.info("Total messages in queue:" + counter);
		return counter;
	}
	/**
	 * queueName = "ActiveMQ.Statistics.Destination."+ destinationQueueName; 
	 * monitoring the queue size and create new instance when there is message
	 */
	private long getQueueSizeViaStatisticsBroker() {
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
	 	        consumer.close();
	        } else {
	        	logger.info("No reply from queue:" + queueName + " after 10 seconds");
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
