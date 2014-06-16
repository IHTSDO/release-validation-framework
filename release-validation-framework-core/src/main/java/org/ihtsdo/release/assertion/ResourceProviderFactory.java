package org.ihtsdo.release.assertion;

import org.ihtsdo.release.assertion.log.ValidationLog;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface ResourceProviderFactory {

	<T extends ResourceProvider> T getResourceProvider(Class<T> resourceProviderClass) throws ResourceProviderFactoryException;

	InputStream getConfigurationStream(String filename) throws FileNotFoundException;

	ValidationLog getValidationLog(Class<?> subject);

}
