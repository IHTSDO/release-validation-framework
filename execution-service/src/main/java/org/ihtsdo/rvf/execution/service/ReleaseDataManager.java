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

    boolean uploadPublishedReleaseData(InputStream inputStream, String fileName,
                                       boolean overWriteExisting, boolean purgeExistingDatabase);

    boolean uploadPublishedReleaseData(File releasePackZip, boolean overWriteExisting, boolean purgeExistingDatabase);

    String loadSnomedData(String versionName, boolean purgeExisting, File zipDataFile);

    boolean isKnownRelease(String releaseVersion);

    Set<String> getAllKnownReleases();

    String getSchemaForRelease(String releaseVersion);

    void setSchemaForRelease(String releaseVersion, String schemaName);
}
