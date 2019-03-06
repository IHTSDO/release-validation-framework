package org.ihtsdo.rvf.autoscaling;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.ihtsdo.rvf.messaging.ValidationMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

@Service
public class RvfWorkerLifecycleManager {

	private static final String EC2_INSTANCE_ID_URL = "http://169.254.169.254/latest/meta-data/instance-id";
	private static final long HALF_HOUR_IN_MILLIS = 30 * 60 * 1000;
	private static final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
	
	private Logger logger = LoggerFactory.getLogger(RvfWorkerLifecycleManager.class);
	
	@Autowired
	private InstanceManager instanceManager;
	
	@Value("${rvf.execution.isWorker}")
	private boolean isWorker;
	
	@Value("${rvf.autoscaling.isEc2Instance}")
	private boolean isEc2Instance;
	
	private ExecutorService executorService;
	
	private Instance instance;
	
	@PostConstruct
	public void init() {
		if (isEc2Instance && isWorker) {
			logger.info("Start instance worker shutdown monitoring thread.");
			executorService = Executors.newSingleThreadExecutor();
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					while (!shutDown()) {
						try {
							Thread.sleep(5 * ONE_MINUTE_IN_MILLIS);
							logger.info("Checking time...");
						} catch (InterruptedException e) {
							logger.error("Consumer thread is interupted", e);
							executorService.shutdown();
						}
					}
				}
			});
		}
	}
	
	public boolean shutDown() {
		if (!isEc2Instance) {
			return false;
		} else {
			if (instance == null) {
				instance = instanceManager.getInstanceById(getInstanceId());
			}
			if (!ValidationMessageListener.isValidationRunning()) {
				// only shutdown when no message to process and close to the hourly mark
				long timeTaken = Calendar.getInstance().getTimeInMillis() - instance.getLaunchTime().getTime();
				if (timeTaken >= HALF_HOUR_IN_MILLIS) {
					logger.info("Shut down instance message consumer as no messages left to process in queue.");
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

}
