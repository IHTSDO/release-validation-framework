package org.ihtsdo.rvf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="ID is not a valid assertion id or UUID") 
public class InvalidFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public InvalidFormatException(String message) {
		super(message);
	}
}
