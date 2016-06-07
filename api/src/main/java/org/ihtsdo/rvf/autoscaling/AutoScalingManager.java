package org.ihtsdo.rvf.autoscaling;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoScalingManager {
	
	private boolean shutDown;
	private Logger logger = Logger.getLogger(AutoScalingManager.class);
	@Autowired
	private InstanceManager instanceManager;
	private boolean isAutoScalling;
	@Autowired
	private ConnectionFactory connectionFactory;
	private String queueName;
	private static int lastPolledQueueSize;
	
	private int maxRunningInstance;
	
	private static List<String> activeInstances;
	
	private boolean isFirstTime = true;
	
	public AutoScalingManager( Boolean isAutoScalling, String destinationQueueName, Integer maxRunningInstance) {
		this.isAutoScalling = isAutoScalling.booleanValue();
		queueName = destinationQueueName;
		activeInstances = new ArrayList<>();
		this.maxRunningInstance = maxRunningInstance;
		
	}
	public void startUp() throws Exception {
		logger.info("isAutoScalingEnabled:" + isAutoScalling);
		if (isAutoScalling) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!shutDown) {
						if (!isFirstTime) {
							int current = getQueueSize();
							if (current != lastPolledQueueSize) {
								logger.info("Total messages in queue:" + current);
							}
							activeInstances = instanceManager.checkActiveInstances(activeInstances);
							if ((current > lastPolledQueueSize) || (current > activeInstances.size())) {
								//will add logic later in terms how many instances need to create for certain size
								// the current approach is to create one instance per message.
								if (current > lastPolledQueueSize) {
									logger.info("Messages have been increased by:" + (current - lastPolledQueueSize) + " since last poll.");
								}
								int totalToCreate = getTotalInstancesToCreate(current, activeInstances.size(), maxRunningInstance);
								if (totalToCreate != 0) {
									logger.info("Start creating " + totalToCreate + " new worker instance");
									long start = System.currentTimeMillis();
									activeInstances.addAll(instanceManager.createInstance(totalToCreate));
									logger.info("Time taken to create new intance in seconds:" + (System.currentTimeMillis() - start) / 1000);
								}
							} 
							lastPolledQueueSize = current;
							try {
								Thread.sleep(30 * 1000);
							} catch (InterruptedException e) {
								logger.error("AutoScalingManager delay is interrupted.", e);
							}
						} else {
							isFirstTime = false;
							// check any running instances
							activeInstances.addAll(instanceManager.getActiveInstances());
						}
					}
				}
			});
			thread.start();
		}
	}
	
	private int getTotalInstancesToCreate(int currentMsgSize, int activeInstances, int maxRunningInstance) {
		int result = 0;
		if (activeInstances < maxRunningInstance) {
			if (currentMsgSize <= activeInstances) {
				logger.info("No new instance will be created as message size is:" + currentMsgSize);
			} else {
				if (currentMsgSize <= maxRunningInstance) {
					result = currentMsgSize - activeInstances;
				} else {
					result = maxRunningInstance - activeInstances;
				}
			}
		} else {
			logger.info("No new instance will be created as total running instances:" + activeInstances + " has reached max:" + maxRunningInstance);
		}
		return result;
	}
	
	private int getQueueSize() {
		int counter = 0;
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
		return counter;
	}
	
	public void shutDown() {
		this.shutDown = true;
	}
	
}
