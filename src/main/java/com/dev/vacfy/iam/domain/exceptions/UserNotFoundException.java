package com.dev.vacfy.iam.domain.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(String.format("User %s not found", message));
    }
}
