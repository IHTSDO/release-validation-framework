package org.ihtsdo.rvf.autoscaling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.rvf.controller.VersionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
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
	private Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	private AmazonEC2Client amazonEC2Client;
	private static int counter;
	private String ec2InstanceStartupScript;
	
	@Value("${rvf.autoscaling.imageId}")
	private String imageId;
	
	@Value("${rvf.autoscaling.instanceType}")
	private String instanceType;
	
	@Value("${rvf.autoscaling.securityGroupId}")
	private String securityGroupId;
	
	@Value("${rvf.autoscaling.keyPairName}")
	private String keyName;
	
	@Value("${rvf.autoscaling.tagName}")
	private String instanceTagName;
	
	@Value("${rvf.autoscaling.ec2SubnetId}")
	private String ec2SubnetId;

	@Value("${rvf.drools.rule.version}")	 
	private String droolsRulesVersion;
	
	@Value("${rvf.drools.rule.directory}")	 
	private String droolsRulesDirectory;
	
	@Value("${rvf.drools.rule.repository}")
	private String droolsRulesRepository;

	@Value("${rvf.drools.rule.branch}")
	private String droolsRulesBranch;
	
	@Value("${aws.key}")
	private String awsPubicKey;
	
	@Value("${aws.privateKey}")
	private String awsPrivateKey;
	
	@Value("${rvf.autoscaling.ec2Endpoint}")
	private String serviceEndpoint;
	
	@Value("${rvf.autoscaling.ec2SigningRegion}")
	private String signingRegion;
	
	@Value("${rvf.execution.isAutoScalingEnabled}")
	private boolean isAutoScallingEnabled;
	
	@Value("${rvf.autoscaling.isEc2Instance}")
	private boolean isEc2Instance;
	
	@Value("${rvf.autoscaling.profile.roleName}")
	private String instanceProfileRoleName;
	
	@PostConstruct
	public void init() {
		if (isAutoScallingEnabled || isEc2Instance) {
			AWSCredentials credentials = new BasicAWSCredentials(awsPubicKey, awsPrivateKey);
			amazonEC2Client = (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(credentials))
					.withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, signingRegion)).build();
		}
		if (isAutoScallingEnabled) {
			ec2InstanceStartupScript = Base64.encodeBase64String(constructStartUpScript().getBytes());
		}
	}

	public List<String> createInstance(int totalToCreate) {
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withImageId(imageId).withInstanceType(instanceType)
				.withMinCount(1).withMaxCount(totalToCreate)
				.withKeyName(keyName)
				.withInstanceInitiatedShutdownBehavior(TERMINATE)
				.withSecurityGroupIds(securityGroupId)
				.withUserData(ec2InstanceStartupScript)
				.withSubnetId(ec2SubnetId)
				.withIamInstanceProfile(new IamInstanceProfileSpecification().withName(instanceProfileRoleName));
		
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

	public Instance getInstanceById(String instanceId) {
		if (instanceId == null) {
			logger.warn("instanceId is null");
			return null;
		}
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(instanceId);
		DescribeInstancesResult result = amazonEC2Client.describeInstances(request);
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
				try (Reader reader = new FileReader(new File(configDir, fileName));) {
					lines.addAll(IOUtils.readLines(reader));
				} catch (IOException e) {
					logger.error("Error when reading proerty file:" + fileName, e);
				}
				StringBuilder result = new StringBuilder();
				for (String line : lines) {
					// set ec2 instance worker properties
					if (line.startsWith("rvf.execution.isWorker")) {
						result.append("rvf.execution.isWorker=true");
					} else if (line.startsWith("rvf.execution.isAutoScalingEnabled")) {
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
		if(StringUtils.isNotBlank(droolsRulesBranch)) {
			//git clone -b master --single-branch
			builder.append(" -b " +"'" + droolsRulesBranch + "'");
			builder.append(" --single-branch");
		} else {
			if (StringUtils.isNotBlank(droolsRulesVersion)) {
				//git clone -b 'v1.9'
				builder.append(" -b " +"'" + droolsRulesVersion + "'");
				builder.append(" --single-branch");
			} else {
				//default to master branch if there is no specific configuration
				builder.append(" -b master --single-branch");
			}
		}
		//"--single-branch https://github.com/IHTSDO/snomed-drools-rules.git /opt/snomed-drools-rules/)
		builder.append(" " + droolsRulesRepository + " ");
		if (droolsRulesDirectory == null || droolsRulesDirectory.isEmpty() || !droolsRulesDirectory.startsWith("/")) {
			droolsRulesDirectory= "/opt/snomed-drools-rules/";
		}
		builder.append(droolsRulesDirectory + "\n");
		builder.append("sudo chown -R rvf-api:rvf-api " + droolsRulesDirectory + "\n");
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
