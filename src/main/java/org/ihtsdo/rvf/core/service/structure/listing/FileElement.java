package org.ihtsdo.rvf.core.service.structure.listing;

public class FileElement {

	private String fileName;
	private Folder folder;

	public FileElement() {
	}

	public FileElement(String name) {
		this.fileName = name;
	}

	public String getFileName() {
		if (folder != null) {
			return folder.getFolderName() + Folder.SEPARATOR + fileName;
		}
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getName() {
		return this.fileName;
	}

	@Override
	public String toString() {
		return "FileElement{" +
				"fileName='" + fileName + '\'' +
				'}';
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

}
