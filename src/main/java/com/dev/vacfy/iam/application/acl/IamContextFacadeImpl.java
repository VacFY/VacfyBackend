package com.dev.vacfy.iam.application.acl;

import com.dev.vacfy.iam.domain.exceptions.GenericException;
import com.dev.vacfy.iam.domain.model.commands.UpdateCredentialDniCommand;
import com.dev.vacfy.iam.domain.services.CredentialCommandService;
import com.dev.vacfy.iam.domain.services.CredentialQueryService;
import com.dev.vacfy.iam.interfaces.acl.IamContextFacade;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

@Service
public class IamContextFacadeImpl implements IamContextFacade {
    private final CredentialCommandService credentialCommandService;
    private final CredentialQueryService credentialQueryService;

    public IamContextFacadeImpl(CredentialCommandService credentialCommandService, CredentialQueryService credentialQueryService) {
        this.credentialCommandService = credentialCommandService;
        this.credentialQueryService = credentialQueryService;
    }

    @Override
    public ImmutablePair<Boolean, Exception> changeCredentialDni(String userId, String userDni) {
        var updateCredentialDniCommand = new UpdateCredentialDniCommand(userId, userDni);
        var result = credentialCommandService.handle(updateCredentialDniCommand);
        if (result == null) throw new GenericException("The action could not be completed");
        return new ImmutablePair<>(result.left, result.right);
    }
}