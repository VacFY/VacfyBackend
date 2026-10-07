package com.dev.vacfy.monitoring.application.internal.outboundservices;

import com.dev.vacfy.monitoring.domain.model.aggregates.Alert;

/** Puerto de salida: avisar a los clientes (panel / app) que una alerta se abrió, se vio o se cerró. */
public interface AlertPublisher {
    void publish(Alert alert);
}
