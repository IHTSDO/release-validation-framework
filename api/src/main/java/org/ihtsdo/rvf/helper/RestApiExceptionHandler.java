package org.ihtsdo.rvf.helper;

import org.ihtsdo.rvf.controller.InvalidFormatException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * A custom exception handler that handles missing entities.
 */
@ControllerAdvice
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(value = { EntityNotFoundException.class })
	protected ResponseEntity<Object> handleMissingEntity(
			final RuntimeException exception, final WebRequest request) {

		final String bodyOfResponse = exception.getMessage();

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		return handleExceptionInternal(exception, bodyOfResponse, headers,
				HttpStatus.NOT_FOUND, request);
	}

	@ExceptionHandler(value = { InvalidFormatException.class })
	protected ResponseEntity<Object> handleFormatNotValidException(
			final RuntimeException exception, final WebRequest request) {
		final String bodyOfResponse = exception.getMessage();
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		return handleExceptionInternal(exception, bodyOfResponse, headers,
				HttpStatus.BAD_REQUEST, request);
	}
}