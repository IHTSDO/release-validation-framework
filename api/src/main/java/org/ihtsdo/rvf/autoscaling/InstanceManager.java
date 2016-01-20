package org.ihtsdo.rvf.autoscaling;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
@Service
public class InstanceManager {
	
	private static final String TERMINATE = "terminate";
	private static final String RUNNING = "running";
	private static final String PENDING = "pending";
	private static final String RVF_WORKER = "RVF_Worker_";
	private static final String NAME = "Name";
	private static final long TIME_TO_DELTE = 56*60*1000;
	private Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	private  AmazonEC2Client amazonEC2Client;
	private static int counter;
	@Autowired
	private String imageId;
	@Autowired
	private String instanceType;
	@Autowired
	private String securityGroupId;
	@Autowired
	private String keyName;
	private String instanceTagName;
	
	public InstanceManager(AWSCredentials credentials, String ec2Endpoint ) {
		amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint(ec2Endpoint);
	}
	
	public Instance createInstance() {
		RunInstancesRequest runInstancesRequest = 
				  new RunInstancesRequest();
			
			runInstancesRequest.withImageId(imageId)
			                     .withInstanceType(instanceType)
			                     .withMinCount(1)
			                     .withMaxCount(1)
			                     .withKeyName(keyName)
			                     .withInstanceInitiatedShutdownBehavior(TERMINATE)
			  					 .withSecurityGroupIds(securityGroupId);
			  RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

			  Instance instance = runInstancesResult.getReservation().getInstances().get(0);
			  String instanceId = instance.getInstanceId();
			  logger.info("RVF worker new instance created with id {} and launched at {}", instanceId, instance.getLaunchTime());
			  CreateTagsRequest createTagsRequest = new CreateTagsRequest();
			  createTagsRequest.withResources(instanceId);
			  if ( instanceTagName != null) {
				  createTagsRequest.withTags(new Tag( NAME, RVF_WORKER + instanceTagName + "_" + counter++));
			  } else {
				  createTagsRequest.withTags(new Tag( NAME, RVF_WORKER + imageId + "_" + counter++));
			  }
			  amazonEC2Client.createTags(createTagsRequest);
			  return instance;
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
	
	public int getActiveInstances(List<Instance> instancesToCheck) {
		//check instances that are in pending or running status
		int totalRunning = 0;
		DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();
		List<String> instanceIds = new ArrayList<>();
		for (Instance instance : instancesToCheck) {
			instanceIds.add(instance.getInstanceId());
		}
		describeInstanceStatusRequest.withInstanceIds(instanceIds);
		DescribeInstanceStatusResult result = amazonEC2Client.describeInstanceStatus(describeInstanceStatusRequest);
		List<InstanceStatus> statusList = result.getInstanceStatuses();
		for (InstanceStatus status : statusList) {
			InstanceState state = status.getInstanceState();
			if (state != null) {
				if (PENDING.equalsIgnoreCase(state.getName()) || RUNNING.equalsIgnoreCase(state.getName())) {
					totalRunning++;
				}
			}
		}
		return totalRunning;
		
	}
	public void checkAndTerminateInstances(List<Instance> instancesToCheck) {
		  List<Instance> instancesToTerminate = new ArrayList<>();
		  for (Instance instance : instancesToCheck) {
			  if ( System.currentTimeMillis() >= (instance.getLaunchTime().getTime() + TIME_TO_DELTE)) {
				  logger.info("Instance id {} was lanched at {} and will be terminated", instance.getInstanceId(),instance.getLaunchTime());
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
			  TerminateInstancesResult result = amazonEC2Client.terminateInstances(deleteRequest);
			  for (InstanceStateChange state :  result.getTerminatingInstances()) {
				  logger.info("Instance id {} current state {}", state.getInstanceId(), state.getCurrentState().getName());
			  }
			  instancesToCheck.removeAll(instancesToTerminate);
		  }
	}
	
	public Instance getInstanceById(String instanceId) {
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(instanceId);
		DescribeInstancesResult result = amazonEC2Client.describeInstances();
		return result.getReservations().get(0).getInstances().get(0);
	}

	public TerminateInstancesResult terminate(List<String> instancesToTerminate) {
		TerminateInstancesRequest deleteRequest = new TerminateInstancesRequest();
		 deleteRequest.withInstanceIds(instancesToTerminate);
		 return amazonEC2Client.terminateInstances(deleteRequest);
	}
}
