package org.ihtsdo.rvf.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.validation.TestReportable;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceManager;
import org.ihtsdo.rvf.validation.resource.ZipFileResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public class ZipFileUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipFileUtils.class);
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
	
	public static void copyUploadToDisk(final MultipartFile file, final File tempFile) throws IOException {
		LOGGER.debug ("Start copy of {} to {}", file.getOriginalFilename(), tempFile.getName());
		//Would it be quicker to do this as a move ie using TransferTo ?
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			try (InputStream inputStream = file.getInputStream()){
				IOUtils.copy(inputStream, out);
			}
		}
		LOGGER.debug ("Finished copy of {} to {}", file.getOriginalFilename(), tempFile.getName());	
	}

}
