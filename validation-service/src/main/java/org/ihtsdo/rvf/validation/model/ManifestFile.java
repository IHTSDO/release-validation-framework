package org.ihtsdo.rvf.validation.model;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ManifestFile {

	private  final Logger logger = LoggerFactory.getLogger(ManifestFile.class);

	private Listing listing;
	private File file;

	public ManifestFile(File manifestFile) {
		this.file = manifestFile;
		this.listing = createListing(manifestFile);
	}

	public Listing createListing(File f) {
		try {
			InputStream configurationStream = new FileInputStream(f);
			DigesterLoader digesterLoader = DigesterLoader.newLoader(new ListingBuilder());
			Digester digester = digesterLoader.newDigester();
			return digester.parse(configurationStream);
		} catch (IOException | SAXException e) {
			logger.error("Failed to parse manifest file {}", f.getAbsolutePath(), e);
		}
		return null;
	}


	public Listing getListing() {
		return listing;
	}

	public void setListing(Listing listing) {
		this.listing = listing;
	}

	public File getFile() {
		return file;
	}
}
