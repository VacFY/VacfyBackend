package com.dev.vacfy.iam.infrastructure.authorization.sfs.pipeline;

import com.dev.vacfy.iam.infrastructure.authorization.sfs.model.OpaqueAuthenticationToken;
import com.dev.vacfy.iam.infrastructure.cookies.CookieService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class AuthorizationRequestFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationRequestFilter.class);
    private final OpaqueTokenService opaqueTokenService;
    private final CookieService cookieService;

    public AuthorizationRequestFilter(OpaqueTokenService opaqueTokenService, CookieService cookieService) {
        this.opaqueTokenService = opaqueTokenService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        LOGGER.info("{} {}", request.getMethod(), request.getRequestURI());
        try {
            Optional<String> token = cookieService.getTokenFromCookie(request);

            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                //response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String sessionToken = token.get();

            var authorizationResponse = opaqueTokenService.getUserDataFromToken(sessionToken);

            if (authorizationResponse.isEmpty()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                //response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            request.setAttribute("opaqueToken", sessionToken);
            request.setAttribute("userId", authorizationResponse.get().userId());
            request.setAttribute("userRole", authorizationResponse.get().role());

            //----------------------------------------------------------------------------------

            LOGGER.info("URI: {}", request.getRequestURI());
            LOGGER.info("Token encontrado: {}", token.isPresent());

            if (authorizationResponse.isPresent()) {
                LOGGER.info("Usuario autenticado: {}", authorizationResponse.get().userId());

                SecurityContextHolder.getContext()
                        .setAuthentication(new OpaqueAuthenticationToken(sessionToken));

                LOGGER.info(
                        "Authentication después de establecerla: {}",
                        SecurityContextHolder.getContext().getAuthentication()
                );
            }

            //----------------------------------------------------------------------------------

            SecurityContextHolder.getContext().setAuthentication(new OpaqueAuthenticationToken(sessionToken));

        } catch (Exception e) {
            LOGGER.error("Authentication failed", e);
            throw e;
        }
        filterChain.doFilter(request, response);
    }
}