package org.ihtsdo.rvf.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.rvf.core.service.util.ZipFileUtils;
import org.junit.Test;

public class ZipFileUtilsTest {

	@Test
	public void testExtractZipFile() throws Exception {
		final File tempDirectory = FileUtils.getTempDirectory();
		final File unzippedFolder  = new File(tempDirectory,"testData");
		try {
			if (unzippedFolder.exists()) {
				unzippedFolder.delete();
			}
			unzippedFolder.mkdir();
			final File zipFile = new File(getClass().getResource("/SnomedCT_Release_INT_20140131.zip").toURI());
			ZipFileUtils.extractFilesFromZipToOneFolder(zipFile, unzippedFolder.getAbsolutePath());
			final File[] filesUnzipped = unzippedFolder.listFiles();
			assertEquals(filesUnzipped.length, 3);
			for( final File file : filesUnzipped) {
				assertTrue(file.isFile());
			}
		} finally {
			FileUtils.deleteQuietly(unzippedFolder);
		}
	}
}
