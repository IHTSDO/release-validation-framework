package org.ihtsdo.rvf.helper;

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
public class MissingEntityRestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { MissingEntityException.class })
    protected ResponseEntity<Object> handleMissingCat(
            final RuntimeException exception, final WebRequest request) {

        final String bodyOfResponse = exception.getMessage();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return handleExceptionInternal(exception, bodyOfResponse, headers,
                HttpStatus.NOT_FOUND, request);
    }

}