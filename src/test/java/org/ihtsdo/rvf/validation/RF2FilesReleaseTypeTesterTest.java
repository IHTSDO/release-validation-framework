package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.impl.CsvMetadataResultFormatter;
import org.ihtsdo.rvf.validation.impl.StreamTestReport;
import org.ihtsdo.rvf.validation.log.impl.TestValidationLogImpl;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static junit.framework.TestCase.assertEquals;

public class RF2FilesReleaseTypeTesterTest {

    @Test
    public void testRunTestNoError() throws URISyntaxException {
        TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
        String manifestFilename = "/manifest_20250731.xml";
        File manifestFile = new File(getClass().getResource(manifestFilename).toURI());
        RF2FilesReleaseTypeTester tester = new RF2FilesReleaseTypeTester(new TestValidationLogImpl(RF2FilesReleaseTypeTester.class)
                , new ManifestFile(manifestFile), testReport);
        tester.runTest();
        assertEquals("Release files are expected to be in correct release folders", 0, testReport.getNumErrors());
    }

    @Test
    public void testRunTestWithError() throws URISyntaxException {
        TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
        String manifestFilename = "/manifest_20250731_Invalid.xml";
        File manifestFile = new File(getClass().getResource(manifestFilename).toURI());
        RF2FilesReleaseTypeTester tester = new RF2FilesReleaseTypeTester(new TestValidationLogImpl(RF2FilesReleaseTypeTester.class)
                , new ManifestFile(manifestFile), testReport);
        tester.runTest();
        assertEquals("Release files are expected to be in incorrect release folders", 3, testReport.getNumErrors());
    }
}
