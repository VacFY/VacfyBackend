package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.commands.UpdateDniCommand;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateDniResource;

public class UpdateDniCommandFromResourceAssembler {
    public static UpdateDniCommand toCommandFromResource(String profileId, UpdateDniResource updateDniResource) {
        return new UpdateDniCommand(profileId, updateDniResource.profileDni());
    }
}
