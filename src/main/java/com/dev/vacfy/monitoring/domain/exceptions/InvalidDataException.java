package com.dev.vacfy.monitoring.domain.exceptions;

/** Datos inválidos enviados por el cliente (HTTP 400). El mensaje se muestra tal cual a la enfermera. */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}
