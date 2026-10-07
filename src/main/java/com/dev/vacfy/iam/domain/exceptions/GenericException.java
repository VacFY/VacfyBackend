package com.dev.vacfy.iam.domain.exceptions;

public class GenericException extends RuntimeException {
    public GenericException(String message) {
        super(String.format("An error has occurred: %s", message));
    }

    public GenericException(String message, String _class) {
        super(String.format("An error has occurred in %s: %s", message, _class));
    }
}
