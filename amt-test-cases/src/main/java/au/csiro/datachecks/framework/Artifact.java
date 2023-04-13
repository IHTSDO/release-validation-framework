package au.csiro.datachecks.framework;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import au.csiro.datachecks.framework.helpers.DownloadHelper;
import au.csiro.datachecks.framework.helpers.ZipFileHelper;

/**
 * Class used to store and pass the information of a datafile used by the datacheck
 * framework. This class represents
 * <ul>
 * <li>the {@link URL} of where the source file is</li>
 * <li>the {@link File} destination of where to put the file</li>
 * <li>a boolean unzipToDestination which indicates if the file is to unzipped
 * after download</li>
 * <li>a {@link List} of {@link String} objects that represent regular
 * expressions matching entries to extract</li>
 * <li>username and password to use for simple authentication to download the
 * files from the specified {@link URL}</li>
 * <li>the {@link URL} of where the source file is</li>
 * </ul>
 * As well as being a data holder of these attributes used to pass this
 * information from Ant or Maven, this class also provides the
 * {@link #getAndExtractFile()} method to act on this information to actually
 * get, and if {@link #unzipToDestination} is set to true extract the file to
 * the {@link #destination} specified.
 * <p>
 * Additionally this class maintains a static collection of files it has already
 * processed, so if these files have already been downloaded and extracted to
 * the specified location, this will not be done again.
 */
public class Artifact implements Comparable<Artifact> {
    private static final Logger log = Logger.getLogger(Artifact.class.getName());

    private static Set<Artifact> alreadyProcessed = new TreeSet<>();
    private URL url;
    private File destination;
    private boolean unzipToDestination;
    private List<String> patternsToUnzip = new ArrayList<>();
    private String username;
    private String password;

    /**
     * Gets the file represented by this object and extracts it (if configured)
     * to its destination location.
     */
    public void getAndExtractFile() {
        if (alreadyProcessed.contains(this)) {
            log.info("URL " + this.url + " already downloaded to destination " + this.destination
                    + ". Nothing to do, skipping");
        } else {
            File downloadLocation = null;

            if (this.unzipToDestination)
                try {
                    downloadLocation = File
                            .createTempFile("datachecks-file-download-" + this.destination.getName(), "");
                    downloadLocation.deleteOnExit();
                } catch (IOException e) {
                    throw new RuntimeException("Failed creating temp file to download '" + this.url + "'", e);
                }
            else {
                downloadLocation = this.destination;
            }

            this.url = reformatUrlSpaces(this.url);
            DownloadHelper.downloadFileWithCache(this.url, downloadLocation, this.username, this.password);

            if (this.unzipToDestination) {
                ZipFileHelper.unzipArchive(downloadLocation, this.destination, this.patternsToUnzip);
            }
            alreadyProcessed.add(this);
        }
    }

    private URL reformatUrlSpaces(URL url) {
        if (url.toExternalForm().contains(" ")) {
            try {
                return new URL(url.toExternalForm().replaceAll(" ", "%20"));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Error reformatting spaces in URL " + url);
            }
        }
        return url;
    }

    /**
     * @return location the URL is to be downloaded or extracted to
     */
    public File getDestination() {
        return this.destination;
    }

    /**
     * Sets the location the URL is to be downloaded or extracted to
     * 
     * @param destination
     */
    public void setDestination(File destination) {
        this.destination = destination;
    }

    /**
     * @return the {@link URL} to the source of the file
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * Sets the {@link URL} to the source of the file
     * 
     * @param url
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * @return the username to authenticate with when downloading from the
     *         file's source URL
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the username to authenticate with when downloading from the file's
     * source URL
     * 
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password authenticate with when downloading from the file's
     *         source URL
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the password authenticate with when downloading from the file's
     * source URL
     * 
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return true if the file is to be unzipped to the destination location
     *         after downloading, false otherwise
     */
    public boolean isUnzipToDestination() {
        return this.unzipToDestination;
    }

    /**
     * Sets whether the file is to be unzipped to the {@link #destination} or
     * simply copied there
     * 
     * @param unzipToDestination
     */
    public void setUnzipToDestination(boolean unzipToDestination) {
        this.unzipToDestination = unzipToDestination;
    }

    /**
     * @return {@link List} of {@link String} regular expressions used to filter
     *         the contents of a zip file when extracting it if
     *         {@link #isUnzipToDestination()} is true - if a zip file entry
     *         matches any of the regular expressions it is extracted to the
     *         {@link #destination}, if the {@link List} or regular expressions
     *         is null or empty, all zip entries will be extracted
     */
    public List<String> getPatternsToUnzip() {
        return this.patternsToUnzip;
    }

    /**
     * Sets the {@link List} of {@link String} regular expressions used to
     * filter the contents of a zip file when extracting it if
     * {@link #isUnzipToDestination()} is true - if a zip file entry matches any
     * of the regular expressions it is extracted to the {@link #destination},
     * if the {@link List} or regular expressions is null or empty, all zip
     * entries will be extracted
     * 
     * @param patternsToUnzip
     */
    public void setPatternsToUnzip(List<String> patternsToUnzip) {
        this.patternsToUnzip = patternsToUnzip;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Artifact o) {
        Collections.sort(this.patternsToUnzip);
        Collections.sort(o.patternsToUnzip);
        return new CompareToBuilder().append(this.url.toExternalForm(), o.url.toExternalForm())
                .append(this.destination, o.destination).append(this.unzipToDestination, this.unzipToDestination)
                .append(this.patternsToUnzip.toString(), o.patternsToUnzip.toString()).toComparison();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, new String[0]);
    }
}
