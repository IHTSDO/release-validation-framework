package org.ihtsdo.rvf.importer;

import org.ihtsdo.rvf.importer.RvfAssertionsDatabasePrimerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/importerServiceContext.xml"})
public class RvfAssertionsDatabasePrimerServiceIntegrationTestManual {
	
	@Autowired
	RvfAssertionsDatabasePrimerService primerService;
	@Test
	public void testPrimeRvfDb() {
		primerService.importAssertionsAndGroups();
	}

}
