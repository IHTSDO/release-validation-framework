package org.ihtsdo.rvf.validation;

import org.apache.commons.io.monitor.FileEntry;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.ihtsdo.rvf.validation.impl.CsvMetadataResultFormatter;
import org.ihtsdo.rvf.validation.impl.StreamTestReport;
import org.ihtsdo.rvf.validation.log.impl.TestValidationLogImpl;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
import org.junit.Test;

import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
	
	@Test
	public void testNormalization() throws Exception {
		TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), false);
		String manifestFilename = "/manifest_ee_test.xml";
		File manifestFile = new File(getClass().getResource(manifestFilename).toURI());
		File releasePackage = File.createTempFile("TestRelease", ".zip");
		try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(releasePackage), StandardCharsets.UTF_8);
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			outputStream.putNextEntry(new ZipEntry("Test/"));
			ZipEntry fileEntry = new ZipEntry("Test/xder2_Refset_eestiKukkumisePõhjuseKlassifikaatorSimpleRefsetDelta_EE1000181_20190614.txt");
			outputStream.putNextEntry(fileEntry);
			
			writer.write("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId");
			writer.newLine();
			writer.flush();
			outputStream.closeEntry();
		}
		ManifestPatternTester tester = new ManifestPatternTester(new TestValidationLogImpl(ManifestPatternTester.class),
				new ZipFileResourceProvider(releasePackage), new ManifestFile(manifestFile), testReport);
		tester.runTests();
		assertEquals("There should be no errors", 0, testReport.getNumErrors());
		releasePackage.delete();
	}

}
