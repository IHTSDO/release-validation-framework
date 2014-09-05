package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContext.xml"})
@Transactional
public class AssertionServiceImplTest {

	@Autowired
	private AssertionService assertionService;

	@Test
	@Rollback
	public void testCreate() throws Exception {
		Assertion assertion = assertionService.create("test 1", new HashMap<String, String>());
		assertNotNull("id shoould be set", assertion.getId());
	}

	@Test
	@Rollback
	public void testFindAll() throws Exception {
		try {
			assertionService.findAll();
		} catch (Exception e) {
			fail("No exception expected but got " + e.getMessage());
		}

	}

}
