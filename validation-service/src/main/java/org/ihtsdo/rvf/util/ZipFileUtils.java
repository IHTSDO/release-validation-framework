package org.ihtsdo.rvf.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipFileUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipFileUtils.class);

	/**
	 * Size of the buffer to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Utility method for extracting a zip file to a given folder and remove the prefix "x" from file name if present.
	 * @param file the zip file to be extracted
	 * @param outputDir the output folder to extract the zip to.
	 * @throws IOException 
	 */
	public static void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
		//debug for investigating encoding issue
		LOGGER.debug("deafult file.encoding:" + System.getProperty("file.encoding"));
		LOGGER.debug("default charset:" + Charset.defaultCharset());
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
							if (fileName.startsWith("x") && fileName.endsWith(".txt")) {
								fileName = fileName.substring(1);
							}
							File entryDestination = new File(outputDir,fileName);
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
				if (!entryDestination.toPath().normalize().startsWith(outputDir)) {
					LOGGER.error("Bad zip entry " + entry.getName());
					continue;
				}
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
	
	
	/**
	 * Zip it
	 * @param zipFile output ZIP file location
	 * @param sourceFileDir 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void zip(final String sourceFileDir, final String zipFile) throws IOException {
	 final byte[] buffer = new byte[1024];
	 try ( final FileOutputStream fos = new FileOutputStream(zipFile);
		 final ZipOutputStream zos = new ZipOutputStream(fos) ) {
		final List<File> fileList = new ArrayList<>();
		generateFileList(new File(sourceFileDir), fileList);
		for (final File file : fileList) {
			final ZipEntry ze= new ZipEntry(file.getName());
			zos.putNextEntry(ze);
			try (final FileInputStream in = new FileInputStream(file) ) {
				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
			}
		}
	 }
   }
 
	/**
	 * Traverse a directory and get all files,
	 * and add the file into fileList
	 * @param node file or directory
	 */
	private static void generateFileList(final File node, final List<File> fileList){
		//add file only
		if(node.isFile()){
			fileList.add(node);
		}
		if(node.isDirectory()){
			final String[] subNote = node.list();
			for(final String filename : subNote){
				generateFileList(new File(node, filename),fileList);
			}
		}
	}

	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	public static void unzip(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectory + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zipIn, filePath);
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	/**
	 * Extracts a zip entry (file entry)
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
}
