package com.dev.vacfy.monitoring.domain.model.commands;

/** Los campos en null mantienen su valor actual. */
public record UpdateVaccineCommand(Long vaccineId, String name, String protectsAgainst, Double minTemp, Double maxTemp,
                                   Boolean freezeSensitive, Boolean heatSensitive, Integer dosesPerVial, String notes,
                                   Boolean verified) { }
