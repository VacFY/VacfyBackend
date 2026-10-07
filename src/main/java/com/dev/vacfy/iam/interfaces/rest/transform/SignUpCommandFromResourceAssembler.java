package com.dev.vacfy.iam.interfaces.rest.transform;

import com.dev.vacfy.iam.domain.model.commands.SignUpCommand;
import com.dev.vacfy.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource signUpResource) {
        return new SignUpCommand(signUpResource.userDni(), signUpResource.userPassword());
    }
}
