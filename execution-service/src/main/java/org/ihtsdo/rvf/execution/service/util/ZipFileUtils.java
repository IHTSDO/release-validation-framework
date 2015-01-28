package org.ihtsdo.rvf.execution.service.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

public class ZipFileUtils {
	/**
	 * Utility method for extracting a zip file to a given folder and remove the prefix "x" from file name if present.
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException 
	 */
	public static void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
		try (ZipFile zipFile = new ZipFile(file)){
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory()) {
					InputStream in = null;
					OutputStream out = null;
					try {
							in = zipFile.getInputStream(entry);
							String fileName = Paths.get(entry.getName()).getFileName().toString();
							if (fileName.startsWith("x")) {
								fileName = fileName.substring(1);
							}
							final File entryDestination = new File(outputDir,fileName);
							out = new FileOutputStream(entryDestination);
							IOUtils.copy(in, out);
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
				}
			}
		} 
	}
	
	
	
	/**
	 * Utility method for extracting a zip file to a given folder
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException 
	 */
	public static void extractZipFile(final File file, final String outputDir) throws IOException {
		
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final File entryDestination = new File(outputDir,  entry.getName());
				entryDestination.getParentFile().mkdirs();
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					InputStream in = null;
					OutputStream out = null;
					try {
						in = zipFile.getInputStream(entry);
						out = new FileOutputStream(entryDestination);
						IOUtils.copy(in, out);
						} finally {
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(out);
						}
				}
			}
		}
	}
}
