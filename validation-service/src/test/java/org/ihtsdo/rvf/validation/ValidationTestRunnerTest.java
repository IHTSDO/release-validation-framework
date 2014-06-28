package org.ihtsdo.rvf.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testValidationServiceContext.xml"})
public class ValidationTestRunnerTest {

    @Autowired
    private ValidationTestRunner validationRunner;

    @Test
    public void testExecute_DataInResponse() throws Exception {
        String testFileName = "/SnomedCT_Release_INT_20140831.zip";
        URL zipUrl = ValidationTestRunner.class.getResource(testFileName);
        File file = new File(zipUrl.toURI());
        ZipFileResourceProvider provider = new ZipFileResourceProvider(file);
        TestReport response = validationRunner.execute(ResponseType.CSV, provider);
        assertTrue(response.getResult() != null);
        assertEquals(3, response.getErrorCount());
    }

    @Test
    public void testExecute_ExdRefSet() throws Exception {
        String testFileName = "/der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt.zip";
        URL zipUrl = ValidationTestRunner.class.getResource(testFileName);
        File file = new File(zipUrl.toURI());
        ZipFileResourceProvider provider = new ZipFileResourceProvider(file);
        TestReport response = validationRunner.execute(ResponseType.CSV, provider);
        assertTrue(response.getResult() != null);
        assertEquals(0, response.getErrorCount());
    }
}