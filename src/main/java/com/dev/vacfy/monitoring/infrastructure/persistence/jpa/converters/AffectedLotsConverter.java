package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.converters;

import com.dev.vacfy.monitoring.domain.model.valueobjects.AffectedLot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** Guarda la lista de lotes afectados de una alerta como JSON en una columna de texto. */
@Converter
public class AffectedLotsConverter implements AttributeConverter<List<AffectedLot>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<AffectedLot>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<AffectedLot> lots) {
        if (lots == null || lots.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(lots);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar affected_lots", e);
        }
    }

    @Override
    public List<AffectedLot> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            return List.of(); // un valor ilegible no debe impedir leer la alerta
        }
    }
}
