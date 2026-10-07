package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.exceptions.ConflictException;
import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.exceptions.InvalidDataException;
import com.dev.vacfy.monitoring.domain.exceptions.ResourceNotFoundException;
import com.dev.vacfy.monitoring.domain.exceptions.TooManyRequestsException;
import com.dev.vacfy.monitoring.interfaces.rest.resources.ApiErrorResource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Convierte las excepciones de vacunas, lotes y termos en un JSON con "message" para la enfermera.
 * Solo atiende estas excepciones: los errores del resto de endpoints no cambian.
 */
@RestControllerAdvice
public class MonitoringExceptionHandler {

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ApiErrorResource> handleInvalidData(InvalidDataException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResource> handleConflict(ConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResource> handleForbidden(ForbiddenException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResource> handleTooManyRequests(TooManyRequestsException e, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResource> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    private static ResponseEntity<ApiErrorResource> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResource(Instant.now().toString(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
