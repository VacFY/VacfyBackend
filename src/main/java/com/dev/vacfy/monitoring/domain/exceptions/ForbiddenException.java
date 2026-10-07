package com.dev.vacfy.monitoring.domain.exceptions;

/** El usuario tiene sesión pero no puede ver o hacer esto (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
