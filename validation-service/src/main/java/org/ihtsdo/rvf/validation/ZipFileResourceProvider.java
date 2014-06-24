package org.ihtsdo.rvf.validation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class ZipFileResourceProvider implements ResourceManager {


    public ZipFileResourceProvider(File file) throws IOException {
        this.zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory()) {
                String key = new File(zipEntry.getName()).getName();
                filenames.put(key, zipEntry);
            }
        }
    }

    public BufferedReader getReader(String name, Charset charset) throws IOException {
        ZipEntry entry = getEntry(name);
        return new BufferedReader(new InputStreamReader(
                zipFile.getInputStream(entry), charset));
    }

    public boolean isFile(String name) {
        return filenames.get(name) != null;
    }

    @Override
    public String getFilePath() {
        return zipFile.getName();
    }

    public ZipEntry getEntry(String name) {
        return filenames.get(name);
    }

    private final ZipFile zipFile;
    private Map<String, ZipEntry> filenames = new HashMap<>();
}
