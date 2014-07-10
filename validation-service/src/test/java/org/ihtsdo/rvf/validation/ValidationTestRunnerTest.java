package org.ihtsdo.rvf.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/validationContext.xml"})
public class ValidationTestRunnerTest {

    @Autowired
    private ValidationTestRunner validationRunner;

    @Test
    public void testExecute_DataInResponse() throws Exception {
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile("/SnomedCT_Release_INT_20140831.zip"));

		TestReport response = validationRunner.execute(ResponseType.CSV, provider);

        assertTrue(response.getResult() != null);
        assertEquals(0, response.getErrorCount());
    }

	@Test
	public void testExecute_ExdRefSet() throws Exception {
		ZipFileResourceProvider provider = new ZipFileResourceProvider(getFile("/der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt.zip"));

		TestReport response = validationRunner.execute(ResponseType.CSV, provider);

		assertTrue(response.getResult() != null);
		assertEquals(0, response.getErrorCount());
	}

	@Test
	public void testExecute_rel2SimpleRefset() throws Exception {
		String fileName = "rel2_Refset_SimpleDelta_INT_20140131.txt";
		TextFileResourceProvider provider = new TextFileResourceProvider(getFile("/" + fileName), fileName);

		TestReport response = validationRunner.execute(ResponseType.CSV, provider);

		assertTrue(response.getResult() != null);
		assertEquals(0, response.getErrorCount());
	}

	private File getFile(String testFileName) throws URISyntaxException {
		URL zipUrl = ValidationTestRunner.class.getResource(testFileName);
		return new File(zipUrl.toURI());
	}

}
