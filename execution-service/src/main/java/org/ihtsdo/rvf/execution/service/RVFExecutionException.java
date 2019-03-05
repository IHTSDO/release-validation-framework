package org.ihtsdo.rvf.execution.service;

public class RVFExecutionException extends Exception {
	
	private static final long serialVersionUID = 917020585637000155L;

	public RVFExecutionException(String message) {
		super(message);
	}

	public RVFExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
