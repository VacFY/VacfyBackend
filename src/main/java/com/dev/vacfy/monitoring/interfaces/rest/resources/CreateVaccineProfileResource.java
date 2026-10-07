package com.dev.vacfy.monitoring.interfaces.rest.resources;

public record CreateVaccineProfileResource(String name, Double minTemp, Double maxTemp, Boolean freezeSensitive) { }
