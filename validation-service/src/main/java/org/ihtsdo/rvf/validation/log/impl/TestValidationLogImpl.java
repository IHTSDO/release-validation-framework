package org.ihtsdo.rvf.validation.log.impl;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestValidationLogImpl implements ValidationLog {

	private final Logger logger;
	private Map<String, Object[]> errors;
	private Class subject;

	public TestValidationLogImpl(Class subject) {
		this.subject = subject;
		logger = LoggerFactory.getLogger(subject);
		errors = new HashMap<>();
	}

	@Override
	public void assertionError(String message, Object... object) {
		errors.put(message, object);
		logger.error(message, object);
	}

	@Override
	public void configurationError(String message, Object... object) {
		errors.put(message, object);
		logger.error(message, object);
	}

	@Override
	public void executionError(String message, Object... object) {
		errors.put(message, object);
		logger.error(message, object);
	}

	@Override
	public void info(String message, Object... object) {
		logger.info(message, object);
	}

	public java.util.Set<String> getErrorStringsOrNull() {
		Set<String> strings = errors.keySet();
		return strings.isEmpty() ? null : strings;
	}

	public Map<String, Object[]> getErrorsAndArgumentsMap() {
		return errors;
	}

}
