package com.dev.vacfy.iam.domain.exceptions;

public class DniAlreadyRegisteredException extends RuntimeException {
    public DniAlreadyRegisteredException(String message) {
        super(String.format("DNI %s already registered", message));
    }
}
