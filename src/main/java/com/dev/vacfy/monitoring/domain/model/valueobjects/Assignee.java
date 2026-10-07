package com.dev.vacfy.monitoring.domain.model.valueobjects;

/** Usuario que tiene o tuvo un termo. fullName es null si no completó su perfil. */
public record Assignee(String userId, String dni, String fullName) { }
