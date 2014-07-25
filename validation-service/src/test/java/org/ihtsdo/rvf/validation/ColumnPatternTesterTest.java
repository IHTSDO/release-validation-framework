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

        assertEquals(1, testReport.getErrorCount());
        assertEquals(0, testReport.getNumSuccesses());
    }

    @Test
    public void testInvalidId() throws Exception {
        String filename = "/der2_sRefset_SimpleMapDelta_INT_20140131.txt";
        executeRun(filename);

        assertEquals("the 2 invalid ids", 2, testReport.getErrorCount());
        assertEquals(228, testReport.getNumSuccesses());
        System.out.println(testReport.getResult());
    }

    @Test
    public void testAssociationRefSet() throws Exception {
        String filename = "/der2_cRefset_AssociationReferenceDelta_20140731.txt";
        executeRun(filename);

        assertEquals("Not an RF2 specification file", 1, testReport.getErrorCount());
        assertEquals(0, testReport.getNumSuccesses());
        System.out.println(testReport.getResult());
    }

    @Test
    public void testOnlyHeaders() throws Exception {
        String filename = "/der2_Refset_SimpleDelta_INT_20140131.txt";
        executeRun(filename);

        assertEquals(0, testReport.getErrorCount());
        assertEquals("The column headers should have been fine, there are 6 of them", 6, testReport.getNumSuccesses());
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
