package org.ihtsdo.rvf.importer;


import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "rvf.import.assertions.on-startup", havingValue = "true")
public class RvfAssertionsDatabasePrimerService {
	@Autowired
	private AssertionsDatabaseImporter dbImporter;
	@Autowired
	private AssertionGroupImporter assertionGroupImporter;
	private static final String scriptsDir = "/scripts";

	@Autowired
	@Qualifier("assertionResourceManager")
	private ResourceManager assertionResourceManager;

	@Autowired
	AssertionService assertionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(RvfAssertionsDatabasePrimerService.class);

	@PostConstruct
	public void importAssertionsAndGroups() throws IOException {
		try (InputStream manifestInputStream = assertionResourceManager.readResourceStream("manifest.xml")) {
			if (dbImporter.isAssertionImportRequired()) {
				LOGGER.info("No assertions exist and start importing...");
				// import content
				dbImporter.importAssertionsFromManifest(manifestInputStream, scriptsDir);
				LOGGER.info("Assertions imported");
				// Create assertion group
				assertionGroupImporter.importAssertionGroups();
			} else {
				LOGGER.info("Assertions and assertion groups exist already.");
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("Failed to import assertions and assertion groups due to no manifest.xml found", e);
			throw new IOException("No manifest.xml file found in the assertions directory.", e);
		}

	}
}
