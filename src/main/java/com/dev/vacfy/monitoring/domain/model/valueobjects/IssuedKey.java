package com.dev.vacfy.monitoring.domain.model.valueobjects;

import com.dev.vacfy.monitoring.domain.model.aggregates.Container;

/** Termo con su clave en claro, que solo se muestra en esta respuesta (en la base queda el hash). */
public record IssuedKey(Container container, String clave) { }
