package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.model.Listing;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ManifestFileTest {

	@Test
	public void testCreateListing() throws Exception {
		// load the manifest file
		File f = new File(getClass().getResource("/manifest_20250731.xml").toURI());
		ManifestFile provider = new ManifestFile(f);
		Listing l = provider.getListing();
		assertEquals(1, l.getFolders().size());
	}

}
