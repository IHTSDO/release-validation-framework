package au.csiro.datachecks.framework;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to transform datacheck tests into RVF tests case definitions
 */
public class TestCaseTransformer {

    private static Logger log = Logger.getLogger(TestCaseTransformer.class.getName());

    /**
     * Value to pass to {@link #setTestSuitesToExecute(String)} to indicate that no
     * suites are to be matched and executes
     */
    public static final String NO_SUITES = "NONE";
    
    /**
     * Value to pass to {@link #setTestSuitesToExecute(String)} to indicate that all
     * suites are to be matched and executed
     */
    public static final String ALL_SUITES = "ALL";

    public static final String DEFAULT_SUITE_INCLUSION_PATTERN = "*.xml";

    private String testSuitesToExecute = ALL_SUITES;

    int testCount = 0;

    int activeTestCount = 0;

    private File preRequisitesScript;

    /**
     * Transforms all the test suites found at the specified testSuiteDirectory
     * and transforms them to the testCaseOutputFile
     *
     * @param testCaseOutputFile
     * @param testSuiteDirectory
     */
    public void transformTests(File testCaseOutputFile, File testSuiteDirectory) {
        testCaseOutputFile.getParentFile().mkdirs();
        List<XmlSuite> suites = parseTestSuitesFromDirectory(testSuiteDirectory);
        if (suites.isEmpty()) {
            log.warning("No suites found for execution!");
            return;
        }
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(testCaseOutputFile))) {
            fw.write(SCRIPT_HEADER);
            for (XmlSuite suite : suites) {
                transformTestSuite(suite, fw);
            }
            fw.write(SCRIPT_FOOTER);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to write test case definitions to file: " + testCaseOutputFile.getAbsolutePath(), e);
        }
        log.info("Active tests: " + activeTestCount + "/" + testCount);
        log.info("Done transforming suites ");

        log.info("Active Test Cases:\n\n" + StringUtils.join(testSql, "\n"));
    }

    private void transformTestSuite(XmlSuite suite, BufferedWriter fileWriter) throws IOException {
        List<XmlTest> theTests = new ArrayList<>();
        int i = 0;

        fileWriter.append("  # -- CREATE AN ASSERTION --\n" +
                "  assertionid=`curl \"${RVF_ENDPOINT}/assertions\" \\\n" +
                "    -s \\\n" +
                "    -X 'POST' \\\n" +
                "    -H 'Accept: */*' \\\n" +
                "    -H \"${AUTH_HEADER}\" \\\n" +
                "    -H 'Content-Type: application/json' \\\n" +
                "    --data-raw $'{\\n    \"name\": \"" + escapeScriptString(suite.getName()) + "\",\\n    \"statement\": null,\\n    \"description\": \"SUITE: " + new File(suite.getFileName()).getName() + "\",\\n    \"docLink\": null,\\n    \"effectiveFrom\": null,\\n    \"keywords\": null,\\n    \"groups\": []\\n}' \\\n" +
                "    --compressed | egrep -o \"assertionId\\\"[ ]*:[ ]*[0-9]+,\" | egrep -o '[0-9]+'`\n" +
//                "  sleep 1\n" +
                "  # -- ADD ASSERTION TO GROUP --\n" +
                "  curl \"${RVF_ENDPOINT}/groups/$groupid/assertions\" \\\n" +
                "    -s \\\n" +
                "    -X 'POST' \\\n" +
                "    -H 'Accept: */*' \\\n" +
                "    -H \"${AUTH_HEADER}\" \\\n" +
                "    -H 'Content-Type: application/json' \\\n" +
                "    --data-raw \"['$assertionid']\" \\\n" +
                "    --compressed || true\n");
//                "  sleep 1\n");

        for (XmlTest test : suite.getTests()) {
            if (test.getName() == null || test.getName().trim().isEmpty() || test.getName().length() > 4000) {
                test.setName(suite.getName() + " - test " + i);
                i++;
            }
            test.addParameter("test-name", test.getName());
            test.addParameter("description", test.getName());
            testCount++;
            if (test.getParameter("skip") == null ||
                    !test.getParameter("skip").equalsIgnoreCase("true")) {
                String expectedValue = test.getParameter("resultCount");
                if (expectedValue == null ||
                        expectedValue.equalsIgnoreCase("null") ||
                        expectedValue.equals("0")) {
                    activeTestCount++;
                    theTests.add(test);
                    String query = test.getParameter("query");
                    String escapedQuery = escapeScriptString(query);
                    if (query != null && !query.trim().isEmpty() && escapedQuery.length() <= 4000) {

                        testSql.add("-- " + test.getName());
                        testSql.add(query);
                        testSql.add("");

                        fileWriter.append("  # -- CREATE TEST --\n" +
                                "  curl \"${RVF_ENDPOINT}/assertions/${assertionid}/tests\" \\\n" +
                                "    -s \\\n" +
                                "    -X 'POST' \\\n" +
                                "    -H 'Accept: */*' \\\n" +
                                "    -H \"${AUTH_HEADER}\" \\\n" +
                                "    -H 'Content-Type: application/json' \\\n" +
                                "    --data-raw $'[\\n   {\\n       \"id\":null,\\n       \"name\":\"" + escapeScriptString(test.getName()) + "\",\\n       \"description\":\"" + escapeScriptString(test.getName()) + "\",\\n       \"type\":\"SQL\",\\n       \"command\":{\"id\":null,\\n           \"configuration\":{\"id\":null,\"items\":[],\"keys\":[]},\\n           \"template\":\"" +
                                escapedQuery +
                                "\",\\n           \"statements\":[]}\\n   }\\n]' \\\n" +
                                "    --compressed  > /dev/null\n");
//                                "  sleep 1\n");
                    }
                } else {
                    log.warning("Test " + test.getName() + " has a resultCount parameter of " + expectedValue + " - this is not supported by RVF");
                }
            }
        }
        suite.setTests(theTests);
        for (XmlSuite childSuite : suite.getChildSuites()) {
            transformTestSuite(childSuite, fileWriter);
        }
    }

    List<String> testSql = new ArrayList<>();

    private String escapeScriptString(String str) {
        return str.replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\\\'");
    }

    @SuppressWarnings("unchecked")
    private List<XmlSuite> parseTestSuitesFromDirectory(File testSuiteDirectory) {
        List<XmlSuite> suites = new ArrayList<>();
        try {
            boolean ignoreAllSuites = NO_SUITES.equalsIgnoreCase(getTestSuitesToExecute());
            if (!ignoreAllSuites) {
                
                boolean executeAllSuites = ALL_SUITES.equalsIgnoreCase(getTestSuitesToExecute());
                String inclusionPattern = executeAllSuites ? DEFAULT_SUITE_INCLUSION_PATTERN : getTestSuitesToExecute();
                
                for (File file : (List<File>) FileUtils.getFiles(testSuiteDirectory, inclusionPattern, null)) {
                    try {
                        log.info("Adding suite " + testSuiteDirectory + File.separator + file.getName());
                        suites.addAll(new Parser(testSuiteDirectory + File.separator + file.getName()).parse());
                    } catch (ParserConfigurationException | SAXException e) {
                        log.log(Level.SEVERE, "Failed to parse test suites due to exception", e);
                        log.log(Level.SEVERE, "BUILD FAILED");
                    }
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to parse test suites due to exception", e);
            log.log(Level.SEVERE, "BUILD FAILED");
        }
        return suites;
    }

    /**
     * @return Should be "ALL", "NONE" or a series of comma separated ANT file patterns used to determine
     * which test suites files to execute.
     * @see #setTestSuitesToExecute(String)
     */
    public String getTestSuitesToExecute() {
        return testSuitesToExecute;
    }

    /**
     * Sets the series of comma separated ANT file patterns used to determine
     * which test suites files to execute. Test suite files with names
     * matching one of the patterns will be loaded, others are ignored. 
     * <p> A value of "ALL" will match all filenames using {@link #DEFAULT_SUITE_INCLUSION_PATTERN}. 
     * <p> A value of "NONE" will ensure no test suite files are loaded.
     * 
     * @param testSuitesToExecute
     */
    public void setTestSuitesToExecute(String testSuitesToExecute) {
        this.testSuitesToExecute = testSuitesToExecute;
    }

    String SCRIPT_HEADER = "#!/bin/sh\n" +
            "set -e\n" +
            "RVF_ENDPOINT=${1:-'http://localhost:8081/api'}\n" +
            "RVF_USER='user'\n" +
            "RVF_PASSWORD='password'\n" +
            "AUTH=`echo -n \"${RVF_USER}:${RVF_PASSWORD}\" | base64`\n" +
            "AUTH_HEADER=\"Authorization: Basic ${AUTH}\"\n" +
            "RC_PATH=${2:-'/app/store/rc/CSIRO_AUEdition_PRODUCTION_32506021000036107-20221031T100001Z.zip'}\n" +
            "PRE_REQ_SQL_PATH=${3:-'/app/store/pre-requisites.sql'}\n" +
            "echo \"Create Test assertions\"\n" +
            "  # -- CREATE AN AMT ASSERTION GROUP --\n" +
            "  groupid=`curl \"${RVF_ENDPOINT}/groups?name=amt\" \\\n" +
            "    -s \\\n" +
            "    -X 'POST' \\\n" +
            "    -H 'Accept: */*' \\\n" +
            "    -H \"${AUTH_HEADER}\" \\\n" +
            "    -H 'Content-Length: 0' \\\n" +
            "    -H 'Content-Type: application/json' \\\n" +
            "    --compressed | egrep -o \"id\\\"[ ]*:[ ]*[0-9]+,\" | egrep -o '[0-9]+'`\n" +
            "  echo \"Amt assertioun group id is: $groupid\"\n" +
            "  sleep 1\n";

    String SCRIPT_FOOTER = "  TEST_RUN_ID=`date +%y%m%d%H%M%S`\n" +
            "  echo -e \"Executing test run:\\t$TEST_RUN_ID\"\n" +
            "  STORAGE_LOCATION=\"rctest-$TEST_RUN_ID\"\n" +
            "  # -- START RVF EXECUTION --\n" +
            "  test=`curl --location --request POST \"${RVF_ENDPOINT}/run-post?rf2DeltaOnly=false&groups=amt&runId=$TEST_RUN_ID&failureExportMax=10&storageLocation=$STORAGE_LOCATION&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false\" \\\n" +
            "  -s \\\n" +
            "  --header 'Accept: */*' \\\n" +
            "  --header \"${AUTH_HEADER}\" \\\n" +
            "  --form \"file=@${RC_PATH}\" \\\n" +
            "  --form \"preRequisiteSql=@${PRE_REQ_SQL_PATH}\"`\n" +
            "  echo $test\n" +
            "  echo \"Waiting for validation to finish\"\n" +
            "  IS_FINISHED=\"nah\"\n" +
            "  while [ -n \"$IS_FINISHED\" ]\n" +
            "  do\n" +
            "    results=`curl -s -X GET --header 'Accept: application/json' --header \"${AUTH_HEADER}\" \"${RVF_ENDPOINT}/result/$TEST_RUN_ID?storageLocation=$STORAGE_LOCATION\"`\n" +
            "    echo \"$results\"\n" +
            "    IS_FINISHED=`echo \"$results\" | grep -E '\"RUNNING\"|\"QUEUED\"' || true`\n" +
            "    sleep 20\n" +
            "  done\n" +
            "TOTAL_FAILS=`echo \"$results\" | jq '.rvfValidationResult.TestResult.totalFailures'`\n" +
            "echo \"Validation finished successfully with $TOTAL_FAILS fails!\"\n" +
            "if [ \"$TOTAL_FAILS\" != \"0\" ]; then\n" +
            "  exit 1\n" +
            "fi\n" +
            "exit 0\n";

    public void setPreRequisitesScript(File preRequisitesScript) {
        this.preRequisitesScript = preRequisitesScript;
    }
}
