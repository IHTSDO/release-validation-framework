package org.ihtsdo.rvf.execution.service.impl;

public class RVFExecutionException extends Exception {

	public RVFExecutionException(String message) {
		super(message);
	}

	public RVFExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
