package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProvider;
import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.ResourceProviderFactoryException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 *
 */
public class RvfResourceProviderFactoryImpl implements ResourceProviderFactory {

    @Override
    public <T extends ResourceProvider> T getResourceProvider(Class<T> resourceProviderClass) throws ResourceProviderFactoryException {
        System.out.println("testing 1");
        return null;
    }

    @Override
    public InputStream getConfigurationStream(String filename) throws FileNotFoundException {
        File file = new File(filename);
        return new FileInputStream(file);
    }
}
