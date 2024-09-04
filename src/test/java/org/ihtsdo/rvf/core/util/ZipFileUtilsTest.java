package org.ihtsdo.rvf.core.util;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ZipFileUtilsTest {

	@Test
	public void testExtractZipFile() throws Exception {
		final File tempDirectory = FileUtils.getTempDirectory();
		final File unzippedFolder  = new File(tempDirectory,"testData");
		try {
			if (unzippedFolder.exists()) {
				assertTrue(unzippedFolder.delete());
			}
			assertTrue(unzippedFolder.mkdir());
			URL url = getClass().getResource("/SnomedCT_Release_INT_20140131.zip");
			assertNotNull(url);
			final File zipFile = new File(url.toURI());
			ZipFileUtils.extractFilesFromZipToOneFolder(zipFile, unzippedFolder.getAbsolutePath());
			final File[] filesUnzipped = unzippedFolder.listFiles();
			assertNotNull(filesUnzipped);
			assertEquals(3, filesUnzipped.length);
			for( final File file : filesUnzipped) {
				assertTrue(file.isFile());
			}
		} finally {
			FileUtils.deleteQuietly(unzippedFolder);
		}
	}
}
