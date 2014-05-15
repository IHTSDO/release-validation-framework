package org.ihtsdo.release.assertion;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.HashMap;
import java.util.Map;

@Component(role = AssertionResourceFactory.class, instantiationStrategy = "keep-alive")
public class AssertionResourceFactoryImpl implements AssertionResourceFactory, Contextualizable {

	private static final String CACHE_KEY = AssertionResourceFactoryImpl.class.getCanonicalName() + ".cache";

	// Keeping the cache in the plexus context because the keep-alive instantiationStrategy doesn't seem to work.
	private Context plexusContext;

	@Override
	public Object getResource(String key) {
		Map<String, Object> cache = getCache();
		if (!cache.containsKey(key)) {
			String syncKey = getClass().getCanonicalName() + "|" + key;
			synchronized (syncKey) {
				if (!cache.containsKey(key)) {
					String freshResource = getFreshResource(key);
					cache.put(key, freshResource);
				}
			}
		}
		return cache.get(key);
	}

	@Override
	public void contextualize(Context context) throws ContextException {
		this.plexusContext = context;
	}

	private String getFreshResource(String key) {
		return "" + Math.random();
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

}
