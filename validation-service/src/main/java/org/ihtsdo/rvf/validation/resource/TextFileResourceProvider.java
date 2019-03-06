package org.ihtsdo.rvf.validation.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TextFileResourceProvider implements ResourceProvider {

	private File file;
	private List<String> fileNames;

	public TextFileResourceProvider(File file, String fileName) {
		this.file = file;
		fileNames = new ArrayList<>();
		fileNames.add(fileName);
	}

	@Override
	public BufferedReader getReader(String name, Charset charset) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
	}

	@Override
	public String getFilePath() {
		return file.getAbsolutePath();
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
