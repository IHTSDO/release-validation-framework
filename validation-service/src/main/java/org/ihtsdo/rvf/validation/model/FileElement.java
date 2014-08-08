package org.ihtsdo.rvf.validation.model;

import java.io.File;

/**
 *
 */
public class FileElement {
    private String fileName;
    private Folder folder;

    public FileElement() {
    }

    public FileElement(String name) {
        this.fileName = name;
    }

    public String getFileName() {
        if(folder != null) {
            return folder.getFolderName() + Folder.SEPARATOR + fileName;
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "FileElement{" +
                "fileName='" + fileName + '\'' +
                '}';
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }
}
