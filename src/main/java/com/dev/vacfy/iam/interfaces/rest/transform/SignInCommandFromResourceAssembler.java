package com.dev.vacfy.iam.interfaces.rest.transform;

import com.dev.vacfy.iam.domain.model.commands.SignInCommand;
import com.dev.vacfy.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(signInResource.userDni(), signInResource.userPassword());
    }
}
