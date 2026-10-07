package com.dev.vacfy.user.domain.model.commands;

public record DeleteDeviceCommand(String profileId, String deviceId) {
}
