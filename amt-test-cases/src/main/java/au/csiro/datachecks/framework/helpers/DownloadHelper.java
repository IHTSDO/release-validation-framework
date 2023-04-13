package au.csiro.datachecks.framework.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.io.Files;

/**
 * Helper class used to help download files to be tested. This class provides
 * four main methods which download files from a specified {@link URL} to a
 * specified {@link File} location. The 4 variations include the ability to
 * specify authentication and optionally use or not use a cache which will
 * remember {@link URL}s already downloaded and copy the previously downloaded
 * file to the {@link File} specified.
 */
public class DownloadHelper {
    private static final int BUFFER = 2048;
    private static final Logger log = Logger.getLogger(DownloadHelper.class.getName());

    private static Map<URL, File> urlsDownloaded = new HashMap<>();

    /**
     * Downloads the specified {@link URL} to the specified {@link File}
     * location using the cache. Use of the cache means that if this file has
     * been previously downloaded, the previously downloaded version will be
     * copied to the specified downloadLocation.
     * 
     * @param url
     *            to download
     * @param downloadLocation
     *            to put the downloaded file
     */
    public static synchronized void downloadFileWithCache(URL url, File downloadLocation) {
        downloadFileWithCache(url, downloadLocation, null, null);
    }

    /**
     * Downloads the specified {@link URL} to the specified {@link File}
     * location using the cache and using password authentication using the
     * username and password supplied. Use of the cache means that if this file
     * has been previously downloaded, the previously downloaded version will be
     * copied to the specified downloadLocation.
     * 
     * @param url
     *            to download
     * @param downloadLocation
     *            to put the downloaded file
     * @param username
     *            to authenticate with
     * @param password
     *            to authenticate with
     */
    public static synchronized void downloadFileWithCache(URL url, File downloadLocation, String username,
            String password) {
        if (urlsDownloaded.containsKey(url)) {
            File alreadyDownloadedCopy = (File) urlsDownloaded.get(url);
            log.info("already downloaded " + url + " will reuse downloaded file " + alreadyDownloadedCopy);
            try {
                Files.copy(alreadyDownloadedCopy, downloadLocation);
            } catch (IOException e) {
                throw new RuntimeException("Failed copying already downloaded copy " + alreadyDownloadedCopy + " of '"
                        + url + "' to " + downloadLocation, e);
            }
        } else {
            downloadFile(url, downloadLocation, username, password);
            urlsDownloaded.put(url, downloadLocation);
        }
    }

    /**
     * Downloads the specified {@link URL} to the specified {@link File}
     * location. No caching is used - if the {@link URL} was previously
     * downloaded it will be again.
     * 
     * @param url
     *            to download
     * @param downloadLocation
     *            to put the downloaded file
     */
    public static void downloadFile(URL url, File downloadLocation) {
        downloadFile(url, downloadLocation, null, null);
    }

    /**
     * Downloads the specified {@link URL} to the specified {@link File}
     * location using password authentication using the username and password
     * supplied. No caching is used - if the {@link URL} was previously
     * downloaded it will be again.
     * 
     * @param url
     *            to download
     * @param downloadLocation
     *            to put the downloaded file
     * @param username
     *            to authenticate with
     * @param password
     *            to authenticate with
     */
    public static void downloadFile(URL url, File downloadLocation, final String username, final String password) {
        log.info("downloading " + url + " to " + downloadLocation);

        downloadLocation.getParentFile().mkdirs();

        if (username != null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(downloadLocation), BUFFER);
                BufferedInputStream bis = new BufferedInputStream(url.openStream(), BUFFER)) {
            int j = 0;
            int i;
            while ((i = bis.read()) != -1) {
                bos.write(i);

                if (j++ % 100000000 == 0) {
                    log.fine("still reading from " + url + " to " + downloadLocation);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot download from " + url, e);
        }

        // if the username is not null then an authenticator has been set
        // so remove it before the next file is downloaded in case it has
        // no authenticator
        if (username != null) {
            Authenticator.setDefault(null);
        }

        log.info("finished downloading " + url + " to " + downloadLocation);
    }
}
