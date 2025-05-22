package org.ihtsdo.rvf.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.*;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * To run this test harness successfully with clean data you need to first
 * 1. drop the rvf_master database and create rvf_master database
 * 2. run the rvf application to load existing assertions and assertion group
 * 3. run test harness
 * Note: if you have changed the regression test data you need drop the corresponding schema as well.
 * SnomedCT_RegressionTest_20130131 and SnomedCT_RegressionTest_20130731 are made up data for testing purpose.
 */

@ContextConfiguration(classes = TestConfig.class)
public class RVFAssertionsRegressIntegrationTest {

    public static final String DIFF = "*** Difference explained: ";

    private static final String FILE_CENTRIC_VALIDATION = "file-centric-validation";
    private static final String COMPONENT_CENTRIC_VALIDATION = "component-centric-validation";
    private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";
    private static final String PROSPECTIVE_RELEASE = "rvf_regression_test_prospective";

    private static final String PREVIOUS_RELEASE = "rvf_regression_test_previous";
    public static final String MDRS = "mdrs";
    @Autowired
    private AssertionExecutionService assertionExecutionService;
    @Autowired
    private AssertionService assertionService;
    @Autowired
    private ReleaseDataManager releaseDataManager;
    @Autowired
    private ResourceDataLoader resourceDataLoader;
    @Autowired
    private MysqlFailuresExtractor mysqlFailuresExtractor;

    private URL releaseTypeExpectedResults;
    private URL componentCentrilExpected;
    private URL fileCentricExpected;
    private MysqlExecutionConfig config;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> rf2FilesLoaded = new ArrayList<>();
    private boolean isRunFirstTime = true;

    @BeforeEach
    public void setUp() throws IOException, BusinessServiceException {
        if (!isRunFirstTime) {
            return;
        }
        //load previous and prospective versions if not loaded already
        assertNotNull(releaseDataManager);
        config = new MysqlExecutionConfig(System.currentTimeMillis());
        config.setPreviousVersion(PREVIOUS_RELEASE);
        config.setProspectiveVersion(PROSPECTIVE_RELEASE);
        config.setFailureExportMax(10);
        //Reload test data from zip files
        boolean reloadTestData = true;

        if (reloadTestData || !releaseDataManager.isKnownRelease(PREVIOUS_RELEASE)) {
            //set it to true for testing mysql binary archive
            boolean testMysqlBinaryArchive = false;

            if (!testMysqlBinaryArchive || !releaseDataManager.restoreReleaseFromBinaryArchive(PREVIOUS_RELEASE + ".zip")) {
                URL previousReleaseUrl = RVFAssertionsRegressIntegrationTest.class.getResource("/SnomedCT_RegressionTest_20130131");
                assertNotNull(previousReleaseUrl, "Must not be null");
                File previousFile = new File(previousReleaseUrl.getFile() + "_test.zip");
                ZipFileUtils.zip(previousReleaseUrl.getFile(), previousFile.getAbsolutePath());
                releaseDataManager.uploadPublishedReleaseData(previousFile, "regression_test", "previous", Collections.emptyList());
                if (testMysqlBinaryArchive) {
                    String archiveFileName = releaseDataManager.generateBinaryArchive("rvf_regression_test_previous");
                    System.out.println("Mysql binary file is archvied at " + archiveFileName);
                }
            }
        }
        if (reloadTestData || !releaseDataManager.isKnownRelease(PROSPECTIVE_RELEASE)) {
            final URL prospectiveReleaseUrl = RVFAssertionsRegressIntegrationTest.class.getResource("/SnomedCT_RegressionTest_20130731");
            assertNotNull(prospectiveReleaseUrl, "Must not be null");
            final File prospectiveFile = new File(prospectiveReleaseUrl.getFile() + "_test.zip");
            ZipFileUtils.zip(prospectiveReleaseUrl.getFile(), prospectiveFile.getAbsolutePath());
            releaseDataManager.loadSnomedData(PROSPECTIVE_RELEASE, rf2FilesLoaded, Collections.emptyList(), prospectiveFile);
            resourceDataLoader.loadResourceData(PROSPECTIVE_RELEASE);
            List<Assertion> assertions = assertionService.getAssertionsByKeyWords("resource", true);
            assertNotNull(assertions);
            assertFalse(assertions.isEmpty());
            assertionExecutionService.executeAssertions(assertions, config);
        }
        releaseTypeExpectedResults = RVFAssertionsRegressIntegrationTest.class.getResource("/regressionTestResults/releaseTypeRegressionExpected.json");
        assertNotNull(releaseTypeExpectedResults, "Must not be null");
        componentCentrilExpected = RVFAssertionsRegressIntegrationTest.class.getResource("/regressionTestResults/componentCentricRegressionExpected.json");
        assertNotNull(componentCentrilExpected, "Must not be null");
        fileCentricExpected = RVFAssertionsRegressIntegrationTest.class.getResource("/regressionTestResults/fileCentricRegressionExpected.json");
        assertNotNull(fileCentricExpected, "Must not be null");
        isRunFirstTime = false;
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
        final List<Assertion> assertions = assertionService.getAssertionsByKeyWords(groupName, false);
        System.out.println("found total assertions:" + assertions.size());
        long timeStart = System.currentTimeMillis();
        final Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionsConcurrently(assertions, config);
        List<TestRunItem> result = new ArrayList<>(runItems);
        mysqlFailuresExtractor.extractTestResults(result, config, assertions);
        long timeEnd = System.currentTimeMillis();
        System.out.println("Time taken:" + (timeEnd - timeStart));
        releaseDataManager.clearQAResult(config.getExecutionId());
        assertTestResult(groupName, expectedJsonFile, result);
    }


    @org.junit.jupiter.api.Test
    public void testGetAssertionsByGroup() {
        AssertionGroup group = assertionService.getAssertionGroupByName("InternationalEdition");
        Set<Assertion> assertions = group.getAssertions();
        List<Assertion> releaseTypeAssertions = new ArrayList<>();
        List<Assertion> mdrsAssertions = new ArrayList<>();
        for (Assertion assertion : group.getAssertions()) {
            if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION)) {
                releaseTypeAssertions.add(assertion);
            } else if (assertion.getKeywords().contains(MDRS)) {
                mdrsAssertions.add(assertion);
            }
        }
        assertEquals(30, mdrsAssertions.size());
        assertEquals(272, assertions.size());
        assertEquals(108, releaseTypeAssertions.size());
    }

    @org.junit.jupiter.api.Test
    public void testGetAssertionsForIntAuthoring() {
        AssertionGroup group = assertionService.getAssertionGroupByName("int-authoring");
        assertEquals(59, group.getAssertions().size());
    }


    @org.junit.jupiter.api.Test
    public void testTotalAssertions() {
        assertEquals(371, assertionService.count().longValue());
    }

    @Test
    public void testGetAssertionsForEEAuthoring() {
        AssertionGroup group = assertionService.getAssertionGroupByName("ee-authoring");
        assertEquals(4, group.getAssertions().size());
    }


    @org.junit.jupiter.api.Test
    public void testTotalGroups() {
        assertEquals(35, assertionService.getAllAssertionGroups().size());
    }

    @org.junit.jupiter.api.Test
    public void testGetAssertionsForCommonAuthoring() {
        AssertionGroup group = assertionService.getAssertionGroupByName("common-authoring");
        assertEquals(64, group.getAssertions().size());
    }

    @Test
    public void testSpecificAssertion() throws Exception {
        runAssertionsTest("c19b9008-bbc9-4209-a3d7-896d414df565");
    }

    private void runAssertionsTest(String assertionUUID) throws Exception {
        final List<Assertion> assertions = new ArrayList<>();
        assertions.add(assertionService.getAssertionByUuid(UUID.fromString(assertionUUID)));
        System.out.println("found total assertions:" + assertions.size());
        long timeStart = System.currentTimeMillis();
        Collection<TestRunItem> runItems = assertionExecutionService.executeAssertionsConcurrently(assertions, config);
        System.out.println("Total tests run:" + runItems.size());
        long timeEnd = System.currentTimeMillis();
        releaseDataManager.clearQAResult(config.getExecutionId());
        System.out.println("Time taken:" + (timeEnd - timeStart));
    }

    private void assertTestResult(final String type, final String expectedJsonFileName, final Collection<TestRunItem> runItems) throws Exception {
        final List<RVFTestResult> results = new ArrayList<>();
        int failureCounter = 0;
        for (final TestRunItem item : runItems) {
            final RVFTestResult result = new RVFTestResult();
            result.setAssertonName(item.getAssertionText());
            result.setFirstNInstances(item.getFirstNInstances());
            result.setAssertionUuid(item.getAssertionUuid());
            result.setTotalFailed(item.getFailureCount() != null ? item.getFailureCount() : -1L);
            results.add(result);

            if (result.getTotalFailed() < 0) {
                throw new RuntimeException("Assertion didn't complete sucessfully - " + item);
            }

            if (result.getTotalFailed() > 0) {
                failureCounter++;
            }
        }
        Collections.sort(results);
        final TestReport actualReport = new TestReport();
        actualReport.setAssertionType(type);
        actualReport.setTotalAssertionsRun(runItems.size());
        actualReport.setTotalFailures(failureCounter);
        actualReport.setResults(results);

        File tempResult = File.createTempFile("tempResult_" + type, ".txt");
        FileWriter writer = new FileWriter(tempResult);
        mapper.writerWithDefaultPrettyPrinter().writeValue(writer, actualReport);
        System.err.println("Please see " + type + " result in file:" + tempResult.getAbsolutePath());
        final Gson gson = new Gson();
        final BufferedReader br = new BufferedReader(new FileReader(expectedJsonFileName));
        final TestReport expectedReport = gson.fromJson(br, TestReport.class);

        final List<RVFTestResult> expected = expectedReport.getResults();
        final List<RVFTestResult> actual = actualReport.getResults();

        final Map<UUID, RVFTestResult> expectedResultByUuidMap = new HashMap<>();
        for (final RVFTestResult result : expectedReport.getResults()) {
            expectedResultByUuidMap.put(result.getAssertionUuid(), result);
        }

        final Map<UUID, RVFTestResult> actualResultByUuidMap = new HashMap<>();
        for (final RVFTestResult result : actualReport.getResults()) {
            actualResultByUuidMap.put(result.getAssertionUuid(), result);
        }

        for (final UUID uuid : expectedResultByUuidMap.keySet()) {
            assertTrue(actualResultByUuidMap.containsKey(uuid), type + " actual test result should have expected assertion but does not: " + expectedResultByUuidMap.get(uuid));
            final RVFTestResult expectedResult = expectedResultByUuidMap.get(uuid);
            final RVFTestResult actualResult = actualResultByUuidMap.get(uuid);
            assertEquals("Assertion name is not the same" + " for assertion: " + actualResult, expectedResult.getAssertionName(), actualResult.getAssertionName());
            if (expectedResult.getTotalFailed() > 0 || actualResult.getTotalFailed() > 0) {
                explainDifference(uuid, expectedResult, actualResult);
                if (expectedResult.getFirstNInstances() != null && actualResult.getFirstNInstances() != null) {
                    assertTrue(expectedResult.getFirstNInstances().containsAll(actualResult.getFirstNInstances()), "First N instances not matching" + " for assertion: " + actualResult);
                }
            }
            assertEquals(expectedResult.getTotalFailed(), actualResult.getTotalFailed(), type + " total failures count doesn't match" + " for assertion: " + actualResult);
        }

        //And also work through the actual results, in case we have additional items there.
        for (final UUID uuid : actualResultByUuidMap.keySet()) {
            assertTrue(expectedResultByUuidMap.containsKey(uuid), type + " unexpected test result in actual: " + actualResultByUuidMap.get(uuid));
            final RVFTestResult expectedResult = expectedResultByUuidMap.get(uuid);
            final RVFTestResult actualResult = actualResultByUuidMap.get(uuid);
            assertEquals("Assertion name is not the same" + " for assertion: " + actualResult, expectedResult.getAssertionName(), actualResult.getAssertionName());
            if (expectedResult.getTotalFailed() > 0) {
                explainDifference(uuid, expectedResult, actualResult);
                assertTrue(expectedResult.getFirstNInstances().containsAll(actualResult.getFirstNInstances()), "First N instances not matching" + " for assertion: " + actualResult);
            }
            assertEquals(expectedResult.getTotalFailed(), actualResult.getTotalFailed(), type + " total failures count doesn't match" + " for assertion: " + actualResult);
        }

        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                explainDifference(actual.get(i).getAssertionUuid(), expected.get(i), actual.get(i));
            }
            if (!expected.get(i).equals(actual.get(i))) {
                System.out.println("Debug Here!");
            }
            assertEquals(expected.get(i), actual.get(i));
            if (expected.get(i).getTotalFailed() > 0) {
                for (FailureDetail detail : actual.get(i).getFirstNInstances()) {
                    assertNotNull(detail.getConceptId(), "ConceptId should not be null");
                }
            }
        }
        assertEquals(expectedReport.getAssertionType(), actualReport.getAssertionType());
        assertEquals(expectedReport.getTotalAssertionsRun(), actualReport.getTotalAssertionsRun());
        assertEquals(expectedReport.getTotalAssertionsFailed(), actualReport.getTotalAssertionsFailed());
        assertEquals(expectedReport.getResults().size(), actualReport.getResults().size());
    }

    private void explainDifference(UUID uuid, RVFTestResult left,
                                   RVFTestResult right) {
        if (!left.getAssertionUuid().equals(right.getAssertionUuid())) {
            System.out.println(DIFF + left + "\n\t expected result UUID " + left.getAssertionUuid() + " does not match actual value " + right.getAssertionUuid());
        } else if (!left.getAssertionName().equals(right.getAssertionName())) {
            System.out.println(DIFF + left + "\n\t expected result assertion name  " + left.getAssertionName() + " does not match actual value " + right.getAssertionName());
        } else if (left.getFirstNInstances() == null && right.getFirstNInstances() != null) {
            System.out.println(DIFF + left + "\n\t expected result firstN is null, unlike actual compared value " + right.getFirstNInstances().size());
        } else if (left.getFirstNInstances() != null && right.getFirstNInstances() == null) {
            System.out.println(DIFF + left + "\n\t expected result firstN size " + left.getFirstNInstances().size() + " does not match actual compared value which is null");
        } else if (left.getTotalFailed() != right.getTotalFailed()) {
            System.out.println(DIFF + left + "\n\t expected result total failed " + left.getTotalFailed() + " does not match actual value " + right.getTotalFailed());
        } else if (left.getFirstNInstances() == null && right.getFirstNInstances() == null) {
            //Can't explain difference if there are no failure instances to examine
            return;
        } else if (left.getFirstNInstances().size() != right.getFirstNInstances().size()) {
            System.out.println(DIFF + left + "\n\t expected result firstN size " + left.getFirstNInstances().size() + " does not match actual compared value " + right.getFirstNInstances().size());
        }
        int leftSize = left.getFirstNInstances() == null ? 0 : left.getFirstNInstances().size();
        int rightSize = right.getFirstNInstances() == null ? 0 : right.getFirstNInstances().size();
        int size = Math.max(leftSize, rightSize);
        for (int i = 0; i < size; i++) {
            if (i < leftSize && i < rightSize) {
                FailureDetail leftFd = left.getFirstNInstances().get(i);
                FailureDetail rightFd = right.getFirstNInstances().get(i);
                if (!leftFd.equals(rightFd)) {
                    System.out.println(DIFF + left + "\n\t expected Failure Detail " + (i + 1) + ": " + leftFd + " does not match actual compared value " + rightFd);
                    return;
                }
            } else if (i >= leftSize) {
                FailureDetail rightFd = right.getFirstNInstances().get(i);
                System.out.println(DIFF + left + "\n\t unexpected extra actual Failure Detail " + (i + 1) + ": " + rightFd);
            } else if (i >= rightSize) {
                FailureDetail leftFd = left.getFirstNInstances().get(i);
                System.out.println(DIFF + left + "\n\t actual results missing expected Failure Detail " + (i + 1) + ": " + leftFd);
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

    @AfterEach
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
            } else if (!assertionName.equals(other.assertionName)) {
                return false;
            }

            if (assertionUuid == null) {
                if (other.assertionUuid != null)
                    return false;
            } else if (!assertionUuid.equals(other.assertionUuid)) {
                return false;
            }

            if (firstNInstances == null && other.firstNInstances != null) {
                return false;
            } else if (other.firstNInstances == null && firstNInstances != null) {
                return false;
            } else if (firstNInstances != null && other.firstNInstances != null) {
                Collections.sort(firstNInstances);
                Collections.sort(other.firstNInstances);
                if (!firstNInstances.equals(other.firstNInstances)) {
                    return false;
                }
            }
            //Final case that will drop through is that both are null

            return totalFailed == other.totalFailed;
        }

        @Override
        public int compareTo(final RVFTestResult o) {
            return this.assertionUuid.compareTo(o.getAssertionUuid());
        }

        @Override
        public String toString() {
            return assertionUuid.toString() + ":" + assertionName + " [" + totalFailed + " failures, " + (firstNInstances == null ? "NULL" : firstNInstances.size()) + " examples]";
        }
    }
}
