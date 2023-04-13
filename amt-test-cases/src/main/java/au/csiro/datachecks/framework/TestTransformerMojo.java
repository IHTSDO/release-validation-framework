package au.csiro.datachecks.framework;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Mojo that loads a set of files, containing datacheck format tests,
 * and then outputs the test queries as RVF script definitions
 * 
 * @goal transform-testcases
 */
public class TestTransformerMojo extends AbstractMojo {

    /**
     * Defines the test suites to be executed.
     * Defaults to ALL which means "all suites". Can use "NONE" which means
     * none of the defined suites. Can also specify a list of regular expressions
     * matching the suite names to be executed - see {@link TestCaseTransformer} for more details.
     *
     * @parameter default-value="ALL"
     */
    private String testSuitesToExecute;

    /**
     * Specifies the location of the test suites (TestNG XML files) to run
     *
     * @parameter default-value="${project.build.directory}/datachecks"
     */
    private File testSuiteDirectory;

    /**
     * Specifies the location where the final RVF test script will be written out
     *
     * @parameter default-value="${project.build.directory}/runRvf.sh"
     */
    private File outputRvfScript;

    /**
     * Specifies the location where the pre-requisite SQL file exists
     *
     * @parameter default-value="${project.build.directory}/datachecks/pre-requisites.sql"
     */
    private File preRequisitesScript;

    public void execute() throws MojoExecutionException, MojoFailureException {
        TestCaseTransformer transformer = new TestCaseTransformer();
        transformer.setTestSuitesToExecute(this.testSuitesToExecute);
        transformer.setPreRequisitesScript(this.preRequisitesScript);
        transformer.transformTests(this.outputRvfScript, this.testSuiteDirectory);
    }

    public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
        TestTransformerMojo mojo = new TestTransformerMojo();
        mojo.outputRvfScript = new File("/tmp/runAmtRvf.sh");
        mojo.preRequisitesScript = new File("release-validation-framework/amt-test-cases/datachecks/pre-requisites.sql");
        mojo.testSuiteDirectory = new File("release-validation-framework/amt-test-cases/datachecks/");
        mojo.testSuitesToExecute = "ALL";
        mojo.execute();
    }
}
