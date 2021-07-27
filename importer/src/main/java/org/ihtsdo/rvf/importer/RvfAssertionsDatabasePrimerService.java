package org.ihtsdo.rvf.importer;


import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RvfAssertionsDatabasePrimerService {
	@Autowired
	private AssertionsDatabaseImporter dbImporter;
	@Autowired
	private AssertionGroupImporter assertionGroupImporter;
	private static final String scriptsDir = "/scripts";

	@Autowired
	@Qualifier("assertionResourceManager")
	private ResourceManager assertionResourceManager;

	@Value("${rvf.assertion.externalConfig}")
	private boolean useExternalAssertionConfigs;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RvfAssertionsDatabasePrimerService.class);

	@PostConstruct
	public void importAssertionsAndGroups() throws IOException {
		if (dbImporter.isAssertionImportRequired()) {
			LOGGER.info("No assertons exist and start importing...");
			InputStream manifestInputStream = null;
			if (useExternalAssertionConfigs) {
				manifestInputStream = assertionResourceManager.readResourceStream("manifest.xml");
			} else {
				manifestInputStream = AssertionsDatabaseImporter.class.getResourceAsStream("/xml/lists/manifest.xml");
			}

			// import content
			dbImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);

			LOGGER.info("Assertions imported");
		}
		//create assertion group
		assertionGroupImporter.importAssertionGroups();
	}
}
