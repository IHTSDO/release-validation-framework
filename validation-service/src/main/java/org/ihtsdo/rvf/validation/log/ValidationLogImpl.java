package org.ihtsdo.rvf.validation.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationLogImpl implements ValidationLog {

	private Class subject;
	private final Logger logger;

	public ValidationLogImpl(Class subject) {
		this.subject = subject;
		logger = LoggerFactory.getLogger(subject);
	}

	@Override
	public void assertionError(String message, Object... object) {
		logger.error(message, object);
	}

	@Override
	public void configurationError(String message, Object... object) {
		logger.error(message, object);
	}

	@Override
	public void executionError(String message, Object... object) {
		logger.error(message, object);
	}

	@Override
	public void info(String message, Object... object) {
		logger.info(message, object);
	}

}
