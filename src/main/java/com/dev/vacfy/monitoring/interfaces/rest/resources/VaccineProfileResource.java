package com.dev.vacfy.monitoring.interfaces.rest.resources;

public record VaccineProfileResource(Long id, String name, Double minTemp, Double maxTemp, Boolean freezeSensitive) { }
