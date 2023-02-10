package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.pojo.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.validation.*;
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
