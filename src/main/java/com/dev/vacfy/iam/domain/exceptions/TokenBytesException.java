package com.dev.vacfy.iam.domain.exceptions;

public class TokenBytesException extends RuntimeException {
    public TokenBytesException() {
        super("Opaque tokens must contain at least 32 random bytes");
    }
}
