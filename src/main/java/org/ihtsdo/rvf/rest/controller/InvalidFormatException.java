package org.ihtsdo.rvf.rest.controller;

import java.io.Serial;

public class InvalidFormatException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;
	public InvalidFormatException(String message) {
		super(message);
	}
}
