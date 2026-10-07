package com.dev.vacfy.iam.interfaces.rest.transform;

import com.dev.vacfy.iam.domain.model.commands.UpdatePasswordCommand;
import com.dev.vacfy.iam.interfaces.rest.resources.UpdatePasswordResource;

public class UpdatePasswordCommandFromResourceAssembler {
    public static UpdatePasswordCommand toCommandFromResource(String userId, UpdatePasswordResource updatePasswordResource) {
        return new UpdatePasswordCommand(userId, updatePasswordResource.currentPassword(), updatePasswordResource.newPassword());
    }
}
