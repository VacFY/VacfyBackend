package com.dev.vacfy.monitoring.domain.model.commands;

public record CreateVaccineCommand(String name, String protectsAgainst, Double minTemp, Double maxTemp,
                                   Boolean freezeSensitive, Boolean heatSensitive, Integer dosesPerVial, String notes) { }
