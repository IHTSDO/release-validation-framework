package org.ihtsdo.rvf.core.service.structure.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public interface ResourceProvider {

	BufferedReader getReader(String name, Charset charset) throws IOException;

	String getFilePath();

	List<String> getFileNames();

	boolean match(String name);

}
