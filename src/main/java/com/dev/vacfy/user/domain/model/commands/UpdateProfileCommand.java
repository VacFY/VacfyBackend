package com.dev.vacfy.user.domain.model.commands;

public record UpdateProfileCommand(String profileId, String profileName, String profileLastName, String profileCompany) {
}
