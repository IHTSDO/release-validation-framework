package org.ihtsdo.rvf.messaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQQueue;
import org.ihtsdo.rvf.autoscaling.InstanceManager;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
@Service
public class RvfValidationMessageConsumer {
	private static final String EC2_INSTANCE_ID_URL = "http://169.254.169.254/latest/meta-data/instance-id";
	private static final long TIME_TO_LIVE = 55*60*1000;
	private static final long DEFAULT_PROCESSING_TIME = 15*60*1000;
	private String queueName;
	@Autowired
	private ValidationRunner runner;
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private ConnectionFactory connectionFactory;
	private boolean isWorker;
	@Autowired
	private InstanceManager instanceManager;
	private boolean isEc2Instance;
	private static long timeStart;
	
	public RvfValidationMessageConsumer( String queueName,Boolean isRvfWorker, Boolean ec2Instance) {
		isWorker = isRvfWorker.booleanValue();
		this.queueName = queueName;
		this.isEc2Instance = ec2Instance.booleanValue();
	}
	
	public void start() {
		logger.info("isRvfWorker instance:" + isWorker);
		timeStart = Calendar.getInstance().getTimeInMillis();
		if (isWorker) {
			Thread thread = new Thread ( new Runnable() {
				
				@Override
				public void run() {
					consumeMessage();
				}
			});
			thread.start();
			logger.info("RvfWorker instance started at:" + Calendar.getInstance().getTime());
		}
		
	}
	
	private void consumeMessage() {
		Connection connection = null;
		MessageConsumer consumer = null;
		Destination destination = new ActiveMQQueue(queueName);
		Session session = null;
		try {
			connection =  connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(new MessageListener() {

				@Override
				public void onMessage(Message message) {
					if (message instanceof TextMessage) {
						runValidation((TextMessage)message);
					}

				}
			});
			
			while (!shutDown(consumer)) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					logger.error("Consumer thread is interupted", e);
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
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
					logger.error("Error when closing session.", e);
				}
			}
		}
	}
	
	public boolean shutDown(MessageConsumer consumer) {
		if (!isEc2Instance) {
			return false;
		} else {
			if ((Calendar.getInstance().getTimeInMillis() - timeStart + DEFAULT_PROCESSING_TIME  ) >= TIME_TO_LIVE) {
				logger.info("Shut down message consumer as no time left to process another one");
				try {
					consumer.close();
				} catch (JMSException e) {
					logger.error("Failed to close message consumer!", e);
				}
				autoTerminate();
				return true;
			}
			return false;
		}
		
	}
	
	private void autoTerminate() {
		List<String> instancesToTerminate = new ArrayList<>();
		try {
			RestTemplate restTemplate = new RestTemplate();
			String instanceId = restTemplate.getForObject(EC2_INSTANCE_ID_URL, String.class);
			logger.debug("Current instance id is:" + instanceId);
			if (instanceId != null) {
				instancesToTerminate.add(instanceId);
			}
		} catch(Exception e) {
			logger.error("Failed to get instance id", e);
		}
		if (!instancesToTerminate.isEmpty()) {
			 instanceManager.terminate(instancesToTerminate);
		     logger.info("Instance id  will be terminated:" + instancesToTerminate.get(0) );
		}
		
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
		if ( config != null) {
			long start = System.currentTimeMillis();
			runner.run(config);
			long end = System.currentTimeMillis();
			logger.info("last validation taken in seconds:" + (end-start) /1000);
		} else {
			logger.error("Null validation config found for message:" + incomingMessage);
		}
	}

}
