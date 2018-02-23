package org.ihtsdo.rvf.messaging;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@Service
public class RvfValidationMessageConsumer {
	private static final String CONSUMER_PREFETCH_SIZE = "?consumer.prefetchSize=1";
	private static final String EC2_INSTANCE_ID_URL = "http://169.254.169.254/latest/meta-data/instance-id";
	private static final long FITY_NINE_MINUTES = 59 * 60 * 1000;
	private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;
	private static final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
	private String queueName;
	@Autowired
	private ValidationRunner runner;
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private ConnectionFactory connectionFactory;
	@Autowired
	private InstanceManager instanceManager;
	
	private boolean isWorker;
	private boolean isEc2Instance;
	private Instance instance;
	private boolean isValidationRunning = false;
	private ExecutorService executorService;

	public RvfValidationMessageConsumer(String queueName, Boolean isRvfWorker, Boolean ec2Instance) {
		isWorker = isRvfWorker.booleanValue();
		this.queueName = queueName;
		this.isEc2Instance = ec2Instance.booleanValue();
	}

	@PostConstruct
	public void init() {
		logger.info("isRvfWorker instance:" + isWorker);
		if (isWorker) {
			executorService = Executors.newSingleThreadExecutor();
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					consumeMessage();
				}
			});
			executorService.shutdown();
			logger.info("RvfWorker instance started at:" + Calendar.getInstance().getTime());
		}
	}

	private void consumeMessage() {
		Connection connection = null;
		MessageConsumer consumer = null;
		Destination destination = new ActiveMQQueue(queueName + CONSUMER_PREFETCH_SIZE);
		Session session = null;
		try {
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					if (message instanceof TextMessage) {
						runValidation((TextMessage) message);
					}
				}
			});
			while (!shutDown()) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					logger.error("Consumer thread is interupted", e);
				}
			}

		} catch (Throwable t) {
			logger.error("Error when consuming RVF validaiton message.", t);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
				if (consumer != null) {
					consumer.close();
				}
				if (session != null) {
					session.close();
				}
			} catch (JMSException e) {
				logger.error("Error when closing message queue connection and session.", e);
			}
		}
	}

	public boolean shutDown() {
		if (!isEc2Instance) {
			return false;
		} else {
			if (instance == null) {
				instance = instanceManager.getInstanceById(getInstanceId());
			}
			if (!isValidationRunning) {
				// only shutdown when no message to process and close to the hourly mark
				long timeTaken = Calendar.getInstance().getTimeInMillis() - instance.getLaunchTime().getTime();
				if ((timeTaken % HOUR_IN_MILLIS) >= FITY_NINE_MINUTES) {
					logger.info("Shut down instance message consumer as no messages left to process in queue and it is approaching to hourly mark.");
					logger.info("Instance total running time in minutes:" + (timeTaken / ONE_MINUTE_IN_MILLIS));
					logger.info("Instance will be terminated with id:" + instance.getInstanceId());
					boolean isTerminated = false;
					int counter = 0;
					while (!isTerminated && counter++ < 3) {
						try {
							TerminateInstancesResult result = instanceManager.terminate(Arrays.asList(instance.getInstanceId()));
							InstanceState state = result.getTerminatingInstances().get(0).getCurrentState();
							if ("running".equals(state.getName())) {
								logger.error("Instance has not been shutdown yet");
								isTerminated = false;
							} else {
								logger.info("Instance is terminated");
								isTerminated = true;
							}
						} catch (Exception e) {
							logger.error("Error when shutting down instance", e);
							isTerminated = false;
						}
					}
					return true;
				}
			}
			return false;
		}
	}

	private String getInstanceId() {
		String instanceId = null;
		try {
			RestTemplate restTemplate = new RestTemplate();
			instanceId = restTemplate.getForObject(EC2_INSTANCE_ID_URL, String.class);
			logger.info("Current instance id is:" + instanceId);
		} catch (Exception e) {
			logger.error("Failed to get instance id", e);
		}
		return instanceId;
	}

	private void runValidation(final TextMessage incomingMessage) {
		isValidationRunning = true;
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
		isValidationRunning = false;
	}
	
	@PreDestroy
	public void cleanUp() {
	  if (executorService != null) {
		  executorService.shutdownNow();
	  }
	}
}
