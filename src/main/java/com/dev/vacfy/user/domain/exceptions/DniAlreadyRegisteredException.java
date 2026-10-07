package com.dev.vacfy.user.domain.exceptions;

public class DniAlreadyRegisteredException extends RuntimeException {
    public DniAlreadyRegisteredException(String message) {
        super(String.format("DNI %s already registered", message));
    }
}
