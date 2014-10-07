package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContext.xml"})
@Transactional
public class AssertionServiceImplTest {

	@Autowired
	private AssertionService assertionService;
    @Autowired
    private EntityService entityService;
    private Assertion assertion;
    private ReleaseCenter releaseCenter;
    private AssertionTest assertionTest;
    private org.ihtsdo.rvf.entity.Test test;

    @Test
	@Rollback
	public void testCreate() throws Exception {
//		Assertion assertion = assertionService.create("Assertion 1", new HashMap<String, String>());
		assertNotNull("id should be set", assertion.getId());
	}

	@Test
	@Rollback
	public void testFindAll() throws Exception {
		try {
			assert assertionService.findAll().contains(assertion);
		} catch (Exception e) {
			fail("No exception expected but got " + e.getMessage());
		}

	}

    @Before
    public void setUp() {
        // ensure database is clean
        assert entityService.findAll(org.ihtsdo.rvf.entity.Test.class).size() == 0;
        assert entityService.findAll(AssertionTest.class).size() == 0;
        assert entityService.findAll(ReleaseCenter.class).size() == 0;
        assert entityService.findAll(Assertion.class).size() == 0;

        assertion = assertionService.create("Assertion 1", new HashMap<String, String>());
        // create test
        test = new org.ihtsdo.rvf.entity.Test();
        test.setName("Test 1");
        test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
        assert test != null;
        assert test.getId() != null;
        assert entityService.findAll(org.ihtsdo.rvf.entity.Test.class).size() > 0;

        // create release centre
        releaseCenter = new ReleaseCenter();
        releaseCenter.setName("Test release centre 1");
        releaseCenter = (ReleaseCenter) entityService.create(releaseCenter);
        assert releaseCenter != null;
        assert releaseCenter.getId() != null;
        assert entityService.findAll(releaseCenter).size() > 0;

        //create assertion test
        assertionTest = new AssertionTest();
        assertionTest.setAssertion(assertion);
        assertionTest.setTest(test);
        assertionTest.setCenter(releaseCenter);
        assertionTest = (AssertionTest) entityService.create(assertionTest);
        assert assertionTest != null;
        assert assertionTest.getId() != null;
        assert entityService.findAll(AssertionTest.class).size() > 0;
    }

    @Test
    @Rollback
    public void testFindAssertionTests() throws Exception {

        // use service to find assertion tests associated with assertion
        List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion, releaseCenter);
        assert  assertionTests != null;
        assert assertionTests.size() > 0;
        assert assertionTests.contains(assertionTest);
    }

    @Test
    @Rollback
    public void testFindTests() throws Exception {

        // use service to find tests associated with assertion
        List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion, releaseCenter);
        assert  tests != null;
        assert tests.size() > 0;
        assert tests.contains(test);
    }

    @Test
    @Rollback
    public void testFindAssertionTestsByIds() throws Exception {

        // use service to find assertion tests associated with assertion
        List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion.getId(), releaseCenter.getId());
        assert  assertionTests != null;
        assert assertionTests.size() > 0;
        assert assertionTests.contains(assertionTest);
    }

    @Test
    @Rollback
    public void testFindTestsByIds() throws Exception {

        // use service to find tests associated with assertion
        List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion.getId(), releaseCenter.getId());
        assert  tests != null;
        assert tests.size() > 0;
        assert tests.contains(test);
    }
}
