package com.dev.vacfy.monitoring.domain.model.queries;

import java.time.Instant;

public record GetReadingsQuery(String contenedor, Instant from, Instant to) { }
