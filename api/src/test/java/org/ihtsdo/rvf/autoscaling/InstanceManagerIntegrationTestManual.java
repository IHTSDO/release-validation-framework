package org.ihtsdo.rvf.autoscaling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDispatcherServletContext.xml"})
@WebAppConfiguration
@Transactional
public class InstanceManagerIntegrationTestManual {
	@Autowired
	InstanceManager instanceManager;

	@Test
	@Ignore
	public void testCreateEc2Instance() {
		List<String> instance = instanceManager.createInstance(1);
		Assert.assertNotNull(instance);
		System.out.println("Instance is created with id:" + instance.get(0));
		List<String> result = instanceManager.getActiveInstances();
		Assert.assertNotNull(result);
		instanceManager.terminate( Arrays.asList("i-e3021268"));
	}


	@Test
	@Ignore
	public void testGetInstanceStatus() {
		List<String> instanceIds = new ArrayList<>();
		instanceIds.add("i-8720390c");
		instanceIds.add("i-98233a13");
		List<String> result = instanceManager.checkActiveInstances(instanceIds);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, instanceIds.size());
	}
}
