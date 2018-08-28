package org.ihtsdo.rvf.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testDataServiceContext.xml"})
@Transactional
public class AssertionServiceImplTest {

	@Autowired
	private AssertionService assertionService;
	@Autowired
	private EntityService entityService;
	private Assertion assertion;
	private AssertionTest assertionTest;
	private org.ihtsdo.rvf.entity.Test test;

	@Test
	@Rollback
	public void testCreate() throws Exception {
		assertNotNull("id should be set", assertion.getAssertionId());
	}

	@Test
	@Rollback
	public void testFindAll() throws Exception {
		try {
			assert assertionService.findAll().contains(assertion);
		} catch (final Exception e) {
			fail("No exception expected but got " + e.getMessage());
		}

	}

	@Before
	public void setUp() {
		assert entityService != null;
		assert assertionService != null;
		assertion = new Assertion();
		assertion.setAssertionText("Test assertion");
		assertion = assertionService.create(assertion);
		assertNotNull(assertion.getAssertionId());
		assertNotNull(assertion.getUuid());

		// create test
		test = new org.ihtsdo.rvf.entity.Test();
		test.setName("Test 1");
		test = (org.ihtsdo.rvf.entity.Test) entityService.create(test);
		assert test != null;
		assert test.getId() != null;
		assert entityService.count(org.ihtsdo.rvf.entity.Test.class) > 0;

		//create assertion test
		assertionTest = new AssertionTest();
		assertionTest.setAssertion(assertion);
		assertionTest.setTest(test);
		assertionTest = (AssertionTest) entityService.create(assertionTest);
		assert assertionTest != null;
		assert assertionTest.getId() != null;
		assert entityService.count(AssertionTest.class) > 0;
	}

	@Test
	public void testFindEntitiesById() throws Exception {
		assertNotNull("Test object must not be null", entityService.find(test.getClass(), test.getId()));
		assertNotNull("AssertionTest object must not be null", entityService.find(assertionTest.getClass(), assertionTest.getId()));
		assertNotNull("Assertion object must not be null", assertionService.find(assertion.getAssertionId()));
		assertNotNull("Assertion object must not be null", assertionService.find(assertion.getUuid()));
	}

	@Test
	@Rollback
	public void testFindAssertionTests() throws Exception {

		// use service to find assertion tests associated with assertion
		final List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion);
		assert  assertionTests != null;
		assert assertionTests.size() > 0;
		assert assertionTests.contains(assertionTest);
	}

	@Test
	@Rollback
	public void testFindTests() throws Exception {

		// use service to find tests associated with assertion
		final List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion);
		assert  tests != null;
		assert tests.size() > 0;
		assert tests.contains(test);
	}

	@Test
	@Rollback
	public void testFindAssertionTestsByIds() throws Exception {

		// use service to find assertion tests associated with assertion
		final List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion.getAssertionId());
		assert  assertionTests != null;
		assert assertionTests.size() > 0;
		assert assertionTests.contains(assertionTest);
	}

	@Test
	@Rollback
	public void testFindTestsByIds() throws Exception {

		// use service to find tests associated with assertion
		final List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion.getAssertionId());
		assert  tests != null;
		assert tests.size() > 0;
		assert tests.contains(test);
	}

	@Test
	@Rollback
	public void testFindAssertionTestsUsingUuids() throws Exception {

		// use service to find assertion tests associated with assertion
		final List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion.getUuid());
		assert  assertionTests != null;
		assert assertionTests.size() > 0;
		assert assertionTests.contains(assertionTest);
	}

	@Test
	@Rollback
	public void testFindTestsByAssertion() throws Exception {

		// use service to find tests associated with assertion
		final List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion);
		assert  tests != null;
		assert tests.size() > 0;
		assert tests.contains(test);
	}

	@Test
	@Rollback
	public void testFindAssertionTestsByUuids() throws Exception {

		// use service to find assertion tests associated with assertion
		final List<AssertionTest> assertionTests = assertionService.getAssertionTests(assertion.getUuid());
		assert  assertionTests != null;
		assert assertionTests.size() > 0;
		assert assertionTests.contains(assertionTest);
	}

	@Test
	@Rollback
	public void testFindTestsByUuids() throws Exception {

		// use service to find tests associated with assertion
		final List<org.ihtsdo.rvf.entity.Test> tests = assertionService.getTests(assertion.getUuid());
		assert  tests != null;
		assert tests.size() > 0;
		assert tests.contains(test);
	}

	@Test
	@Rollback
	public void testAddTestToAssertion() throws Exception {
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		final Assertion returnedAssertion = assertionService.addTest(assertion, getRandomTest());
		assert returnedAssertion != null;
		assert assertionService.getAssertionTests(assertion).size() > originalCount;
	}

	@Test
	@Rollback
	public void testAddTestToAssertionWithReleaseCentre() throws Exception {
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		final Assertion returnedAssertion = assertionService.addTest(assertion, getRandomTest());
		assert returnedAssertion != null;
		assert assertionService.getAssertionTests(assertion).size() > originalCount;
	}

	@Test
	@Rollback
	public void testAddTestCollectionToAssertion() throws Exception {
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
		tests.add(getRandomTest());
		tests.add(getRandomTest());
		final Assertion returnedAssertion = assertionService.addTests(assertion, tests);
		assert returnedAssertion != null;
		assert assertionService.getAssertionTests(assertion).size() > originalCount;
	}

	@Test
	@Rollback
	public void testAddTestCollectionToAssertionWithReleaseCentre() throws Exception {
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
		tests.add(getRandomTest());
		tests.add(getRandomTest());
		final Assertion returnedAssertion = assertionService.addTests(assertion, tests);
		assert returnedAssertion != null;
		assert assertionService.getAssertionTests(assertion).size() > originalCount;
	}

	private org.ihtsdo.rvf.entity.Test getRandomTest(){
		final org.ihtsdo.rvf.entity.Test test = new org.ihtsdo.rvf.entity.Test();
		test.setName("Random Test " + UUID.randomUUID());
		return test;
	}

	@Test
	@Rollback
	public void testDeleteTestToAssertion() throws Exception {
		final org.ihtsdo.rvf.entity.Test test = getRandomTest();
		final Assertion returnedAssertion = assertionService.addTest(assertion, test);
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		assert returnedAssertion != null;

		assertionService.deleteTest(assertion, test);
		assert assertionService.getAssertionTests(assertion).size() < originalCount;
	}

	@Test
	@Rollback
	public void testDeleteTestToAssertionWithReleaseCentre() throws Exception {
		final org.ihtsdo.rvf.entity.Test test = getRandomTest();
		final Assertion returnedAssertion = assertionService.addTest(assertion, test);
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		assert returnedAssertion != null;

		assertionService.deleteTest(assertion, test);
		assert assertionService.getAssertionTests(assertion).size() < originalCount;
	}

	@Test
	@Rollback
	public void testDeleteTestCollectionToAssertion() throws Exception {
		final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
		tests.add(getRandomTest());
		tests.add(getRandomTest());
		final Assertion returnedAssertion = assertionService.addTests(assertion, tests);
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		assert returnedAssertion != null;

		assertionService.deleteTests(assertion, tests);
		assert assertionService.getAssertionTests(assertion).size() < originalCount;
	}

	@Test
	@Rollback
	public void testDeleteTestCollectionToAssertionWithReleaseCentre() throws Exception {
		final List<org.ihtsdo.rvf.entity.Test> tests = new ArrayList<>();
		tests.add(getRandomTest());
		tests.add(getRandomTest());
		final Assertion returnedAssertion = assertionService.addTests(assertion, tests);
		final int originalCount = assertionService.getAssertionTests(assertion).size();
		assert returnedAssertion != null;

		assertionService.deleteTests(assertion, tests);
		assert assertionService.getAssertionTests(assertion).size() < originalCount;
	}

	@Test
	@Rollback
	public void testSaveForAssertionGroup() throws Exception {

		Assertion assertion2 = new Assertion();
		assertion2.setAssertionText("Second assertion in group");
		// save assertion2
		assertion2 = assertionService.create(assertion2);
		assertNotNull(assertion2.getAssertionId());


		Assertion assertion3 = new Assertion();
		assertion3.setAssertionText("Third assertion in group");
		// save assertion3
		assertion3 = assertionService.create(assertion3);
		assertNotNull(assertion3.getAssertionId());

		final Set<Assertion> assertions = new HashSet<>();
		assertions.add(assertion);
		assertions.add(assertion2);
		AssertionGroup group = new AssertionGroup();
		group.setName("Test assertion group");
		group.setAssertions(assertions);


		group = (AssertionGroup) entityService.create(group);
		assertNotNull(group.getId());

		final Assertion retrievedAssertion = assertionService.find(assertion2.getAssertionId());
		System.out.println("retrievedAssertion = " + retrievedAssertion);
		List<Assertion> retrievedAssertions = assertionService.getAssertionsForGroup(group);
		System.out.println("retrievedAssertions.size() = " + retrievedAssertions.size());
		assertTrue("Group must contain assertion", retrievedAssertions.contains(assertion));
		assertTrue("Group must contain assertion2", retrievedAssertions.contains(assertion2));
		assertTrue("Group must contain 2 assertions", 2 == retrievedAssertions.size());

		final List<AssertionGroup> groups = assertionService.getGroupsForAssertion(assertion2);
		assertNotNull("Groups must have been retrieved", groups);
		System.out.println("groups.size() = " + groups.size());
		assertTrue("Groups must contain current group", groups.contains(group));

		// now assertion3 using add method
		AssertionGroup updatedGroup = assertionService.addAssertionToGroup(assertion3, group);
		retrievedAssertions = assertionService.getAssertionsForGroup(updatedGroup);
		System.out.println("retrievedAssertions.size() = " + retrievedAssertions.size());
		assertTrue("updatedGroup must contain 3 assertions", 3 == retrievedAssertions.size());
		for(final Assertion a : retrievedAssertions){
			System.out.println("a.getId() = " + a.getAssertionId());
			System.out.println("a.getAssertionText() = " + a.getAssertionText());
		}
		assertTrue("updatedGroup must contain assertion", retrievedAssertions.contains(assertion));
		assertTrue("updatedGroup must contain assertion2", retrievedAssertions.contains(assertion2));
		assertTrue("updatedGroup must contain assertion3", retrievedAssertions.contains(assertion3));

		// now remove assertion 3 using remove method
		updatedGroup = assertionService.removeAssertionFromGroup(assertion3, updatedGroup);
		retrievedAssertions = assertionService.getAssertionsForGroup(updatedGroup);
		System.out.println("retrievedAssertions.size() = " + retrievedAssertions.size());
		for(final Assertion a : retrievedAssertions){
			System.out.println("a.getId() = " + a.getAssertionId());
			System.out.println("a.getAssertionText() = " + a.getAssertionText());
		}
		assertTrue("updatedGroup must contain 2 assertions", 2 == retrievedAssertions.size());
		assertTrue("updatedGroup must contain assertion", retrievedAssertions.contains(assertion));
		assertTrue("updatedGroup must contain assertion2", retrievedAssertions.contains(assertion2));
		assertTrue("updatedGroup must contain assertion3", ! retrievedAssertions.contains(assertion3));

		//test deleting
		AssertionGroup toDelete = new AssertionGroup();
		toDelete.setName("To be deleted");
		toDelete.setAssertions(assertions);
		entityService.create(toDelete);
		toDelete.removeAllAssertionsFromGroup();
		entityService.delete(toDelete);
	}

	@After
	public void tearDown() throws Exception {
		assert assertionService != null;
		assertionService.delete(assertion);
		assert entityService != null;
		entityService.delete(test);
		entityService.delete(assertionTest);
	}
}
