package org.ihtsdo.snomed.rvf.importer;


import java.io.InputStream;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RvfAssertionsDatabasePrimerService {
	@Autowired
	private AssertionsDatabaseImporter dbImporter;
	@Autowired
	private AssertionGroupImporter assertionGroupImporter;
	private static final String scriptsDir = "/scripts";
	public void importAssertionsAndGroups() {
		if (assertionGroupImporter.isImportRequired()) {
			final URL manifestUrl = AssertionsImporter.class.getResource("/xml/lists/manifest.xml");
			InputStream manifestInputStream = AssertionsImporter.class.getResourceAsStream("/xml/lists/manifest.xml");
			// import content
			dbImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);
			//create assertion group
			assertionGroupImporter.importAssertionGroups();
		}
	}

}
