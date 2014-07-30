package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.log.TestValidationLogImpl;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ColumnPatternTesterTest {

    private ColumnPatternTester tester;
    private TestReport testReport;

    @Test
    public void testFileNotFound() throws Exception {
        ResourceManager resourceManager = new TestFileResourceProvider(new File(""));
        testReport = new TestReport(new CsvResultFormatter());
        tester = new ColumnPatternTester(new TestValidationLogImpl(ColumnPatternTester.class), resourceManager, testReport);

        tester.runTests();

        assertEquals(1, testReport.getNumErrors());
        assertEquals(0, testReport.getNumSuccesses());
    }

    @Test
    public void testInvalidId() throws Exception {
        String filename = "/der2_sRefset_SimpleMapDelta_INT_20140131.txt";
        executeRun(filename);

        assertEquals("the 2 invalid ids", 2, testReport.getNumErrors());
        assertEquals(228, testReport.getNumSuccesses());
        System.out.println(testReport.getResult());
    }

    @Test
    public void testAssociationRefSet() throws Exception {
        String filename = "/der2_cRefset_AssociationReferenceDelta_20140731.txt";
        executeRun(filename);

        assertEquals("Not an RF2 specification file", 1, testReport.getNumErrors());
        assertEquals(0, testReport.getNumSuccesses());
        System.out.println(testReport.getResult());
    }

    @Test
    public void testOnlyHeaders() throws Exception {
        String filename = "/der2_Refset_SimpleDelta_INT_20140131.txt";
        executeRun(filename);

        assertEquals(0, testReport.getNumErrors());
        assertEquals("The column headers should have been fine, there are 6 of them", 6, testReport.getNumSuccesses());
    }

    @Test
    public void testEmptyEffectiveTime() throws Exception {
        String filename = "/sct2_TextDefinition_Delta-en_INT_20140731.txt";
        executeRun(filename);

        assertEquals("Invalid Effective Time there are 9 rows hence 9 errors", 9, testReport.getNumErrors());
    }
    @Test
    public void testComplexMapEmptyId() throws Exception {
        String filename = "/der2_iissscRefset_ComplexMapDelta_INT_20140731.txt";
        executeRun(filename);

        assertEquals("No id by 9 rows", 9, testReport.getNumErrors());
    }
    @Test
    public void testExtraColumn() throws Exception {
        String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140731.txt";
        executeRun(filename);

        assertEquals("Only 4 as we abort the rest", 4, testReport.getNumErrors());
    }
    @Test
    public void testExtraColumnAtEnd() throws Exception {
        String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140831.txt";
        executeRun(filename);

        assertEquals("only the 4 errors i column 3 rows", 4, testReport.getNumErrors());
    }
    @Test
    public void testInvalidColumnName() throws Exception {
        String filename = "/sct2_Relationship_Full_INT_20140731.txt";
        executeRun(filename);

        assertEquals("Just the header", 1, testReport.getNumErrors());
    }
    @Test
    public void testInvalidModuleId() throws Exception {
        String filename = "/sct2_StatedRelationship_Snapshot_INT_20140731.txt";
        executeRun(filename);

        assertEquals("9 rows contain this invalid module id", 9, testReport.getNumErrors());
    }
    @Test
    public void testMissingId() throws Exception {
        String filename = "/der2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
        executeRun(filename);

        assertEquals("1 header + 9 rows contain missing id", 10, testReport.getNumErrors());
    }
    @Test
    public void testMissingModuleId() throws Exception {
        String filename = "/der2_cRefset_LanguageDelta-en_INT_20140731.txt";
        executeRun(filename);

        assertEquals("1 header + 9 rows contain missing id", 10, testReport.getNumErrors());
    }
    @Test
    public void testMixedErrors() throws Exception {
        String filename = "/der2_cRefset_LanguageSnapshot-en_INT_20140731.txt";
        executeRun(filename);

        assertEquals("1 header + 9 rows contain missing id", 32, testReport.getNumErrors());
    }
    public void executeRun(String filename) throws URISyntaxException {
        File f = new File(getClass().getResource(filename).toURI());

        ResourceManager resourceManager = new TestFileResourceProvider(f);
        testReport = new TestReport(new CsvResultFormatter());
        tester = new ColumnPatternTester(new TestValidationLogImpl(ColumnPatternTester.class), resourceManager, testReport);

        tester.runTests();
    }


    class TestFileResourceProvider implements ResourceManager {

        public TestFileResourceProvider(File file) {
            this.file = file;
            fileNames.add(file.getName());
        }

        @Override
        public BufferedReader getReader(String name, Charset charset) throws IOException {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        }

        @Override
        public String getFilePath() {
            return file.getAbsolutePath();
        }

        @Override
        public List<String> getFileNames() {
            return fileNames;
        }

        private List<String> fileNames = new ArrayList<>();
        private File file;
    }

}
