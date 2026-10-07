package com.dev.vacfy.monitoring.domain.model.commands;

public record AcknowledgeAlertCommand(Long alertId, String userId) { }
