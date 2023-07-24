package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.resource.ZipFileResourceProvider;
import org.ihtsdo.rvf.core.service.structure.validation.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

public class ManifestPatternTesterTest {

	@Test
	public void testRunTests() throws Exception {

		TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), true);
		String manifestFilename = "/manifest_20250731.xml";
		String packageName = "/SnomedCT_Release_INT_20140831.zip";
		URL url = getClass().getResource(manifestFilename);
		assertNotNull(url);
		File f = new File(url.toURI());
		url = getClass().getResource(packageName);
		assertNotNull(url);
		File zipFile = new File(url.toURI());
		ManifestPatternTester tester = new ManifestPatternTester(new TestValidationLogImpl(ManifestPatternTester.class),
				new ZipFileResourceProvider(zipFile), new ManifestFile(f), testReport);
		tester.runTests();

		assertEquals("errors should include: 1) 1 missing Terminology under full + 5 files, " +
				"10 under refset/Map, language and Metadata, 2 under Content, 6 under snapshot " +
				"12 under Snapshot/Refset/Map, language, Metadata, 18 under delta", 54, testReport.getNumErrors());

	}
	
	@org.junit.jupiter.api.Test
	public void testNormalization() throws Exception {
		TestReportable testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), false);
		String manifestFilename = "/manifest_ee_test.xml";
		URL url = getClass().getResource(manifestFilename);
		assertNotNull(url);
		File manifestFile = new File(url.toURI());
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
		Assertions.assertTrue(releasePackage.delete());
	}

}
