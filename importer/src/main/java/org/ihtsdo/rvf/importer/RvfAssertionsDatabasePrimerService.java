package org.ihtsdo.rvf.importer;


import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

	@Autowired
	AssertionService assertionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(RvfAssertionsDatabasePrimerService.class);

	@PostConstruct
	public void importAssertionsAndGroups() throws IOException {
		InputStream manifestInputStream = assertionResourceManager.readResourceStream("manifest.xml");
		if (dbImporter.isAssertionImportRequired()) {
			LOGGER.info("No assertons exist and start importing...");

			// import content
			dbImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);

			LOGGER.info("Assertions imported");
		} else {
			// detect assertions to insert/update/delete

			List<Assertion> dbAssertions = assertionService.findAll();
			Set<UUID> dbAssertionUUIDs = dbAssertions.stream(). map(Assertion::getUuid).collect(Collectors.toSet());

			List<Assertion> fileAssertions = dbImporter.getAssertionsFromFile(manifestInputStream);
			Set<UUID> fileAssertionUUIDs = fileAssertions.stream(). map(Assertion::getUuid).collect(Collectors.toSet());

			// remove assertions
			List<UUID> toDeleteAssertions = dbAssertionUUIDs.stream().filter(uuid -> !fileAssertionUUIDs.contains(uuid)).collect(Collectors.toList());
			for (UUID uuid : toDeleteAssertions) {
				final Assertion assertion = assertionService.findAssertionByUUID(uuid);
				if (assertion != null) {
					List<AssertionGroup> groups = assertionService.getGroupsForAssertion(assertion);
					// remove assertion from group
					for (AssertionGroup group : groups) {
						assertionService.removeAssertionFromGroup(assertion, group);
					}
					assertionService.delete(assertion);
				}
			}

			// import content to insert/update assertions
			dbImporter.importAssertionsFromFile(manifestInputStream, scriptsDir);
			LOGGER.info("Assertions updated");
		}
		//create assertion group
		assertionGroupImporter.importAssertionGroups();
	}
}
