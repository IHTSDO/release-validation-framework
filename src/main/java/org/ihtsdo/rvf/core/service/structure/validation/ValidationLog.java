package org.ihtsdo.rvf.core.service.structure.validation;

public interface ValidationLog {

	void assertionError(String message, Object... object);

	void configurationError(String message, Object... object);

	void executionError(String message, Object... object);

	void info(String message, Object... object);

}
