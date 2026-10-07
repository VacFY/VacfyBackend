package com.dev.vacfy.monitoring.domain.exceptions;

/** Demasiados intentos seguidos (HTTP 429). */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
