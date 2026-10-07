package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import jakarta.servlet.http.HttpServletRequest;

/** Quién hace la petición, según los atributos que deja AuthorizationRequestFilter. */
public final class RequestViewer {
    private RequestViewer() { }

    public static Viewer from(HttpServletRequest request) {
        return Viewer.of(request.getAttribute("userId"), request.getAttribute("userRole"));
    }
}
