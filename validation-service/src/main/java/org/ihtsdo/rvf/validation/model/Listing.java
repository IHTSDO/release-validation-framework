package org.ihtsdo.rvf.validation.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Listing {

    protected List<Folder> folders = new ArrayList<>();
    protected List<String> paths = new ArrayList<>();

    public Listing() {
    }

    public Listing(List<Folder> folders) {
        this.folders = folders;
    }
    
    public List<Folder> getFolders() {
        return folders;
    }
    
    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }
    
    public void addFolder(Folder folder) {
        folder.setListing(this);
        folders.add(folder);
    }
    
    public void addPath(String value) {
        paths.add(value);    
    }
    
}
