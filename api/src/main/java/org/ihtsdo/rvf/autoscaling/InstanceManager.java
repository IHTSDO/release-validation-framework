package org.ihtsdo.rvf.autoscaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
@Service
public class InstanceManager {
	
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
	private String ec2Endpoint;
	
	public InstanceManager(AWSCredentials credentials) {
		amazonEC2Client = new AmazonEC2Client(credentials);
	}
	
	public RunInstancesResult createInstance() {
		amazonEC2Client.setEndpoint(ec2Endpoint);
		RunInstancesRequest runInstancesRequest = 
				  new RunInstancesRequest();
			
			runInstancesRequest.withImageId(imageId)
			                     .withInstanceType(instanceType)
			                     .withMinCount(1)
			                     .withMaxCount(1)
			  					 .withSecurityGroupIds(securityGroupId);
			  					 
			  RunInstancesResult runInstancesResult = 
					  amazonEC2Client.runInstances(runInstancesRequest);

			  String instanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
			  logger.info("RVF worker new instance created with id:" + instanceId);
			  CreateTagsRequest createTagsRequest = new CreateTagsRequest();
			  createTagsRequest.withResources(instanceId);
			  createTagsRequest.withTags(new Tag( "Name", "RVF_Worker" + counter++));
			  amazonEC2Client.createTags(createTagsRequest);
			  return runInstancesResult;
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

//	public String getKeyName() {
//		return keyName;
//	}
//
//	public void setKeyName(String keyName) {
//		this.keyName = keyName;
//	}
//
//	public String getSecurityGroupName() {
//		return securityGroupName;
//	}
//
//	public void setSecurityGroupName(String securityGroupName) {
//		this.securityGroupName = securityGroupName;
//	}

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}
	public String getEc2Endpoint() {
		return ec2Endpoint;
	}

	public void setEc2Endpoint(String ec2Endpoint) {
		this.ec2Endpoint = ec2Endpoint;
	}
}
