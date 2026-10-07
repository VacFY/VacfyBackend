package com.dev.vacfy.iam.domain.model.commands;

public record UpdatePasswordCommand(String userId, String currentPassword, String newPassword) {
}
