package com.dev.vacfy.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnauthorizedRequestHandlerEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {

        LOGGER.error("AuthenticationEntryPoint executed");
        LOGGER.error("Unauthorized request: {}", ex.getMessage());
        LOGGER.error("Exception: {}", ex.getClass().getName(), ex);

        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}