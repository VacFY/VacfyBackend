package com.dev.vacfy.iam.application.internal.commandservices;

import com.dev.vacfy.iam.application.internal.outboundservices.hashing.HashingService;
import com.dev.vacfy.iam.domain.exceptions.ContextFacadeException;
import com.dev.vacfy.iam.domain.exceptions.DniAlreadyRegisteredException;
import com.dev.vacfy.iam.domain.exceptions.InvalidCredentialsException;
import com.dev.vacfy.iam.domain.exceptions.UserNotFoundException;
import com.dev.vacfy.iam.domain.model.aggregates.Credential;
import com.dev.vacfy.iam.domain.model.commands.SignInCommand;
import com.dev.vacfy.iam.domain.model.commands.SignUpCommand;
import com.dev.vacfy.iam.domain.model.commands.UpdateCredentialDniCommand;
import com.dev.vacfy.iam.domain.model.commands.UpdatePasswordCommand;
import com.dev.vacfy.iam.domain.services.CredentialCommandService;
import com.dev.vacfy.iam.infrastructure.persistence.jpa.repositories.CredentialRepository;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import com.dev.vacfy.user.interfaces.acl.ProfileContextFacade;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialCommandServiceImpl implements CredentialCommandService {
    private final CredentialRepository credentialRepository;
    private final HashingService hashingService;
    private final OpaqueTokenService opaqueTokenService;

    private final ProfileContextFacade profileContextFacade;

    public CredentialCommandServiceImpl(CredentialRepository credentialRepository, HashingService hashingService, OpaqueTokenService opaqueTokenService, @Lazy ProfileContextFacade profileContextFacade) {
        this.credentialRepository = credentialRepository;
        this.hashingService = hashingService;
        this.opaqueTokenService = opaqueTokenService;
        this.profileContextFacade = profileContextFacade;
    }

    @Override
    public Optional<String> handle(SignUpCommand signUpCommand) {
        if (credentialRepository.existsByUserDni_UserDni(signUpCommand.userDni())) throw new DniAlreadyRegisteredException(signUpCommand.userDni());
        var user = new Credential(signUpCommand.userDni(), hashingService.encode(signUpCommand.userPassword()));

        var result = profileContextFacade.createProfile(user.getUserId().userId().toString(), user.getUserDni().userDni());
        if (!result.left) {
            throw new ContextFacadeException(result.right);
        }

        credentialRepository.save(user);
        var token = opaqueTokenService.generateToken(user.getUserId().userId());
        return Optional.of(token);
    }

    @Override
    public Optional<String> handle(SignInCommand signInCommand) {
        var user = credentialRepository.findByUserDni_UserDni(signInCommand.userDni()).orElseThrow(InvalidCredentialsException::new);
        if (!hashingService.matches(signInCommand.userPassword(), user.getUserPassword().userPassword())) throw new InvalidCredentialsException();
        var token = opaqueTokenService.generateToken(user.getUserId().userId());
        return Optional.of(token);
    }

    @Override
    public void handle(UpdatePasswordCommand updatePasswordCommand) {
        var user = credentialRepository.findByUserId_UserId(UUID.fromString(updatePasswordCommand.userId()));
        if (user.isEmpty()) throw new UserNotFoundException(updatePasswordCommand.userId());
        var userToUpdate = user.get();
        if (!hashingService.matches(updatePasswordCommand.currentPassword(), userToUpdate.getUserPassword().userPassword())) throw new InvalidCredentialsException();
        var newPassword = hashingService.encode(updatePasswordCommand.newPassword());
        credentialRepository.save(userToUpdate.updatePassword(newPassword));
    }

    @Override
    public ImmutablePair<Boolean, Exception> handle(UpdateCredentialDniCommand updateCredentialDniCommand) {
        if (credentialRepository.existsByUserDni_UserDni(updateCredentialDniCommand.userDni())) return new ImmutablePair<>(false, new RuntimeException("DNI already registered"));
        var credential = credentialRepository.findByUserId_UserId(UUID.fromString(updateCredentialDniCommand.userId()));
        if (credential.isEmpty()) return new ImmutablePair<>(false, new RuntimeException("User not found"));
        var credentialToUpdate = credential.get();
        try {
            credentialRepository.save(credentialToUpdate.updateDni(updateCredentialDniCommand.userDni()));
            return new ImmutablePair<>(true, null);
        } catch (Exception e) {
            return new ImmutablePair<>(false, e);
        }
    }
}