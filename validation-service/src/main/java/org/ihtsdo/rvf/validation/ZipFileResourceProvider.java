package org.ihtsdo.rvf.validation;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 */
public class ZipFileResourceProvider implements ResourceManager {

    public ZipFileResourceProvider(File file) throws IOException {
        this.zipFile = new ZipFile(file);
        this.filePath = zipFile.getName();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            assignFileNames(zipEntry);
        }
    }

    public BufferedReader getReader(String name, Charset charset) throws IOException {
        ZipEntry entry = getEntry(name);
        return new BufferedReader(new InputStreamReader(
                zipFile.getInputStream(entry), charset));
    }

    private void assignFileNames(ZipEntry zipEntry) {
        if (!zipEntry.isDirectory()) {
            String key = new File(zipEntry.getName()).getName();
            if (key.endsWith(".txt") && (!(key.toLowerCase().contains("Readme".toLowerCase()) || key.startsWith(".")))) {
                filenames.put(key, zipEntry);
            }
        }
    }

    public boolean isFile(String name) {
        return filenames.get(name) != null;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public List<String> getFileNames() {
        return new ArrayList<>(filenames.keySet());
    }

    public ZipEntry getEntry(String name) {
        return filenames.get(name);
    }

    private ZipFile zipFile;
    private String filePath;
    private Map<String, ZipEntry> filenames = new LinkedHashMap<>();
}
