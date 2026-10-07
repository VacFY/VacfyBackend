package com.dev.vacfy.monitoring.domain.model.commands;

import java.util.List;

/** @param careProfile ANTI_FREEZE, PROTECT_FROM_LIGHT_AND_HEAT o CHECK_MANUFACTURER (opcional) */
public record CreateVaccineCommand(String name, String protectsAgainst, Double minTemp, Double maxTemp,
                                   Boolean freezeSensitive, Boolean heatSensitive, Integer dosesPerVial, String notes,
                                   String careProfile, List<String> careInstructions) { }
