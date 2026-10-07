package com.dev.vacfy.monitoring.domain.model.commands;

import com.dev.vacfy.monitoring.domain.model.valueobjects.Viewer;

/** @param clave clave fija de la etiqueta (K7P-29Q) o código temporal del QR (K7P29Q) */
public record LinkContainerCommand(Viewer viewer, String codigo, String clave) { }
