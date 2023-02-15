package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.listing.Listing;
import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ManifestFileTest {

	@Test
	public void testCreateListing() throws Exception {
		// load the manifest file
		URL url = getClass().getResource("/manifest_20250731.xml");
		assertNotNull(url);
		File f = new File(url.toURI());
		ManifestFile provider = new ManifestFile(f);
		Listing l = provider.getListing();
		assertEquals(1, l.getFolders().size());
	}
}
