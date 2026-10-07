package com.dev.vacfy.monitoring.domain.model.commands;

/** @param status USED o DISCARDED */
public record CloseLotCommand(Long lotId, String status, String reason) { }
