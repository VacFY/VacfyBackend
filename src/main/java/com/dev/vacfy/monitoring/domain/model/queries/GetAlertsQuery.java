package com.dev.vacfy.monitoring.domain.model.queries;

/**
 * @param status OPEN (por defecto: ACTIVE + ACKNOWLEDGED), ACTIVE, ACKNOWLEDGED, RESOLVED o ALL
 * @param contenedor opcional
 */
public record GetAlertsQuery(String status, String contenedor) { }
