package org.ihtsdo.rvf.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 *
 */
public interface ResourceManager {

    BufferedReader getReader(String name, Charset charset) throws IOException;

    String getFilePath();

    List<String> getFileNames();
}
