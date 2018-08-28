package org.ihtsdo.rvf.validation.model;

import java.util.ArrayList;
import java.util.List;

public class Folder {

	public static final String SEPARATOR = "/";

	private String folderName;
	private List<Folder> folders = new ArrayList<>();
	private List<FileElement> files = new ArrayList<>();
	private Folder parent;
	private Listing listing;

	public Folder() {
	}

	public Folder(String name, List<Folder> folders, List<FileElement> files) {

		this.folderName = name;
		this.folders = folders;
		this.files = files;
	}

	public String getFolderName() {
		if (parent != null) {
			return parent.getFolderName() + SEPARATOR + folderName;
		}
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public List<Folder> getFolders() {
		return folders;
	}

	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}

	public List<FileElement> getFiles() {
		return files;
	}

	public void setFiles(List<FileElement> files) {
		this.files = files;
	}

	public void addFolder(Folder folder) {
		folder.setParent(this);
		this.folders.add(folder);
	}

	public void addFile(FileElement fileElement) {
		fileElement.setFolder(this);
		this.files.add(fileElement);
	}

	@Override
	public String toString() {
		return "Folder{" +
				"folderName='" + folderName + '\'' +
				'}';
	}

	public Folder getParent() {
		return parent;
	}

	public void setParent(Folder parent) {
		this.parent = parent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Folder folder = (Folder) o;

		if (files != null ? !files.equals(folder.files) : folder.files != null) return false;
		if (!folderName.equals(folder.folderName)) return false;
		if (folders != null ? !folders.equals(folder.folders) : folder.folders != null) return false;
		return parent != null ? parent.equals(folder.parent) : folder.parent == null;
	}

	@Override
	public int hashCode() {
		int result = folderName.hashCode();
		result = 31 * result + (folders != null ? folders.hashCode() : 0);
		result = 31 * result + (files != null ? files.hashCode() : 0);
		result = 31 * result + (parent != null ? parent.hashCode() : 0);
		return result;
	}

	public Listing getListing() {
		return listing;
	}

	public void setListing(Listing listing) {
		this.listing = listing;
	}

}
