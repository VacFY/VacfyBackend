package com.dev.vacfy.iam.infrastructure.authorization.sfs.websocket;

import com.dev.vacfy.iam.infrastructure.cookies.CookieService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionHandshakeInterceptorTest {
    private final OpaqueTokenService tokens = mock(OpaqueTokenService.class);
    private final CookieService cookies = mock(CookieService.class);
    private final SessionHandshakeInterceptor interceptor = new SessionHandshakeInterceptor(tokens, cookies);

    @Test
    void handshakeWithoutSessionIsRejected() {
        when(cookies.getTokenFromCookie(any())).thenReturn(Optional.empty());
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();
        boolean accepted = interceptor.beforeHandshake(new ServletServerHttpRequest(new MockHttpServletRequest()),
                new ServletServerHttpResponse(servletResponse), null, attributes);
        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void handshakeWithSessionCookieKeepsUserAndRole() {
        when(cookies.getTokenFromCookie(any())).thenReturn(Optional.of("token"));
        when(tokens.getUserDataFromToken("token")).thenReturn(Optional.of(new AuthorizationResponse("ana", "ENFERMERA")));
        Map<String, Object> attributes = new HashMap<>();
        var response = new ServletServerHttpResponse(new MockHttpServletResponse());
        assertTrue(interceptor.beforeHandshake(new ServletServerHttpRequest(new MockHttpServletRequest()), response, null, attributes));
        assertEquals("ana", attributes.get("userId"));
        assertEquals("ENFERMERA", attributes.get("userRole"));
    }

    @Test
    void sessionAlreadyValidatedByTheFilterIsReused() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "sup");
        request.setAttribute("userRole", "SUPERVISOR");
        Map<String, Object> attributes = new HashMap<>();
        assertTrue(interceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()), null, attributes));
        assertEquals("SUPERVISOR", attributes.get("userRole"));
        verifyNoInteractions(tokens);
    }

    @Test
    void rejectedHandshakeAnswers401() throws Exception {
        when(cookies.getTokenFromCookie(any())).thenReturn(Optional.empty());
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        var response = new ServletServerHttpResponse(servletResponse);
        interceptor.beforeHandshake(new ServletServerHttpRequest(new MockHttpServletRequest()), response, null, new HashMap<>());
        response.flush();
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
    }
}
