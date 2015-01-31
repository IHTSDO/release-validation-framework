package org.ihtsdo.snomed.rvf.importer.impl;

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.ihtsdo.snomed.rvf.importer.AssertionsImporter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * A test class for {@link org.ihtsdo.snomed.rvf.importer.impl.AssertionsImporterImpl}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/importerServiceContext.xml"})
public class AssertionsImporterImplIntegrationTest {

    @Autowired
    AssertionsImporter assertionsImporter;

    @Before
    public void setUp() throws Exception {
        assertNotNull("Assertions Importer must not be null", assertionsImporter);
    }

    @Test
    public void testImportAssertionsFromFile() throws Exception {
    	//"/xml/lists/manifest.xml"
    	//"/testManifest.xml"
        final URL manifestUrl = AssertionsImporterImplIntegrationTest.class.getResource("/xml/lists/manifest.xml");
        assertNotNull("manifestUrl must not be null", manifestUrl);
        final URL scriptsFolderUrl = AssertionsImporterImplIntegrationTest.class.getResource("/scripts");
        assertNotNull("scriptsFolderUrl must not be null", scriptsFolderUrl);

        // import content
        assertionsImporter.importAssertionsFromFile(manifestUrl.getPath(), scriptsFolderUrl.getPath());
    }
}