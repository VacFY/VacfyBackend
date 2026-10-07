package com.dev.vacfy.iam.domain.exceptions;

public class SecretBytesException extends RuntimeException {
    public SecretBytesException() {
        super("The secret must contain at least 32 bytes (characters)");
    }
}
