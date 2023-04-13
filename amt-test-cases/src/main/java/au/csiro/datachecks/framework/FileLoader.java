package au.csiro.datachecks.framework;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class used to load files for datachecks.
 * <p>
 * The {@link #loadTestDataFiles(DatabaseConnectionSettings, List, File)} method can be used to load a list of
 * {@link TestFileSet} objects using the schema files found in the directory specified by the File parameter into the
 * database specified by the {@link DatabaseConnectionSettings} parameter.
 * <p>
 * The {@link #setTestFilesToLoad(String)} method can be used to specify which of the {@link TestFileSet} objects passed
 * to {@link #loadTestDataFiles(DatabaseConnectionSettings, List, File)} will actually be loaded. The
 * {@link #setTestFilesToLoad(String)} method accepts a {@link String} which is a list of comma separated regular
 * expressions. Simply if the name of the {@link TestFileSet} matches one of the regular expressions it is loaded, if
 * not then it isn't loaded. This value set by this method defaults to a special value "ALL" which matches all
 * {@link TestFileSet} names. The other special value is "NONE" which matches none of the names.
 */
public class FileLoader {

    public static final String NO_FILES = "NONE";
    public static final String ALL_FILES = "ALL";

    private static final String DELIMITER = "//";
    private static final String TOKEN = "@";
    public static final String RF2_PREFIX = "der2_";
    public static final String RF2_PREFIX_REGEX = ".?" + RF2_PREFIX + ".*";
    private static final String lineSeparator = System.getProperty("line.separator");

    public static Logger log = Logger.getLogger(FileLoader.class.getName());

    private String testFilesToLoad = ALL_FILES;
    private Set<String> testFileSetNames = new HashSet<>();

    /**
     * Sets the series of comma separated regular expressions used to determine
     * which {@link TestFileSet}s to load - {@link TestFileSet}s whose names
     * match one of the regular expressions are loaded, others are ignored. A
     * value of "ALL" will match all {@link TestFileSet} names. A value of
     * "NONE" will ensure no {@link TestFileSet}s will be loaded.
     * 
     * @param testFilesToLoad
     */
    public void setTestFilesToLoad(String testFilesToLoad) {
        this.testFilesToLoad = testFilesToLoad;
    }

    /**
     * Loads the specified set of test files to the database specified by the
     * databaseConnectionSettings parameter.
     * <p>
     * This method will go through each of the {@link TestFileSet} objects in the testFiles parameter and load the file
     * set into the database provided the {@link TestFileSet}'s name matches the {@link #testFilesToLoad} patterns,
     * which can be set with {@link #setTestFilesToLoad(String)}.
     * <p>
     * When loading the files each {@link TestFileSet} has a specified file type - this will be matched against the
     * available schema types from the available schemas at schemaDirectory. For a schema to be used, the
     * {@link TestFileSet}'s file type must match the start of the schema file's name up to the first underscore.
     *
     * @param testFiles*
     * @throws InterruptedException
     */
    public void loadTestDataFiles(List<TestFileSet> testFiles) throws InterruptedException {
        validateTestFileConfiguration(testFiles);
        extractArtifacts(testFiles);
    }

    private void validateTestFileConfiguration(List<TestFileSet> testFiles) {
        if (testFiles.isEmpty()) {
            throw new RuntimeException("Cannot test - no file sets specified!");
        }
        for (TestFileSet testFileSet : testFiles) {
            if ((testFileSet.getName().equals("")) || (this.testFileSetNames.contains(testFileSet.getName()))) {
                throw new RuntimeException(
                    "Test file set name must not be empty and must be unique - name in violation was '"
                            + testFileSet.getName() + "'");
            }

            this.testFileSetNames.add(testFileSet.getName());
            String releaseFileFormat = testFileSet.getType();
            if (!releaseFileFormat.equals("rf2")) {
                throw new RuntimeException("Cannot handle test file set file type '" + releaseFileFormat
                        + "' is unknown. Known types are: RF2");
            }
        }

        log.info("All files " + this.testFileSetNames + " pass validation");
    }

    private void extractArtifacts(List<TestFileSet> testFiles) {
        for (TestFileSet fileSet : testFiles) {
            for (Artifact artifact : fileSet.getExtracts()) {
                artifact.getAndExtractFile();
            }
        }
    }
}
