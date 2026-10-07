package com.dev.vacfy.monitoring.domain.model.queries;

import java.util.Collection;

/**
 * @param status     OPEN (por defecto: ACTIVE + ACKNOWLEDGED), ACTIVE, ACKNOWLEDGED, RESOLVED o ALL
 * @param contenedor opcional
 * @param containers termos que puede ver quien consulta; null = todos (supervisor)
 */
public record GetAlertsQuery(String status, String contenedor, Collection<String> containers) {
    public GetAlertsQuery(String status, String contenedor) {
        this(status, contenedor, null);
    }
}
