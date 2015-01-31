package org.ihtsdo.snomed.rvf.importer;

/**
 * An interface specification for an service that importers {@link org.ihtsdo.rvf.entity.Assertion}s and
 * related {@link org.ihtsdo.rvf.entity.Test}s.
 */
public interface AssertionsImporter {

    void importAssertionsFromFile(String xmlFilePath, String sqlResourcesFolderLocation);
}
