package com.dev.vacfy.iam.infrastructure.authorization.sfs.websocket;

import com.dev.vacfy.iam.infrastructure.cookies.CookieService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * Autentica el handshake de los WebSockets con la misma cookie de sesión que la API REST o, si no llega la
 * cookie (panel web en otro dominio), con un ticket de un solo uso en {@code ?ticket=}. Guarda "userId" y
 * "userRole" en los atributos de la sesión WebSocket. Sin sesión válida, rechaza con 401.
 */
@Component
public class SessionHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHandshakeInterceptor.class);
    private final OpaqueTokenService opaqueTokenService;
    private final CookieService cookieService;
    private final WebSocketTicketService ticketService;

    public SessionHandshakeInterceptor(OpaqueTokenService opaqueTokenService, CookieService cookieService,
                                       WebSocketTicketService ticketService) {
        this.opaqueTokenService = opaqueTokenService;
        this.cookieService = cookieService;
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        Optional<AuthorizationResponse> session = authenticate(request);
        if (session.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            LOGGER.debug("Handshake WebSocket rechazado: sin sesión válida");
            return false;
        }
        attributes.put("userId", session.get().userId());
        attributes.put("userRole", session.get().role());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private Optional<AuthorizationResponse> authenticate(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return Optional.empty();
        HttpServletRequest http = servletRequest.getServletRequest();
        // AuthorizationRequestFilter ya validó la cookie en esta misma petición
        Object userId = http.getAttribute("userId");
        Object role = http.getAttribute("userRole");
        if (userId != null && role != null) return Optional.of(new AuthorizationResponse(userId.toString(), role.toString()));
        Optional<AuthorizationResponse> fromCookie =
                cookieService.getTokenFromCookie(http).flatMap(opaqueTokenService::getUserDataFromToken);
        if (fromCookie.isPresent()) return fromCookie;
        return ticketService.redeem(http.getParameter("ticket"));
    }
}
