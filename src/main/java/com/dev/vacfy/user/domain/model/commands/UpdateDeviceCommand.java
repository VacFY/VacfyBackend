package com.dev.vacfy.user.domain.model.commands;

public record UpdateDeviceCommand(String deviceUserId, String deviceName, String deviceConnectionAddress) {
}
