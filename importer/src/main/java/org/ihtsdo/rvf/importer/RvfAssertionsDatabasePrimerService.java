package org.ihtsdo.rvf.importer;


import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RvfAssertionsDatabasePrimerService {
	@Autowired
	private AssertionsDatabaseImporter dbImporter;
	@Autowired
	private AssertionGroupImporter assertionGroupImporter;
	private static final String scriptsDir = "/scripts";

	private static final Logger LOGGER = LoggerFactory.getLogger(RvfAssertionsDatabasePrimerService.class);

	@PostConstruct
	public void importAssertionsAndGroups() {
		if (dbImporter.isAssertionImportRequired()) {
			LOGGER.info("Import requested or no assertions exist; start importing...");
			InputStream manifestInputStream = AssertionsDatabaseImporter.class.getResourceAsStream("/xml/lists/manifest.xml");
			// import content
			dbImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);

			LOGGER.info("Assertions imported");
		}
		//create assertion group
		assertionGroupImporter.importAssertionGroups();
	}
}
