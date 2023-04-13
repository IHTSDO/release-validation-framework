package au.csiro.datachecks.framework.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper class for unzipping archive files providing two main methods -
 * <ul>
 * <li>{@link #unzipArchive(File, File)} that unzips all the files in a
 * specified zip file to a specified directory
 * <li>{@link #unzipArchive(File, File, Collection)} that does as the first, but
 * accepts a {@link Collection} of regular expression {@link String}s which
 * entries in the zip file must match to be extracted
 * <ul>
 */
public class ZipFileHelper {
    private static final int BUFFER = 2048;
    private static final Logger log = Logger.getLogger(ZipFileHelper.class.getName());

    /**
     * Unzips the specified zipfileToExtract to the specified unzipLocation. All
     * entries in the zip are extracted.
     * 
     * @param zipfileToExtract
     * @param unzipLocation
     */
    public static void unzipArchive(File zipfileToExtract, File unzipLocation) {
        unzipArchive(zipfileToExtract, unzipLocation, new ArrayList<String>());
    }

    /**
     * Unzips entries whose names match one of the patternsToUnzip from the
     * specified zipfileToExtract to the specified unzipLocation.
     * 
     * @param zipfileToExtract
     * @param unzipLocation
     * @param patternsToUnzip
     *            {@link Collection} of {@link String} regular expressions used
     *            to determine which entries to extract from the zip - if empty
     *            or null all files are extracted
     */
    public static void unzipArchive(File zipfileToExtract, File unzipLocation, Collection<String> patternsToUnzip) {
        String extractPatternMessage;
        if ((patternsToUnzip == null) || (patternsToUnzip.isEmpty()))
            extractPatternMessage = "all files from ";
        else {
            extractPatternMessage = "files matching " + patternsToUnzip + " from ";
        }
        log.info("unzipping " + extractPatternMessage + zipfileToExtract + " to " + unzipLocation);

        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(zipfileToExtract);
        } catch (IOException e) {
            throw new RuntimeException("Cannot extract from zipfile '" + zipfileToExtract + "'", e);
        }

        Enumeration<? extends ZipEntry> entries = zipfile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (matchesExtractPattern(entry.getName(), patternsToUnzip)) {
                try {
                    extractEntry(unzipLocation, entry, zipfile);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "Cannot extract '" + entry + "' from zipfile '" + zipfileToExtract + "'", e);
                }
            }

        }

        log.info("finished unzipping " + extractPatternMessage + zipfileToExtract + " to " + unzipLocation);
    }

    private static void extractEntry(File unzipLocation, ZipEntry entry, ZipFile zipfile) throws IOException {
        File entryExtractTarget = new File(unzipLocation, entry.getName());
        if (entry.isDirectory()) {
            log.fine("Extracting directory: " + entry + " to " + entryExtractTarget);
            entryExtractTarget.mkdirs();
        } else {
            log.fine("Extracting file: " + entry + " to " + entryExtractTarget);

            if (!entryExtractTarget.getParentFile().exists()) {
                log.info(entryExtractTarget.getParent() + " does not exist - creating");
                entryExtractTarget.getParentFile().mkdirs();
            }

            BufferedInputStream is = new BufferedInputStream(zipfile.getInputStream(entry));
            Throwable localThrowable3 = null;
            try {
                BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(entryExtractTarget), BUFFER);

                Throwable localThrowable4 = null;
                try {
                    byte[] data = new byte[BUFFER];
                    int count;
                    while ((count = is.read(data, 0, BUFFER)) != -1)
                        dest.write(data, 0, count);
                } catch (Throwable localThrowable1) {
                    localThrowable4 = localThrowable1;
                    throw localThrowable1;
                } finally {
                    if (dest != null)
                        if (localThrowable4 != null)
                            try {
                                dest.close();
                            } catch (Throwable x2) {
                                localThrowable4.addSuppressed(x2);
                            }
                        else
                            dest.close();
                }
            } catch (Throwable localThrowable2) {
                localThrowable3 = localThrowable2;
                throw localThrowable2;
            } finally {
                if (is != null)
                    if (localThrowable3 != null)
                        try {
                            is.close();
                        } catch (Throwable x2) {
                            localThrowable3.addSuppressed(x2);
                        }
                    else
                        is.close();
            }
        }
    }

    private static boolean matchesExtractPattern(String name, Collection<String> patternsToUnzip) {
        if ((patternsToUnzip == null) || (patternsToUnzip.isEmpty())) {
            return true;
        }

        for (String pattern : patternsToUnzip) {
            if (name.matches(pattern)) {
                return true;
            }
        }

        return false;
    }
}
