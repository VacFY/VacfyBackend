package com.dev.vacfy.user.domain.exceptions;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) {
        super(String.format("The device associated with the user ID %s was not found", message));
    }
}
