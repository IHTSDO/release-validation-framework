package org.ihtsdo.rvf.validation;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.ihtsdo.rvf.validation.model.Listing;
import org.ihtsdo.rvf.validation.model.ListingBuilder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ManifestFile {
    
    private Listing listing;
    private File manifestFile;

    public ManifestFile(File manifestFile) {
        this.manifestFile = manifestFile;
        this.listing = createListing(manifestFile);
    }

    public Listing createListing(File f) {
        try {
            InputStream configurationStream = new FileInputStream(f);
            DigesterLoader digesterLoader = DigesterLoader.newLoader(new ListingBuilder());
            Digester digester = digesterLoader.newDigester();
            return digester.parse(configurationStream);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }
}
