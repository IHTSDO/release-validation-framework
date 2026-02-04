package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.configuration.IntegrationTest;
import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.resource.TextFileResourceProvider;
import org.ihtsdo.rvf.core.service.structure.resource.ZipFileResourceProvider;
import org.ihtsdo.rvf.core.service.structure.validation.StructuralTestRunner;
import org.ihtsdo.rvf.core.service.structure.validation.TestReportable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class StructuralTestRunnerTest extends IntegrationTest {

	@Autowired
	private StructuralTestRunner validationRunner;

	@Test
	public void testExecute_DataInResponse() throws Exception {
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile("/SnomedCT_Release_INT_20140831.zip"));
		TestReportable response = validationRunner.execute(provider, new TestWriterDelegate(new StringWriter()), false);

		assertNotNull(response.getResult());
		System.out.println(response.getResult());
		assertEquals(0, response.getNumErrors());
	}

	@Test
	public void testExecute_ExdRefSet() throws Exception {
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile("/der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt.zip"));

		TestReportable response = validationRunner.execute(provider, new TestWriterDelegate(new StringWriter()), true);

		assertNotNull(response.getResult());
		assertEquals(0, response.getNumErrors(), "no errors expected");
	}

	@Test
	public void testExecute_rel2SimpleRefset() throws Exception {
		String fileName = "der2_Refset_SimpleDelta_INT_20140131.txt";
		TextFileResourceProvider provider = new TextFileResourceProvider(getFile("/" + fileName), fileName);

		TestReportable response = validationRunner.execute(provider, new TestWriterDelegate(new StringWriter()), true);

		assertNotNull(response.getResult());
		assertEquals(0, response.getNumErrors());
	}

	@Test
	public void testExecute_validPostCondition_NoErrors() throws Exception {
		String fileName = "/SnomedCT_Release_INT_20140928.zip";
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile(fileName));

		TestReportable response = validationRunner.execute(provider, new TestWriterDelegate(new StringWriter()), false);
		assertNotNull(response.getResult());
		assertEquals(3444, response.getNumTestRuns(), "There should be 3444 tests in total");
		assertEquals(22, response.getNumErrors(), "There should be 6 file names that are not valid plus 16 other errors");
	}

	@Test
	public void testExecute_validPostCondition_Streaming() throws Exception {
		String fileName = "/SnomedCT_Release_INT_20140928.zip";
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile(fileName));

		StringWriter sw = new StringWriter();
		PrintWriter bos = new PrintWriter(sw);

		TestReportable response = validationRunner.execute(provider, bos, false);
		// check bos contains all our info
		assertTrue(sw.getBuffer().length() > 0);

		assertNotNull(response.getResult());
		System.out.println(response.getResult());
		assertEquals(22, response.getNumErrors(), "There are 6 file names that are not valid plus 16 other errors");
	}

	@Test
	public void testExecuteWithManifest() throws Exception {
		String fileName = "/SnomedCT_Release_INT_20140831.zip";
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile(fileName));
		ManifestFile manifestFile = new ManifestFile(getFile("/manifest_20250731.xml"));

		TestReportable response = validationRunner.execute(provider, null, new TestWriterDelegate(new StringWriter()), true, manifestFile);

		assertNotNull(response.getResult());
		assertEquals(54, response.getNumErrors(), "Should only be manifest errors in this");
	}

	private File getFile(String testFileName) throws URISyntaxException {
		URL zipUrl = StructuralTestRunner.class.getResource(testFileName);
		assertNotNull(zipUrl);
		return new File(zipUrl.toURI());
	}
}
