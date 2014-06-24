package org.ihtsdo.rvf.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 *
 */
public interface ResourceManager {

    BufferedReader getReader(String name, Charset charset) throws IOException;

    boolean isFile(String filename);

    String getFilePath();
}
