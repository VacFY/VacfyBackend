package com.dev.vacfy.user.application.acl;

import com.dev.vacfy.iam.domain.exceptions.GenericException;
import com.dev.vacfy.user.domain.model.commands.CreateProfileCommand;
import com.dev.vacfy.user.domain.services.ProfileCommandService;
import com.dev.vacfy.user.interfaces.acl.ProfileContextFacade;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

@Service
public class ProfileContextFacadeImpl implements ProfileContextFacade {
    private final ProfileCommandService profileCommandService;

    public ProfileContextFacadeImpl(ProfileCommandService profileCommandService) {
        this.profileCommandService = profileCommandService;
    }

    public ImmutablePair<Boolean, Exception> createProfile(String profileId, String profileDni) {
        var createProfileCommand = new CreateProfileCommand(profileId, profileDni);
        var result = profileCommandService.handle(createProfileCommand);
        if (result == null) throw new GenericException("The action could not be completed");
        return new ImmutablePair<>(result.left, result.right);
    }
}
