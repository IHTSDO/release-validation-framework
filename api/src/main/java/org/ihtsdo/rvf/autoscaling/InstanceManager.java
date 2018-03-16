package org.ihtsdo.rvf.autoscaling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.controller.VersionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
@Service
public class InstanceManager {

	private static final String TAG = "tag:";
	private static final String WORKER_TYPE = "workerType";
	private static final String PROPERTIES = ".properties";
	private static final String RVF_CONFIG_LOCATION = "rvfConfigLocation";
	private static final String TERMINATE = "terminate";
	private static final String RUNNING = "running";
	private static final String PENDING = "pending";
	private static final String RVF_WORKER = "RVF_Worker_";
	private static final String NAME = "Name";
	private static final long TIME_TO_DELETE = 56 * 60 * 1000;
	private Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	private AmazonEC2Client amazonEC2Client;
	private static int counter;
	@Autowired
	private String imageId;
	@Autowired
	private String instanceType;
	@Autowired
	private String securityGroupId;
	@Autowired
	private String keyName;
	@Autowired
	private String instanceTagName;
	@Autowired
	private String ec2SubnetId;
	@Autowired
	private String droolsRulesVersion;
	@Autowired
	private String droolsRuelsModuleName;
	private String ec2InstanceStartupScript;

	public InstanceManager(AWSCredentials credentials, String ec2Endpoint) {
		amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint(ec2Endpoint);
		ec2InstanceStartupScript = Base64.encodeBase64String(constructStartUpScript().getBytes());
	}

	public List<String> createInstance(int totalToCreate) {
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withImageId(imageId).withInstanceType(instanceType)
				.withMinCount(1).withMaxCount(totalToCreate)
				.withKeyName(keyName)
				.withInstanceInitiatedShutdownBehavior(TERMINATE)
				.withSecurityGroupIds(securityGroupId)
				.withUserData(ec2InstanceStartupScript)
				.withSubnetId(ec2SubnetId);
		List<String> ids = new ArrayList<>();
		try {
			RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);
			List<Instance> instances = runInstancesResult.getReservation().getInstances();
			for (Instance instance : instances) {
				String instanceId = instance.getInstanceId();
				logger.info("RVF worker new instance created with id {} and launched at {}", instanceId, instance.getLaunchTime());
				CreateTagsRequest createTagsRequest = new CreateTagsRequest();
				createTagsRequest.withResources(instanceId);
				if (instanceTagName != null) {
					Tag typeTag = new Tag(WORKER_TYPE, instanceTagName);
					Tag nameTag = new Tag(NAME, RVF_WORKER + instanceTagName + "_" + counter++);
					createTagsRequest.withTags(typeTag, nameTag);
					logger.info("Name tag {} is created for instance id {}", nameTag, instanceId);
				} else {
					createTagsRequest.withTags(new Tag(NAME, RVF_WORKER + imageId + "_" + counter++));
				}
				amazonEC2Client.createTags(createTagsRequest);
				ids.add(instanceId);
			}
		} catch (Exception e) {
			logger.error("Failed to create RVF instance workers.", e);
		}
		return ids;
	}

	public Map<String,String> getPublicIpAddress(List<String> instanceIds) {
		Map<String, String> instacneIpAddressMap = new HashMap<>();
		try {
			//wait for the instance to be created otherwise the public address will be null
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
			logger.error("Failed to sleep for 30 seconds",e);
		}
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.setInstanceIds(instanceIds);

		DescribeInstancesResult result = amazonEC2Client.describeInstances(request);
		List<Reservation> reservations = result.getReservations();
		for (Reservation reservation : reservations) {
			for (Instance instance : reservation.getInstances()) {
				instacneIpAddressMap.put(instance.getInstanceId(), instance.getPublicIpAddress());
			}
		}
		return instacneIpAddressMap;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}

	public List<String> checkActiveInstances(List<String> instanceIds) {
		List<String> activeInstances = new ArrayList<>();
		if (instanceIds != null && !instanceIds.isEmpty()) {
			DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.withInstanceIds(instanceIds);
			DescribeInstancesResult result = amazonEC2Client.describeInstances(request);
			List<Reservation> reservations = result.getReservations();
			for (Reservation reserv : reservations) {
				for (Instance instance : reserv.getInstances()) {
					InstanceState state = instance.getState();
					if (PENDING.equalsIgnoreCase(state.getName())
							|| RUNNING.equalsIgnoreCase(state.getName())) {
						activeInstances.add(instance.getInstanceId());
						logger.info("Active instance {} with public ip address {}", instance.getInstanceId(), instance.getPublicIpAddress());
					}
				}
			}
		}
		logger.debug("Current total active instances:" + activeInstances.size());
		return activeInstances;
	}

	public List<String> getActiveInstances() {
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withFilters(new Filter(TAG + WORKER_TYPE, Arrays.asList(instanceTagName)));
		List<Instance> instances = new ArrayList<>();
		try {
			DescribeInstancesResult result = amazonEC2Client.describeInstances(request);
			List<Reservation> reservations = result.getReservations();
			for (Reservation reserv : reservations) {
				instances.addAll(reserv.getInstances());
			}
			logger.debug("Total instances {} found with filter {}", instances.size(), TAG + WORKER_TYPE + "=" + instanceTagName);
		} catch (Exception e) {
			String msg = "Unexpected error encountered when checking active instances.";
			logger.error(msg, e);
			throw new RuntimeException(msg, e);
		}

		List<String> activeInstances = new ArrayList<>();
		for (Instance instance : instances) {
			InstanceState state = instance.getState();
			if (PENDING.equalsIgnoreCase(state.getName())
					|| RUNNING.equalsIgnoreCase(state.getName())) {
				activeInstances.add(instance.getInstanceId());
				logger.info("Active instance {} with public ip address {}", instance.getInstanceId(), instance.getPublicIpAddress());
			}
		}
		logger.info("Total active instances:" + activeInstances.size());
		return activeInstances;
	}

	public void checkAndTerminateInstances(List<Instance> instancesToCheck) {
		List<Instance> instancesToTerminate = new ArrayList<>();
		for (Instance instance : instancesToCheck) {
			if (System.currentTimeMillis() >= (instance.getLaunchTime().getTime() + TIME_TO_DELETE)) {
				logger.info("Instance id {} was lanched at {} and will be terminated",
						instance.getInstanceId(), instance.getLaunchTime());
				instancesToTerminate.add(instance);
			}
		}
		if (!instancesToTerminate.isEmpty()) {
			List<String> instanceIds = new ArrayList<>();
			for (Instance instance : instancesToTerminate) {
				instanceIds.add(instance.getInstanceId());
			}
			TerminateInstancesRequest deleteRequest = new TerminateInstancesRequest();
			deleteRequest.withInstanceIds(instanceIds);
			TerminateInstancesResult result = amazonEC2Client
					.terminateInstances(deleteRequest);
			for (InstanceStateChange state : result.getTerminatingInstances()) {
				logger.info("Instance id {} current state {}", state
						.getInstanceId(), state.getCurrentState().getName());
			}
			instancesToCheck.removeAll(instancesToTerminate);
		}
	}

	public Instance getInstanceById(String instanceId) {
		if (instanceId == null) {
			logger.warn("instanceId is null");
			return null;
		}
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(instanceId);
		DescribeInstancesResult result = amazonEC2Client
				.describeInstances(request);
		return result.getReservations().get(0).getInstances().get(0);
	}

	public TerminateInstancesResult terminate(List<String> instancesToTerminate) {
		TerminateInstancesRequest deleteRequest = new TerminateInstancesRequest();
		deleteRequest.withInstanceIds(instancesToTerminate);
		return amazonEC2Client.terminateInstances(deleteRequest);
	}

	private Map<String, String> getProperties(String rvfConfig) {
		File configDir = new File(rvfConfig);
		String[] propertyFiles = null;
		if (configDir.isDirectory()) {
			propertyFiles = configDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(PROPERTIES);
				}
			});
		}
		Map<String, String> propertiesByFileName = new HashMap<>();
		if (propertyFiles != null) {
			for (String fileName : propertyFiles) {
				List<String> lines = new ArrayList<>();
				try {
					lines.addAll(IOUtils.readLines(new FileReader(new File(
							configDir, fileName))));
				} catch (IOException e) {
					logger.error("Error when reading proerty file:" + fileName,
							e);
				}
				StringBuilder result = new StringBuilder();
				for (String line : lines) {
					// set ec2 instance worker properties
					if (line.startsWith("rvf.execution.isWorker")) {
						result.append("rvf.execution.isWorker=true");
					} else if (line
							.startsWith("rvf.execution.isAutoScalingEnabled")) {
						result.append("rvf.execution.isAutoScalingEnabled=false");
					} else if (line.startsWith("rvf.autoscaling.isEc2Instance")) {
						result.append("rvf.autoscaling.isEc2Instance=true");
					} else {
						result.append(line);
					}
					result.append("\n");
				}
				propertiesByFileName.put(fileName, result.toString());
			}
		}
		return propertiesByFileName;
	}

	private String constructStartUpScript() {
		String appVersion = getAppVersion();
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		builder.append("sudo apt-get update -o Dir::Etc::sourcelist=\"sources.list.d/maven_ihtsdotools_org_content_repositories_*\""
				+ "\n");
		if (appVersion != null && !appVersion.isEmpty()) {
			builder.append("sudo apt-get install --force-yes -y rvf-api="
					+ appVersion + "\n");
		} else {
			builder.append("sudo apt-get install --force-yes -y rvf-api \n");
		}
		builder.append("sudo dpkg -s rvf-api\n");
		String rvfConfig = System.getProperty(RVF_CONFIG_LOCATION);
		Map<String, String> propertyStrByFilename = getProperties(rvfConfig);
		for (String filename : propertyStrByFilename.keySet()) {
			builder.append("sudo echo \"" + propertyStrByFilename.get(filename)
					+ "\" > " + rvfConfig + "/" + filename + "\n");
		}
		// checkout drools version
		builder.append("sudo git clone");
		if (droolsRulesVersion != null && !droolsRulesVersion.isEmpty()) {
			//git clone -b 'v1.9'
			builder.append(" -b " +"'v" + droolsRulesVersion + "'");
			builder.append(" --single-branch");
		}
		//"--single-branch https://github.com/IHTSDO/snomed-drools-rules.git /opt/snomed-drools-rules/)
		builder.append(" https://github.com/IHTSDO/snomed-drools-rules.git ");
		if (droolsRuelsModuleName == null || droolsRuelsModuleName.isEmpty() || !droolsRuelsModuleName.startsWith("/")) {
			droolsRuelsModuleName= "/opt/snomed-drools-rules/";
		}
		builder.append(droolsRuelsModuleName + "\n");
		builder.append("sudo chown -R rvf-api:rvf-api " + droolsRuelsModuleName + "\n");
		builder.append("sudo supervisorctl start rvf-api" + "\n");
		builder.append("exit 0");
		return builder.toString();
	}

	private String getAppVersion() {
		String version = null;
		File file = new File(VersionController.VERSION_FILE_PATH);
		if (file.isFile()) {
			try (BufferedReader bufferedReader = new BufferedReader(
					new FileReader(file))) {
				version = bufferedReader.readLine();
			} catch (IOException e) {
				logger.error("Error to read the version number from file.", e);
			}
		}
		return version;
	}
}
