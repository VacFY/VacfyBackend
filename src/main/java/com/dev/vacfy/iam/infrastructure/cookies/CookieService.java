package com.dev.vacfy.iam.infrastructure.cookies;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

public interface CookieService {

    void setTokenCookie(HttpServletResponse response, String token);

    void clearTokenCookie(HttpServletResponse response);

    Optional<String> getTokenFromCookie(HttpServletRequest request);
}