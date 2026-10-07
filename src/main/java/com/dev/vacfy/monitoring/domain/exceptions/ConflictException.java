package com.dev.vacfy.monitoring.domain.exceptions;

/** La operación choca con lo que ya existe (HTTP 409). El mensaje se muestra tal cual a la enfermera. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
