package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.impl.CsvMetadataResultFormatter;
import org.ihtsdo.rvf.validation.impl.StreamTestReport;
import org.ihtsdo.rvf.validation.log.impl.TestValidationLogImpl;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;

import static junit.framework.TestCase.assertEquals;

public class ManifestRefsetTesterTest {

    @Test
    public void testRunTestsNoError() throws Exception {
        TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
        String manifestFilename = "/manifest_20140131_updated_format.xml";
        String packageName = "/SnomedCT_Release_INT_20140131.zip";
        File f = new File(getClass().getResource(manifestFilename).toURI());
        File zipFile = new File(getClass().getResource(packageName).toURI());
        ManifestRefsetTester tester = new ManifestRefsetTester(new TestValidationLogImpl(ManifestRefsetTester.class),
        new ZipFileResourceProvider(zipFile), new ManifestFile(f), testReport);
        tester.runTests();
        assertEquals("No file having refsets differ from refsets listed in manifest", 0, testReport.getNumErrors());
    }

}
