package com.dev.vacfy.monitoring.domain.exceptions;

/** El código escaneado o escrito no se pudo leer (HTTP 400). */
public class Gs1ParseException extends InvalidDataException {
    public Gs1ParseException(String message) {
        super(message);
    }
}
