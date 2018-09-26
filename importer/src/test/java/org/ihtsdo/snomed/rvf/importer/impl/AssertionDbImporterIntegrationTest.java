package org.ihtsdo.snomed.rvf.importer.impl;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.snomed.rvf.importer.AssertionsDatabaseImporter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * A test class for {@link org.ihtsdo.snomed.rvf.importer.AssertionsImporter}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testImporterServiceContext.xml"})
@Ignore
public class AssertionDbImporterIntegrationTest {
	
	public static final String scriptsDir = "/scripts";
	public static final String hierarchyTermModellingDir = "hierarchy-term-modelling";

	@Autowired
	AssertionsDatabaseImporter assertionsImporter;

	@Before
	public void setUp() throws Exception {
		assertNotNull("Assertions Importer must not be null", assertionsImporter);
	}

	@Test
	public void testImportAssertionsFromFile() throws Exception {
		//"/xml/lists/manifest.xml"
		//"/testManifest.xml"
		final InputStream manifestInputStream = AssertionsImporterImplIntegrationTestManual.class.getResourceAsStream("/xml/lists/manifest.xml");
		assertNotNull("manifestUrl must not be null", manifestInputStream);

		// import content
		assertionsImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);
	}
	
	@Test
	public void testImportAssertionsFromDirectory() throws Exception {
		String targetDirName = scriptsDir + "/" + hierarchyTermModellingDir;
		final URL targetDirUrl = AssertionsImporterImplIntegrationTestManual.class.getResource(targetDirName);
		assertNotNull(targetDirName + " directory not found", targetDirUrl);
		assertionsImporter.importAssertionsFromDirectory(new File(targetDirUrl.toURI()), hierarchyTermModellingDir);
	}
	
	@Test
	public void testAddSqlScript() throws IOException {
		assertionsImporter.addSqlTestToAssertion(new Assertion(), IOUtils.toString(AssertionsImporterImplIntegrationTestManual.class.getResource("/scripts/release-type/release-type-full-validation-concept.sql")));
	}
}