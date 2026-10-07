package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.commands.UpdateProfileCommand;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateProfileResource;

public class UpdateProfileCommandFromResourceAssembler {
    public static UpdateProfileCommand toCommandFromResource(String profileId, UpdateProfileResource updateProfileResource) {
        return new UpdateProfileCommand(profileId, updateProfileResource.profileName(), updateProfileResource.profileLastName(), updateProfileResource.profileCompany());
    }
}
