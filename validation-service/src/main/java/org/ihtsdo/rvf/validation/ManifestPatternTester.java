package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.model.FileElement;
import org.ihtsdo.rvf.validation.model.Folder;
import org.ihtsdo.rvf.validation.resource.ResourceManager;

import java.util.Date;
import java.util.List;

/**
 *
 */
public class ManifestPatternTester {

    private ValidationLog validationLog;
    private ResourceManager resourceManager;
    private ManifestFile manifestFile;
    private TestReportable report;

    private static final String MANIFEST_STRUCTURE_TEST = "ManifestPackageStructureTest";

    public ManifestPatternTester(ValidationLog validationLog, ResourceManager resourceManager, ManifestFile manifestFile,
                                 TestReportable report) {
        this.validationLog = validationLog;
        this.resourceManager = resourceManager;
        this.manifestFile = manifestFile;
        this.report = report;
    }

    public void runTests() {

        List<Folder> folders = manifestFile.getListing().getFolders();
        Date startTime = new Date();
        Integer rowNum = 0;
        Integer columnNum = 0;
        testStructure(folders, rowNum, columnNum, startTime);
        validationLog.info("{} files and {} folders tested in {} milliseconds.", columnNum, rowNum, (new Date().getTime() - startTime.getTime()));
    }

    private void testStructure(List<Folder> folders, Integer rowNum, Integer columnNum, Date startTime) {

        if (folders.isEmpty()) return;

        for (Folder folder : folders) {
            rowNum++;
            String name = folder.getFolderName();
            boolean match = resourceManager.match(name);
            if (!match) {
                validationLog.assertionError("Invalid package structure expected directory at {} but found none", name);
                report.addError(rowNum + "-" + columnNum, startTime, name, name, name, MANIFEST_STRUCTURE_TEST, "", "No Folder Found", name);
            } else {
                report.addSuccess(rowNum + "-" + columnNum, startTime, name, name, "", MANIFEST_STRUCTURE_TEST, "");
            }
            List<FileElement> files = folder.getFiles();
            for (FileElement file : files) {
                columnNum++;
                String filename = file.getFileName();

                if (!(resourceManager.match(filename))) {
                    validationLog.assertionError("Invalid package structure expected file at {} but found none", filename);
                    report.addError(rowNum + "-" + columnNum, startTime, filename, filename, filename, MANIFEST_STRUCTURE_TEST, "", "No File Found", filename);
                } else {
                    report.addSuccess(rowNum + "-" + columnNum, startTime, filename, filename, "", MANIFEST_STRUCTURE_TEST, "");
                }
            }
            List<Folder> children = folder.getFolders();
            if (!children.isEmpty()) {
                testStructure(children, rowNum, columnNum, startTime);
            }
        }
    }
}
