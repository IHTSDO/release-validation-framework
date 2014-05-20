package org.ihtsdo.release.assertion;

public interface ResourceProviderFactory {

	<T extends ResourceProvider> T getResourceProvider(Class<T> resourceProviderClass) throws ResourceProviderFactoryException;

}
