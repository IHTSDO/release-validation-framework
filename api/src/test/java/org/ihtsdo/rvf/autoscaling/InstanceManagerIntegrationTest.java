package org.ihtsdo.rvf.autoscaling;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.ec2.model.Instance;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class InstanceManagerIntegrationTest {
	@Autowired
	InstanceManager instanceManager;
	
	@Test
	public void testCreateEc2Instance() {
		Instance instance = instanceManager.createInstance();
		Assert.assertNotNull(instance);
		Assert.assertNotNull(instance.getInstanceId());
		System.out.println("Instance is created with id:" + instance.getInstanceId());
		List<Instance> result = instanceManager.getActiveInstances();
		Assert.assertNotNull(result);
	}
}
