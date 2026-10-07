package com.dev.vacfy.iam.interfaces.rest.resources;

public record UpdatePasswordResource(String currentPassword, String newPassword) {
}
