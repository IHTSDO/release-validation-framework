package org.ihtsdo.release.assertion;

import org.ihtsdo.release.assertion.log.ValidationLog;

public interface ResourceProviderFactory {

	ValidationLog getValidationLog(Class<?> subject);

}
