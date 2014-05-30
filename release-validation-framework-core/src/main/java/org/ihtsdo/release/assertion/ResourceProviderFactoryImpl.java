package org.ihtsdo.release.assertion;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.release.assertion.log.ValidationLogImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(role = ResourceProviderFactory.class)
public class ResourceProviderFactoryImpl implements ResourceProviderFactory, Contextualizable {

	// Keeping the cache in the plexus context because the keep-alive instantiationStrategy doesn't seem to work.
	private Context plexusContext;

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceProviderFactoryImpl.class);
	private static final String CACHE_KEY = ResourceProviderFactoryImpl.class.getCanonicalName() + ".cache";

	@Override
	public <T extends ResourceProvider> T getResourceProvider(Class<T> resourceProviderClass) throws ResourceProviderFactoryException {
		String resourceProviderClassString = resourceProviderClass.getCanonicalName();
		Map<String, Object> cache = getCache();
		if (!cache.containsKey(resourceProviderClassString)) {
			String syncKey = getClass().getCanonicalName() + "|" + resourceProviderClassString;
			synchronized (syncKey) {
				if (!cache.containsKey(resourceProviderClassString)) {
					ResourceProvider resourceProvider = createResourceProvider(resourceProviderClass);
					cache.put(resourceProviderClassString, resourceProvider);
				}
			}
		}
		T t = (T) cache.get(resourceProviderClassString);
		LOGGER.info("Returning ResourceProvider {}", t);
		return t;
	}

	@Override
	public InputStream getConfigurationStream(String filename) throws FileNotFoundException {
		MavenProject mavenProject = (MavenProject) getCache().get(key(MavenProject.class));
		File basedir = mavenProject.getBasedir();
		File file = new File(basedir, filename);
		return new FileInputStream(file);
	}

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

	private <T extends ResourceProvider> T createResourceProvider(Class<T> resourceProviderClass) throws ResourceProviderFactoryException {
		LOGGER.info("Attempting to create ResourceProvider {}", resourceProviderClass);

		// Pick constructor with the most parameters
		Constructor<T>[] constructors = (Constructor<T>[]) resourceProviderClass.getConstructors();
		Constructor<T> constructorWithMostArgs = null;
		for (Constructor<T> constructor : constructors) {
			if (constructorWithMostArgs == null || constructor.getParameterTypes().length > constructorWithMostArgs.getParameterTypes().length) {
				constructorWithMostArgs = constructor;
			}
		}

		// Resolve arguments by cache lookup
		List initArgs = new ArrayList();
		Map<String, Object> cache = getCache();
		for (Class parameterClass : constructorWithMostArgs.getParameterTypes()) {
			String key = key(parameterClass);

			Object initArg = null;
			if (cache.containsKey(key)) {
				initArg = cache.get(key);
			} else if (ResourceProvider.class.isAssignableFrom(parameterClass)) {
				initArg = createResourceProvider(parameterClass);
			}

			if (initArg != null) {
				initArgs.add(initArg);
			} else {
				LOGGER.error("Cache {}", cache);
				throw new ResourceProviderFactoryException("Unable to use " + resourceProviderClass.getCanonicalName() + " constructor, argument of type " + parameterClass.getCanonicalName() + " not available.");
			}
		}

		// Create instance
		try {
			LOGGER.info("Creating ResourceProvider {} with args {}", resourceProviderClass, initArgs);
			T t = constructorWithMostArgs.newInstance(initArgs.toArray());
			t.init();
			return t;
		} catch (Exception e) {
			throw new ResourceProviderFactoryException("Failed to create ResourceProvider " + resourceProviderClass.getCanonicalName(), e);
		}
	}

	protected void init(MavenProject project, ArtifactRepository artifactRepository) {
		Map<String, Object> cache = getCache();
		cache.put(key(MavenProject.class), project);
		cache.put(key(ArtifactRepository.class), artifactRepository);
	}

	private String key(Object o) {
		return key(o.getClass());
	}

	private String key(Class aClass) {
		return aClass.getCanonicalName();
	}

	private Map<String, Object> getCache() {
		try {
			if (!plexusContext.contains(CACHE_KEY)) {
				synchronized (CACHE_KEY) {
					if (!plexusContext.contains(CACHE_KEY)) {
						plexusContext.put(CACHE_KEY, new HashMap<>());
					}
				}
			}
			return (Map<String, Object>) plexusContext.get(CACHE_KEY);
		} catch (ContextException e) {
			throw new RuntimeException(e);
		}
	}

	@Override // set Context
	public void contextualize(Context context) throws ContextException {
		this.plexusContext = context;
	}

}
