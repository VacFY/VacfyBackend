package com.dev.vacfy.user.domain.model.commands;

public record CreateDeviceCommand(String deviceUserId, String deviceName, String deviceConnectionAddress) {
}
