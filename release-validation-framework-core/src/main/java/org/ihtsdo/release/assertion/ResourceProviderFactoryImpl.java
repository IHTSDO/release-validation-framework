package org.ihtsdo.release.assertion;

import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.release.assertion.log.ValidationLogImpl;

public class ResourceProviderFactoryImpl implements ResourceProviderFactory {

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

}
