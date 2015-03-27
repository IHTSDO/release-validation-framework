package org.ihtsdo.rvf.validation.resource;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TextFileResourceProvider implements ResourceProvider {

	private final String fileName;
	private File file;
	private List<String> fileNames;

	public TextFileResourceProvider(File file, String fileName) {
		this.file = file;
		this.fileName = fileName;
		fileNames = new ArrayList<>();
		fileNames.add(fileName);
	}

	@Override
	public BufferedReader getReader(String name, Charset charset) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
	}

	@Override
	public String getFilePath() {
		return fileName;
	}

	@Override
	public List<String> getFileNames() {
		return fileNames;
	}

	@Override
	public boolean match(String name) {
		return false;
	}

}
