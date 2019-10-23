package org.ihtsdo.rvf.validation;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.model.FileElement;
import org.ihtsdo.rvf.validation.model.Folder;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RF2FilesReleaseTypeTester {

    private static final String FILES_ORG_TEST = "RF2FilesReleaseTypeTester";
    private final ValidationLog validationLog;
    private final ResourceProvider resourceManager;
    private final ManifestFile manifestFile;
    private final TestReportable report;
    private static final String DELTA = "Delta";
    private static final String SNAPSHOT = "Snapshot";
    private static final String FULL = "Full";
    private static final String[] RELEASE_FILE_TYPES = {DELTA, SNAPSHOT, FULL};

    public RF2FilesReleaseTypeTester(ValidationLog validationLog, ResourceProvider resourceManager, ManifestFile manifestFile, TestReportable report) {
        this.validationLog = validationLog;
        this.resourceManager = resourceManager;
        this.manifestFile = manifestFile;
        this.report = report;
    }

    public void runTest() {
        Date startDate = new Date();
        List<Folder> folderList = manifestFile.getListing().getFolders();
        List<String> foldersToCheck = Arrays.asList(RELEASE_FILE_TYPES);
        Folder rootFolder = folderList.get(0);
        List<Folder> releaseTypeFolders = new ArrayList<>();
        getReleaseTypeFolders(rootFolder, foldersToCheck, releaseTypeFolders);
        for (Folder releaseTypeFolder : releaseTypeFolders) {
            String releaseType = releaseTypeFolder.getName();
            List<String> filesInFolder = new ArrayList<>();
            getFileName(releaseTypeFolder, filesInFolder);
            List<String> invalidFileNames = verifyFileNames(releaseType, filesInFolder);
            if(!invalidFileNames.isEmpty()) {
                String expected = "All files under folder " + releaseType + " should be " + releaseType + " files";
                report.addError("", startDate, "manifest.xml", "",""
                        , FILES_ORG_TEST, "*" + releaseType + "*", invalidFileNames.toString(), expected, 0L);
            }
        }
    }

    private List<String> verifyFileNames(String currentReleaseType,  List<String> fileList) {
        List<String> invalidFileNames = new ArrayList<>();
        for (String fileName : fileList) {
            if (!fileName.contains(currentReleaseType)) {
                invalidFileNames.add(fileName);
            }
        }
        return invalidFileNames;
    }

    private void getFileName(Folder folder, List<String> fileNames) {
        List<Folder> subFolders = folder.getFolders();
        for (Folder subFolder : subFolders) {
            if (subFolder.getFolders() != null && !subFolder.getFolders().isEmpty()) {
                getFileName(subFolder, fileNames);
            }
            if (subFolder.getFiles() != null && !subFolder.getFiles().isEmpty()) {
                for (FileElement fileElement : subFolder.getFiles()) {
                    fileNames.add(fileElement.getName());
                }
            }
        }
    }

    private void getReleaseTypeFolders(Folder folder, List<String> releaseTypes, List<Folder> releaseTypeFolders) {
        if (releaseTypes.contains(folder.getName())) {
            releaseTypeFolders.add(folder);
        } else {
            for (Folder subFolder : folder.getFolders()) {
                if (subFolder.getFolders() != null && !subFolder.getFolders().isEmpty()) {
                    getReleaseTypeFolders(subFolder, releaseTypes, releaseTypeFolders);
                }
            }
        }

    }






}
