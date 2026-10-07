package com.dev.vacfy.iam.domain.services;

import com.dev.vacfy.iam.domain.model.commands.SignInCommand;
import com.dev.vacfy.iam.domain.model.commands.SignUpCommand;
import com.dev.vacfy.iam.domain.model.commands.UpdateCredentialDniCommand;
import com.dev.vacfy.iam.domain.model.commands.UpdatePasswordCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;

public interface CredentialCommandService {
    Optional<String> handle(SignUpCommand signUpCommand);
    Optional<String> handle(SignInCommand signInCommand);
    void handle(UpdatePasswordCommand updatePasswordCommand);

    ImmutablePair<Boolean, Exception> handle(UpdateCredentialDniCommand updateCredentialEmailCommand);
}
