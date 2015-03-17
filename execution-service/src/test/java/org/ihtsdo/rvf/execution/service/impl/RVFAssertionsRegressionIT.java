package org.ihtsdo.rvf.execution.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.service.AssertionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
@Transactional
public class RVFAssertionsRegressionIT {

	private static final String FILE_CENTRIC_VALIDATION = "file-centric-validation";
	private static final String COMPONENT_CENTRIC_VALIDATION = "component-centric-validation";
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
	private static final String PROSPECTIVE_RELEASE = "regression_test_prospective";
	private static final String PREVIOUS_RELEASE = "regression_test_previous";
	@Autowired
    private AssertionExecutionService assertionExecutionService;
    @Resource(name = "dataSource")
    private DataSource dataSource;
    @Resource(name = "snomedDataSource")
    private DataSource snomedDataSource;
    @Autowired
    private AssertionService assertionService;
    @Autowired
    private ReleaseDataManager releaseDataManager;
    @Autowired
    private AssertionDao assertionDao;
    
    private  URL releaseTypeExpectedResults;
	private URL componentCentrilExpected;
	private URL fileCentricExpected;
	private Long runId;
	@Before
	public void setUp() {
		//load previous and prospective versions if not loaded already
        assertNotNull(releaseDataManager);
        if (!releaseDataManager.isKnownRelease(PREVIOUS_RELEASE)) {
        	final URL previousReleaseUrl = RVFAssertionsRegressionIT.class.getResource("/SnomedCT_RegressionTest_20130131.zip");
            assertNotNull("Must not be null", previousReleaseUrl);
			final File previousFIle = new File(previousReleaseUrl.getFile());
			releaseDataManager.loadSnomedData(PREVIOUS_RELEASE, previousFIle);
        }
        if(!releaseDataManager.isKnownRelease(PROSPECTIVE_RELEASE)) {
        	final URL prspectiveReleaseUrl = RVFAssertionsRegressionIT.class.getResource("/SnomedCT_RegressionTest_20130731.zip");
            assertNotNull("Must not be null", prspectiveReleaseUrl);
        	releaseDataManager.loadSnomedData(PROSPECTIVE_RELEASE, new File(prspectiveReleaseUrl.getFile()));
        }
        
        releaseTypeExpectedResults = RVFAssertionsRegressionIT.class.getResource("/regressionTestResults/releaseTypeRegressionExpected.json");
        assertNotNull("Must not be null", releaseTypeExpectedResults);
        componentCentrilExpected = RVFAssertionsRegressionIT.class.getResource("/regressionTestResults/componentCentricRegressionExpected.json");
        assertNotNull("Must not be null", componentCentrilExpected);
        fileCentricExpected = RVFAssertionsRegressionIT.class.getResource("/regressionTestResults/fileCentricRegressionExpected.json");
        assertNotNull("Must not be null", fileCentricExpected);
        runId = System.currentTimeMillis();
        
	}
	
	@Test
	public void testReleaseTypeAssertions() throws Exception {
		final List<AssertionTest> tests = getAssertionTestsByKeyword(RELEASE_TYPE_VALIDATION);
		final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, PROSPECTIVE_RELEASE, PREVIOUS_RELEASE);
		assertTestResult(RELEASE_TYPE_VALIDATION, releaseTypeExpectedResults.getFile(), runItems);
		
	}

	private List<AssertionTest> getAssertionTestsByKeyword(final String keyword) {
		final List<AssertionTest> tests = new ArrayList<>();
    	final List<Assertion> assertions = assertionDao.getAssertionsByKeywords(keyword);
		for(final Assertion assertion : assertions ) {
    		tests.addAll(assertionDao.getAssertionTests(assertion));
    	}
		return tests;
	}
	private void assertTestResult(final String type, final String expectedJsonFileName,final Collection<TestRunItem> runItems) throws Exception {
		final List<RVFTestResult> results = new ArrayList<>();
		int failureCounter = 0;
		for(final TestRunItem item : runItems) {
			final RVFTestResult result = new RVFTestResult();
			result.setAssertonName(item.getAssertionText());
			result.setFirstNInstances(item.getFirstNInstances());
			result.setTotalFailed(item.getFailureCount() != null ? item.getFailureCount() : -1L);
			results.add(result);
			if(result.getTotalFailed() > 0) {
				failureCounter ++;
			}
		}
		final TestReport actualReport = new TestReport();
		actualReport.setAssertionType(type);;
		actualReport.setTotalAssertionsRun(runItems.size());
		actualReport.setTotalFailures(failureCounter);
		actualReport.setResults(results);
		final ObjectMapper mapper = new ObjectMapper();
		System.out.println("Test result");
		System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualReport));
		final Gson gson = new Gson();
		final BufferedReader br = new BufferedReader(new FileReader(expectedJsonFileName));
		final TestReport expectedReport = gson.fromJson(br, TestReport.class);
		assertEquals(expectedReport.getAssertionType(), actualReport.getAssertionType());
		assertEquals(expectedReport.getTotalAssertionsRun(), actualReport.getTotalAssertionsRun());
		assertEquals(expectedReport.getTotalAssertionsFailed(),actualReport.getTotalAssertionsFailed());
		assertEquals(expectedReport.getResults().size(), actualReport.getResults().size());
		final List<RVFTestResult> expected = expectedReport.getResults();
		final List<RVFTestResult> actual = actualReport.getResults();
		final Map<String,RVFTestResult> expectedResultByNameMap = new HashMap<>();
		for (final RVFTestResult result : expectedReport.getResults()) {
			expectedResultByNameMap.put(result.getAssertionName(), result);
		}
		final Map<String,RVFTestResult> actualResultByNameMap = new HashMap<>();
		for (final RVFTestResult result : actualReport.getResults()) {
			actualResultByNameMap.put(result.getAssertionName(), result);
		}
		for (final String name : expectedResultByNameMap.keySet()) {
			assertTrue("Acutal test result should have assertion name but doesn't.", actualResultByNameMap.containsKey(name));
			assertEquals("Acutal result is different from the expected for assertion name:" + name, expectedResultByNameMap.get(name),actualResultByNameMap.get(name));
		}
		
		Collections.sort(expected);
		Collections.sort(actual);
		for(int i=0;i < expected.size();i++) {
			assertEquals(expected.get(i), actual.get(i));
		}
	}

	@Test
	public void testComponentCentricAssertions() throws Exception {
		final List<AssertionTest> tests = getAssertionTestsByKeyword(COMPONENT_CENTRIC_VALIDATION);
		final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, PROSPECTIVE_RELEASE, PREVIOUS_RELEASE);
		assertTestResult(COMPONENT_CENTRIC_VALIDATION, componentCentrilExpected.getFile(), runItems);
	}
	@Test
	public void testFileCentricAssertions() throws Exception {
		final List<AssertionTest> tests = getAssertionTestsByKeyword(FILE_CENTRIC_VALIDATION);
		final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionTests(tests, runId, PROSPECTIVE_RELEASE, PREVIOUS_RELEASE);
		assertTestResult(FILE_CENTRIC_VALIDATION, fileCentricExpected.getFile(), runItems);
	}
	
	private static class TestReport {

		private String assertionType;
		private int totalAssertionsRun;
		private int totalAssertionsFailed;
		private List<RVFTestResult> results;

		public void setAssertionType(final String keyword) {
			assertionType = keyword;
		}

		public void setTotalAssertionsRun(final int runTotal) {
			totalAssertionsRun = runTotal;
		}

		public void setTotalFailures(final int failureCounter) {
			totalAssertionsFailed = failureCounter;
		}

		public void setResults(final List<RVFTestResult> testResults) {
			results = testResults;
		}

		public int getTotalAssertionsRun() {
			return totalAssertionsRun;
		}

		public int getTotalAssertionsFailed() {
			return totalAssertionsFailed;
		}

		public void setTotalAssertionsFailed(final int totalFailed) {
			totalAssertionsFailed = totalFailed;
		}

		public String getAssertionType() {
			return assertionType;
		}

		public List<RVFTestResult> getResults() {
			return results;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((assertionType == null) ? 0 : assertionType.hashCode());
			result = prime * result
					+ ((results == null) ? 0 : results.hashCode());
			result = prime * result + totalAssertionsFailed;
			result = prime * result + totalAssertionsRun;
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final TestReport other = (TestReport) obj;
			if (assertionType == null) {
				if (other.assertionType != null)
					return false;
			} else if (!assertionType.equals(other.assertionType))
				return false;
			if (results == null) {
				if (other.results != null)
					return false;
			} else if (!results.equals(other.results))
				return false;
			if (totalAssertionsFailed != other.totalAssertionsFailed)
				return false;
			if (totalAssertionsRun != other.totalAssertionsRun)
				return false;
			return true;
		}
		
		
		
	}
	private static class RVFTestResult implements Comparable<RVFTestResult> {
		private String assertionName;
		private List<String> firstNInstances;
		private long totalFailed;
		public void setAssertonName(final String assertionText) {
			assertionName = assertionText;
		}

		public void setFirstNInstances(final List<String> firstNInstances) {
			this.firstNInstances = firstNInstances;
		}

		public void setTotalFailed(final long failureCount) {
			totalFailed = failureCount;
		}

		public String getAssertionName() {
			return assertionName;
		}
		public List<String> getFirstNInstances() {
			return firstNInstances;
		}
		public long getTotalFailed() {
			return totalFailed;
		}
		
		
		@Override
		public String toString() {
			return "RVFTestResult [assertionName=" + assertionName
					+ ", firstNInstances=" + firstNInstances + ", totalFailed="
					+ totalFailed + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((assertionName == null) ? 0 : assertionName.hashCode());
			result = prime
					* result
					+ ((firstNInstances == null) ? 0 : firstNInstances
							.hashCode());
			result = prime * result + (int) (totalFailed ^ (totalFailed >>> 32));
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final RVFTestResult other = (RVFTestResult) obj;
			if (assertionName == null) {
				if (other.assertionName != null)
					return false;
			} else if (!assertionName.equals(other.assertionName))
				return false;
			if (firstNInstances == null) {
				if (other.firstNInstances != null)
					return false;
			} else if (!firstNInstances.containsAll(other.firstNInstances))
				return false;
			if (totalFailed != other.totalFailed)
				return false;
			return true;
		}

		@Override
		public int compareTo(final RVFTestResult o) {
			return this.assertionName.compareTo(o.getAssertionName());
		}
	}
}
