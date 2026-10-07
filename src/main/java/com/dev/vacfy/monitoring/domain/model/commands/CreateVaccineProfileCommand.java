package com.dev.vacfy.monitoring.domain.model.commands;

public record CreateVaccineProfileCommand(String name, Double minTemp, Double maxTemp, Boolean freezeSensitive) { }
