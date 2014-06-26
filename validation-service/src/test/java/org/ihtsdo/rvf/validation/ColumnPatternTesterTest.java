package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.log.TestValidationLogImpl;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.Column;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ColumnPatternTesterTest {

    private ColumnPatternTester tester;
    private ValidationLog validationLog;
    private ColumnPatternConfiguration configuration;
    private ResourceManager resourceManager;
    private TestReport testReport;

    @Before
    public void setup() {
        validationLog = new TestValidationLogImpl(ColumnPatternTester.class);
        configuration = new ColumnPatternConfiguration();
        resourceManager = new TestFileResourceProvider(new File(""));
        testReport = new TestReport(new CsvResultFormatter());
        tester = new ColumnPatternTester(validationLog, configuration, resourceManager, testReport);
    }

    @Test
    public void testMissingFileName() throws Exception {
        ColumnPatternConfiguration.File file = new ColumnPatternConfiguration.File();
        Column column = new Column();
        column.setName("id");
        column.setSctid("");
        file.getColumn().add(column);
        configuration.getFile().add(file);
        tester.runTests();

        assertEquals(1, testReport.getErrorCount());
        assertEquals(0, testReport.getNumSuccesses());
    }

    class TestFileResourceProvider implements ResourceManager {
        public TestFileResourceProvider(File file) {
            this.file = file;
        }

        @Override
        public BufferedReader getReader(String name, Charset charset) throws IOException {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        }

        @Override
        public boolean isFile(String filename) {
            return file == null || !file.isDirectory();
        }

        @Override
        public String getFilePath() {
            return file.getAbsolutePath();
        }

        private File file;
    }
}