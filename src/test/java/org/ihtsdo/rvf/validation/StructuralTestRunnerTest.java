package org.ihtsdo.rvf.validation;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.TextFileResourceProvider;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class StructuralTestRunnerTest {

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
		assertEquals("no errors expected", 0, response.getNumErrors());
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
		String[] invalidFileNames = {"sct2_Concept_Delta_INT_20140131_10.txt", "sct2_Concept_Full_INT_20140131_test.txt", "sct2_Concept_Full_INT_20140131_UUID.txt"};

		assertNotNull(response.getResult());
		assertEquals("There should be 3444 tests in total", 3444, response.getNumTestRuns());
		assertEquals("There are 3 file names that are not valid plus 4 other errors", 7, response.getNumErrors());
	}

	@Test
	public void testExecute_validPostCondition_Streaming() throws Exception {
		String fileName = "/SnomedCT_Release_INT_20140928.zip";
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile(fileName));

		StringWriter sw = new StringWriter();
		PrintWriter bos = new PrintWriter(sw);

		TestReportable response = validationRunner.execute(provider, bos, false);
		String[] invalidFileNames = {"sct2_Concept_Delta_INT_20140131_10.txt", "sct2_Concept_Full_INT_20140131_test.txt", "sct2_Concept_Full_INT_20140131_UUID.txt"};
		// check bos contains all our info
		assertTrue(sw.getBuffer().length() > 0);

		assertNotNull(response.getResult());
		System.out.println(response.getResult());
		assertEquals("There are 3 file names that are not valid plus 4 other errors", 7, response.getNumErrors());
	}

	@Test
	public void testExecuteWithManifest() throws Exception {
		String fileName = "/SnomedCT_Release_INT_20140831.zip";
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile(fileName));
		ManifestFile manifestFile = new ManifestFile(getFile("/manifest_20250731.xml"));

		TestReportable response = validationRunner.execute(provider, new TestWriterDelegate(new StringWriter()), true, manifestFile);

		assertNotNull(response.getResult());
		assertEquals("should only be manifest errors in this", 54, response.getNumErrors());
	}

	private File getFile(String testFileName) throws URISyntaxException {
		URL zipUrl = StructuralTestRunner.class.getResource(testFileName);
		return new File(zipUrl.toURI());
	}

}
