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

public class ManifestPatternTesterTest {

	@Test
	public void testRunTests() throws Exception {

		TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
		String manifestFilename = "/manifest_20250731.xml";
		String packageName = "/SnomedCT_Release_INT_20140831.zip";
		File f = new File(getClass().getResource(manifestFilename).toURI());
		File zipFile = new File(getClass().getResource(packageName).toURI());
		ManifestPatternTester tester = new ManifestPatternTester(new TestValidationLogImpl(ManifestPatternTester.class),
				new ZipFileResourceProvider(zipFile), new ManifestFile(f), testReport);
		tester.runTests();

		assertEquals("errors should include: 1) 1 missing Terminology under full + 5 files, " +
				"10 under refset/Map, language and Metadata, 2 under Content, 6 under snapshot " +
				"12 under Snapshot/Refset/Map, language, Metadata, 18 under delta", 54, testReport.getNumErrors());

	}

}
