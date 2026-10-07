package com.dev.vacfy.iam.infrastructure.authorization.sfs.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

public class OpaqueAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    public OpaqueAuthenticationToken(Object principal) {
        super(List.of());
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}