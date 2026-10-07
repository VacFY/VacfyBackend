package com.dev.vacfy.user.domain.exceptions;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String message) {
        super(String.format("Profile %s not found", message));
    }
}
