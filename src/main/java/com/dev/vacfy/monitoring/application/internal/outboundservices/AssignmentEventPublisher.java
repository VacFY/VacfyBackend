package com.dev.vacfy.monitoring.application.internal.outboundservices;

import java.util.Collection;

/** Puerto de salida: avisar a los clientes que cambió quién tiene un termo, para que refresquen "Mis termos". */
public interface AssignmentEventPublisher {
    /** @param userIds usuarios afectados (quien lo tomó y quien lo tenía); los supervisores también reciben el aviso */
    void assignmentChanged(String contenedor, Collection<String> userIds);
}
