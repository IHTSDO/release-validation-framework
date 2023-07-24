package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.validation.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

public class RF2FilesReleaseTypeTesterTest {

    @org.junit.jupiter.api.Test
    public void testRunTestNoError() throws URISyntaxException {
        TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
        String manifestFilename = "/manifest_20250731.xml";
        URL url = getClass().getResource(manifestFilename);
        assertNotNull(url);
        File manifestFile = new File(url.toURI());
        RF2FilesReleaseTypeTester tester = new RF2FilesReleaseTypeTester(new TestValidationLogImpl(RF2FilesReleaseTypeTester.class)
                , new ManifestFile(manifestFile), testReport);
        tester.runTest();
        assertEquals("Release files are expected to be in correct release folders", 0, testReport.getNumErrors());
    }

    @Test
    public void testRunTestWithError() throws URISyntaxException {
        TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
        String manifestFilename = "/manifest_20250731_Invalid.xml";
        URL url = getClass().getResource(manifestFilename);
        assertNotNull(url);
        File manifestFile = new File(url.toURI());
        RF2FilesReleaseTypeTester tester = new RF2FilesReleaseTypeTester(new TestValidationLogImpl(RF2FilesReleaseTypeTester.class)
                , new ManifestFile(manifestFile), testReport);
        tester.runTest();
        assertEquals("Release files are expected to be in incorrect release folders", 3, testReport.getNumErrors());
    }
}
