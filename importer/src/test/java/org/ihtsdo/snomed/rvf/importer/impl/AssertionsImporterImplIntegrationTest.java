package org.ihtsdo.snomed.rvf.importer.impl;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.snomed.rvf.importer.AssertionsImporter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * A test class for {@link org.ihtsdo.snomed.rvf.importer.AssertionsImporter}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/importerServiceContext.xml"})
public class AssertionsImporterImplIntegrationTest {
	
	public static final String scriptsDir = "/scripts";
	public static final String hierarchyTermModellingDir = "hierarchy-term-modelling";

	private AssertionsImporter assertionsImporter;

	@Before
	public void setUp() throws Exception {
		assertionsImporter = new AssertionsImporter();
		assertNotNull("Assertions Importer must not be null", assertionsImporter);
	}

	@Test
	public void testImportAssertionsFromFile() throws Exception {
		//"/xml/lists/manifest.xml"
		//"/testManifest.xml"
		final URL manifestUrl = AssertionsImporterImplIntegrationTest.class.getResource("/xml/lists/manifest.xml");
		assertNotNull("manifestUrl must not be null", manifestUrl);
		final URL scriptsFolderUrl = AssertionsImporterImplIntegrationTest.class.getResource(scriptsDir);
		assertNotNull("scriptsFolderUrl must not be null", scriptsFolderUrl);

		// import content
		assertionsImporter.importAssertionsFromFile(manifestUrl.getPath(), scriptsFolderUrl.getPath());
	}
	
	@Test
	public void testImportAssertionsFromDirectory() throws Exception {
		String targetDirName = scriptsDir + "/" + hierarchyTermModellingDir;
		final URL targetDirUrl = AssertionsImporterImplIntegrationTest.class.getResource(targetDirName);
		assertNotNull(targetDirName + " directory not found", targetDirUrl);
		assertionsImporter.importAssertionsFromDirectory(new File(targetDirUrl.toURI()), hierarchyTermModellingDir);
	}
	
	@Test
	@Ignore
	public void testAddSqlScript() throws IOException {
		final AssertionsImporter importer = new AssertionsImporter();
		importer.addSqlTestToAssertion(new Assertion(), IOUtils.toString(AssertionsImporterImplIntegrationTest.class.getResource("/scripts/release-type/release-type-full-validation-concept.sql")));
	}
}