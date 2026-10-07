package com.dev.vacfy.monitoring.interfaces.rest;

import com.dev.vacfy.monitoring.domain.exceptions.ForbiddenException;
import com.dev.vacfy.monitoring.domain.model.valueobjects.AccessScope;
import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;
import jakarta.servlet.http.HttpServletRequest;

/** Quién hace la petición, según los atributos que deja AuthorizationRequestFilter. */
public final class RequestViewer {
    private RequestViewer() { }

    public static Viewer from(HttpServletRequest request) {
        return Viewer.of(request.getAttribute("userId"), request.getAttribute("userRole"));
    }

    /** 403 si el termo no está entre los que puede ver. */
    public static void requireAccess(AccessScope scope, String contenedor) {
        if (!scope.canSee(contenedor == null ? null : contenedor.trim())) {
            throw new ForbiddenException("No tiene acceso al termo " + contenedor + ": vincúlelo primero con su clave.");
        }
    }
}
