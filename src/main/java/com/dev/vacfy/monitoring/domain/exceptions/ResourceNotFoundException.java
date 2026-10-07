package com.dev.vacfy.monitoring.domain.exceptions;

/** El recurso pedido no existe (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
