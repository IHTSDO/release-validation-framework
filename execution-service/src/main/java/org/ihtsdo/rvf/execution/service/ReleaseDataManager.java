package org.ihtsdo.rvf.execution.service;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

/**
 * Utility service that manages a repository of published SNOMED CT releases. Note that we have deliberately not
 * included a delete/update functionality in this service. This get functionality should never be exposed to the external world
 * without security, because it would allow anonymous users to download SNOMED CT data.
 */
public interface ReleaseDataManager {

    boolean uploadPublishedReleaseData(InputStream inputStream, String fileName, String version, boolean isAppend);

    boolean uploadPublishedReleaseData(File releasePackZip, String version, boolean isAppend);

    String loadSnomedData(String versionName, boolean isAppend, File ... zipDataFile);

    boolean isKnownRelease(String releaseVersion);

    Set<String> getAllKnownReleases();

    String getSchemaForRelease(String releaseVersion);

    void setSchemaForRelease(String releaseVersion, String schemaName);

	File getZipFileForKnownRelease(String knownVersion);
}
