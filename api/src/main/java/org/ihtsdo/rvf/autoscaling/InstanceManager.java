package org.ihtsdo.rvf.autoscaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

public class InstanceManager {
	
	private Logger logger = LoggerFactory.getLogger(InstanceManager.class);
	private  AmazonEC2Client amazonEC2Client;
	private static int counter;
	
	public InstanceManager(AWSCredentials credentials) {
		amazonEC2Client = new AmazonEC2Client(credentials);
		amazonEC2Client.setEndpoint("ec2.us-west-2.amazonaws.com");
	}

	public RunInstancesResult createInstance() {
		RunInstancesRequest runInstancesRequest = 
				  new RunInstancesRequest();
			        	
			  runInstancesRequest.withImageId("ami-d00412b1")
			                     .withInstanceType("t2.micro")
			                     .withMinCount(1)
			                     .withMaxCount(1)
			                     .withKeyName("WestDevops")
			                     .withSecurityGroups("SSH_HTTPS")
			  					 .withSecurityGroupIds("sg-5624cd32");
			  					 
			  RunInstancesResult runInstancesResult = 
					  amazonEC2Client.runInstances(runInstancesRequest);

			  String instanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
			  logger.info("RVF worker new instance created with id:" + instanceId);
			  CreateTagsRequest createTagsRequest = new CreateTagsRequest();
			  createTagsRequest.withResources(instanceId);
			  createTagsRequest.withTags(new Tag ( "Name", "RVF_Worker" + counter++));
			  amazonEC2Client.createTags(createTagsRequest);
			  return runInstancesResult;
	}	
}
