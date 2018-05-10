package org.ihtsdo.rvf.execution.service.test.harness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.impl.ExecutionConfig;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

/**
 * To run this test harness successfully with clean data you need to first 
 * 1. drop the rvf_master database and create rvf_master database
 * 2. run the rvf application to load existing assertions and assertion group
 * 3. run test harness
 * Note: if you have changed the regression test data you need drop the corresponding schema as well.
 * SnomedCT_RegressionTest_20130131 and SnomedCT_RegressionTest_20130731 are made up data for testing purpose.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
@Transactional
public class RVFAssertionsRegressionTestHarnesss {

	private static final String FILE_CENTRIC_VALIDATION = "file-centric-validation";
	private static final String COMPONENT_CENTRIC_VALIDATION = "component-centric-validation";
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
	private static final String PROSPECTIVE_RELEASE = "regression_test_prospective";
	
	private static final String PREVIOUS_RELEASE = "regression_test_previous";
	@Autowired
    private AssertionExecutionService assertionExecutionService;
    @Resource(name = "dataSource")
    private DataSource dataSource;
    @Autowired
    private AssertionService assertionService;
    @Autowired
    private ReleaseDataManager releaseDataManager;
    @Autowired
    private ResourceDataLoader resourceDataLoader;
    @Autowired
    private  AssertionDao assertionDao;
    private  URL releaseTypeExpectedResults;
	private URL componentCentrilExpected;
	private URL fileCentricExpected;
	private ExecutionConfig config;
	private final ObjectMapper mapper = new ObjectMapper();
	private  List<String> rf2FilesLoaded = new ArrayList<>();
	
	@Before
	public void setUp() throws IOException, SQLException, BusinessServiceException {
		//load previous and prospective versions if not loaded already
        assertNotNull(releaseDataManager);
        if (!releaseDataManager.isKnownRelease(PREVIOUS_RELEASE)) {
        	final URL previousReleaseUrl = RVFAssertionsRegressionTestHarnesss.class.getResource("/SnomedCT_RegressionTest_20130131");
            assertNotNull("Must not be null", previousReleaseUrl);
			final File previousFile = new File(previousReleaseUrl.getFile() + "_test.zip");
			ZipFileUtils.zip(previousReleaseUrl.getFile(), previousFile.getAbsolutePath());
			releaseDataManager.uploadPublishedReleaseData(previousFile, "regression_test", "previous");
        }
        if(!releaseDataManager.isKnownRelease(PROSPECTIVE_RELEASE)) {
        	final URL prospectiveReleaseUrl = RVFAssertionsRegressionTestHarnesss.class.getResource("/SnomedCT_RegressionTest_20130731");
            assertNotNull("Must not be null", prospectiveReleaseUrl);
            final File prospectiveFile = new File(prospectiveReleaseUrl.getFile() + "_test.zip");
			ZipFileUtils.zip(prospectiveReleaseUrl.getFile(), prospectiveFile.getAbsolutePath());
        	releaseDataManager.loadSnomedData(PROSPECTIVE_RELEASE,rf2FilesLoaded, prospectiveFile);
        }
        
        releaseTypeExpectedResults = RVFAssertionsRegressionTestHarnesss.class.getResource("/regressionTestResults/releaseTypeRegressionExpected.json");
        assertNotNull("Must not be null", releaseTypeExpectedResults);
        componentCentrilExpected = RVFAssertionsRegressionTestHarnesss.class.getResource("/regressionTestResults/componentCentricRegressionExpected.json");
        assertNotNull("Must not be null", componentCentrilExpected);
        fileCentricExpected = RVFAssertionsRegressionTestHarnesss.class.getResource("/regressionTestResults/fileCentricRegressionExpected.json");
        assertNotNull("Must not be null", fileCentricExpected);
        releaseDataManager.setSchemaForRelease(PREVIOUS_RELEASE, "rvf_" + PREVIOUS_RELEASE);
        releaseDataManager.setSchemaForRelease(PROSPECTIVE_RELEASE, "rvf_"+ PROSPECTIVE_RELEASE);
        resourceDataLoader.loadResourceData(releaseDataManager.getSchemaForRelease(PROSPECTIVE_RELEASE));
        final List<Assertion> assertions = assertionDao.getAssertionsByKeyWord("resource",true);
		config = new ExecutionConfig(System.currentTimeMillis());
		config.setPreviousVersion(PREVIOUS_RELEASE);
		config.setProspectiveVersion(PROSPECTIVE_RELEASE);
		config.setFailureExportMax(10);
		assertionExecutionService.executeAssertions(assertions, config);
	}
	
	@Test
	public void testReleaseTypeAssertions() throws Exception {
		runAssertionsTest(RELEASE_TYPE_VALIDATION, releaseTypeExpectedResults.getFile());
	}
	
	@Test
	public void testComponentCentricAssertions() throws Exception {
		runAssertionsTest(COMPONENT_CENTRIC_VALIDATION, componentCentrilExpected.getFile());
	}
	@Test
	public void testFileCentricAssertions() throws Exception {
		runAssertionsTest(FILE_CENTRIC_VALIDATION, fileCentricExpected.getFile());
	}
	 
	private void runAssertionsTest(final String groupName, final String expectedJsonFile) throws Exception {
		 final List<Assertion> assertions= assertionDao.getAssertionsByKeyWord(groupName, false);
		 System.out.println("found total assertions:" + assertions.size());
		 long timeStart = System.currentTimeMillis();
			final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionsConcurrently(assertions, config);
//		 final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertions(assertions, config);
			
			long timeEnd = System.currentTimeMillis();
			System.out.println("Time taken:" +(timeEnd-timeStart));
			releaseDataManager.clearQAResult(config.getExecutionId());
			assertTestResult(groupName, expectedJsonFile, runItems);
	 }
	
	
	@Test
	public void testGetAssertionsByGroup() {
		AssertionGroup group = assertionService.getAssertionGroupByName("InternationalEdition");
		
		List<Assertion> assertions = assertionService.getAssertionsForGroup(group);
		List<Assertion> releaseTypeAssertions = new ArrayList<>();
		for (Assertion assertion : assertions) {
			if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
				releaseTypeAssertions.add(assertion);
			}
		}
		assertEquals(259, assertions.size());
		assertEquals(103, releaseTypeAssertions.size());
	}
	
	@Test
	public void testGetAssertionsForIntAuthoring() {
		AssertionGroup group = assertionService.getAssertionGroupByName("int-authoring");
		
		List<Assertion> assertions = assertionService.getAssertionsForGroup(group);
		assertEquals(24, assertions.size());
	}
	
	
	@Test
	public void testGetAssertionsForCommonAuthoring() {
		AssertionGroup group = assertionService.getAssertionGroupByName("common-authoring");
		
		List<Assertion> assertions = assertionService.getAssertionsForGroup(group);
		assertEquals(120, assertions.size());
	}
	
	@Test
	public void testSpecificAssertion() throws Exception {
		runAssertionsTest("30947783-78F5-4FFC-A22B-B03D83C5909D");
	}
	
	private void runAssertionsTest(String assertionUUID) throws Exception {
		final List<Assertion> assertions= new ArrayList<>();
		assertions.add(assertionDao.getAssertionByUUID(assertionUUID));
		System.out.println("found total assertions:" + assertions.size());
		long timeStart = System.currentTimeMillis();
		final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionsConcurrently(assertions, config);
//		final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertions(assertions, config);
		for (TestRunItem item : runItems) {
			System.out.println (item.getAssertionUuid() + " failures: " + item.getFailureCount());
		}
		long timeEnd = System.currentTimeMillis();
		releaseDataManager.clearQAResult(config.getExecutionId());
		System.out.println("Time taken:" +(timeEnd-timeStart));
	 }
	
	private void assertTestResult(final String type, final String expectedJsonFileName,final Collection<TestRunItem> runItems) throws Exception {
		final List<RVFTestResult> results = new ArrayList<>();
		int failureCounter = 0;
		for(final TestRunItem item : runItems) {
			final RVFTestResult result = new RVFTestResult();
			result.setAssertonName(item.getAssertionText());
			result.setFirstNInstances(item.getFirstNInstances());
			result.setAssertionUuid(item.getAssertionUuid());
//			assertNull("No failure should have occured for assertion uuid." + item.getAssertionUuid(), item.getFailureMessage());
			result.setTotalFailed(item.getFailureCount() != null ? item.getFailureCount() : -1L);
			results.add(result);
		
			if (result.getTotalFailed() < 0) {
				throw new RuntimeException("Assetion didn't complete sucessfully!" + item.toString());
			}
			
			if (result.getTotalFailed() > 0) {
				failureCounter ++;
			}
		}
		Collections.sort(results);
		final TestReport actualReport = new TestReport();
		actualReport.setAssertionType(type);
		actualReport.setTotalAssertionsRun(runItems.size());
		actualReport.setTotalFailures(failureCounter);
		actualReport.setResults(results);
//		File tempResult = File.createTempFile("tempResult_"+ type, ".txt");
//		FileWriter writer = new FileWriter(tempResult);
//		mapper.writeValue(writer,actualReport);
//		System.out.println("Please see result in file:" + tempResult.getAbsolutePath());

		System.out.println("Test result");
		String actualReportStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualReport);
		System.out.println(actualReportStr);
		final Gson gson = new Gson();
		final BufferedReader br = new BufferedReader(new FileReader(expectedJsonFileName));
		final TestReport expectedReport = gson.fromJson(br, TestReport.class);
		
		/*InputStream is = new FileInputStream(expectedJsonFileName);
		String expectedReportStr = IOUtils.toString(is);
		JSONObject expectedReportObj = new JSONObject(expectedReportStr);
		JSONObject actualReportObj = new JSONObject(actualReportStr);
		JSONAssert.assertEquals(expectedReportObj, actualReportObj, false);*/
		
		final List<RVFTestResult> expected = expectedReport.getResults();
		final List<RVFTestResult> actual = actualReport.getResults();
		final Map<UUID,RVFTestResult> expectedResultByNameMap = new HashMap<>();
		for (final RVFTestResult result : expectedReport.getResults()) {
			expectedResultByNameMap.put(result.getAssertionUuid(), result);
		}
		final Map<UUID,RVFTestResult> actualResultByNameMap = new HashMap<>();
		for (final RVFTestResult result : actualReport.getResults()) {
			actualResultByNameMap.put(result.getAssertionUuid(), result);
		}
		for (final UUID uuid : expectedResultByNameMap.keySet()) {
			assertTrue("Actual test result should have assertion uuid but doesn't: " + uuid, actualResultByNameMap.containsKey(uuid));
			final RVFTestResult expectedResult = expectedResultByNameMap.get(uuid);
			final RVFTestResult actualResult = actualResultByNameMap.get(uuid);
			assertEquals("Assertion name is not the same" + " for assertion uuid:" + uuid, expectedResult.getAssertionName(),actualResult.getAssertionName());
			assertEquals("Total failures count doesn't match"  + " for assertion uuid:" + uuid, expectedResult.getTotalFailed(), actualResult.getTotalFailed());
			if (expectedResult.getTotalFailed() > 0 ) {
				explainDifference(uuid, expectedResult, actualResult);
				assertTrue("First N instances not matching" + " for assertion uuid:" + uuid, expectedResult.getFirstNInstances().containsAll(actualResult.getFirstNInstances()));
			}
		}
		Collections.sort(expected);
		Collections.sort(actual);
		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size();i++) {
			if (!expected.get(i).equals(actual.get(i))) {
				explainDifference(actual.get(i).getAssertionUuid(), expected.get(i),actual.get(i));
			}
			assertEquals(expected.get(i), actual.get(i));
			if (expected.get(i).getTotalFailed() > 0) {
				for (FailureDetail detail : actual.get(i).getFirstNInstances()) {
					assertTrue("ConceptId should not be null", detail.getConceptId() != null); 
				}
			}
		}
		assertEquals(expectedReport.getAssertionType(), actualReport.getAssertionType());
		assertEquals(expectedReport.getTotalAssertionsRun(), actualReport.getTotalAssertionsRun());
		assertEquals(expectedReport.getTotalAssertionsFailed(),actualReport.getTotalAssertionsFailed());
		assertEquals(expectedReport.getResults().size(), actualReport.getResults().size());
	}
	
	private void explainDifference(UUID uuid, RVFTestResult left,
			RVFTestResult right) {
		if (!left.getAssertionUuid().equals(right.getAssertionUuid())) {
			System.out.println ("Difference explained: Result UUID " + left.getAssertionUuid() + " does not match actual compared value " + right.getAssertionUuid());
		} else if (!left.getAssertionName().equals(right.getAssertionName())) {
			System.out.println ("Difference explained: Result assertion name  " + left.getAssertionName() + " does not match actual compared value " + right.getAssertionName());
		} else if (left.getTotalFailed() != right.getTotalFailed()) {
			System.out.println ("Difference explained: Result total failed  " + left.getTotalFailed() + " does not match actual compared value" + right.getTotalFailed());
		} else if (left.getFirstNInstances().size() != right.getFirstNInstances().size()) {
			System.out.println ("Difference explained: " + uuid + " Result firstN size  " + left.getFirstNInstances().size() + " does not match actual compared value " + right.getFirstNInstances().size());
		}
		
		for (int i=0; i<left.getFirstNInstances().size(); i++) {
			FailureDetail leftFd = left.getFirstNInstances().get(i);
			FailureDetail rightFd = right.getFirstNInstances().get(i); 
			if (!leftFd.equals(rightFd)) {
				System.out.println ("Difference explained: " + uuid + " expected Failure Detail " + i + ": " + leftFd + " does not match actual compared value " + rightFd);
				return;
			}
		}
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
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestReport other = (TestReport) obj;
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
			return totalAssertionsRun == other.totalAssertionsRun;
		}
		
	}
	
	@After
	public void tearDown() {
		rf2FilesLoaded.clear();
	}
	private static class RVFTestResult implements Comparable<RVFTestResult> {
		private String assertionName;
		private UUID assertionUuid;
		private List<FailureDetail> firstNInstances;
		private long totalFailed;
		public void setAssertonName(final String assertionText) {
			assertionName = assertionText;
		}

		public void setFirstNInstances(final List<FailureDetail> list) {
			this.firstNInstances = list;
		}

		public void setTotalFailed(final long failureCount) {
			totalFailed = failureCount;
		}

		public String getAssertionName() {
			return assertionName;
		}
		public List<FailureDetail> getFirstNInstances() {
			return firstNInstances;
		}
		public long getTotalFailed() {
			return totalFailed;
		}
		
		public UUID getAssertionUuid() {
			return assertionUuid;
		}

		public void setAssertionUuid(final UUID assertionUuid) {
			this.assertionUuid = assertionUuid;
		}

		public void setAssertionName(final String assertionName) {
			this.assertionName = assertionName;
		}
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((assertionName == null) ? 0 : assertionName.hashCode());
			result = prime * result
					+ ((assertionUuid == null) ? 0 : assertionUuid.hashCode());
			result = prime
					* result
					+ ((firstNInstances == null) ? 0 : firstNInstances
							.hashCode());
			result = prime * result
					+ (int) (totalFailed ^ (totalFailed >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RVFTestResult other = (RVFTestResult) obj;
			if (assertionName == null) {
				if (other.assertionName != null)
					return false;
			} else if (!assertionName.equals(other.assertionName))
				return false;
			if (assertionUuid == null) {
				if (other.assertionUuid != null)
					return false;
			} else if (!assertionUuid.equals(other.assertionUuid))
				return false;
			if (firstNInstances == null) {
				if (other.firstNInstances != null)
					return false;
			} else {
				Collections.sort(firstNInstances);
				Collections.sort(other.firstNInstances);
				if (!firstNInstances.equals(other.firstNInstances)) {
					return false;
				}
			}
				
			return totalFailed == other.totalFailed;
		}

		@Override
		public int compareTo(final RVFTestResult o) {
			return this.assertionUuid.compareTo(o.getAssertionUuid());
		}
		
		@Override
		public String toString() {
			return assertionUuid.toString() + ":" + assertionName + " [" + totalFailed +" failures, " + firstNInstances.size() + " examples]";
		}
	}
}
