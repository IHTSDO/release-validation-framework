package org.ihtsdo.rvf.core.service;

import java.io.Serial;

public class RVFExecutionException extends Exception {
	
	@Serial
	private static final long serialVersionUID = 917020585637000155L;

	public RVFExecutionException(String message) {
		super(message);
	}

	public RVFExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
