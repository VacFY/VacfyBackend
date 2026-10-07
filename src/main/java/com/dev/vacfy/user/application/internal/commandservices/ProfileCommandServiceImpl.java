package com.dev.vacfy.user.application.internal.commandservices;

import com.dev.vacfy.iam.domain.exceptions.ContextFacadeException;
import com.dev.vacfy.iam.interfaces.acl.IamContextFacade;
import com.dev.vacfy.user.domain.exceptions.DniAlreadyRegisteredException;
import com.dev.vacfy.user.domain.exceptions.ProfileNotFoundException;
import com.dev.vacfy.user.domain.model.aggregates.Profile;
import com.dev.vacfy.user.domain.model.commands.CreateProfileCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateDniCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateProfileCommand;
import com.dev.vacfy.user.domain.services.ProfileCommandService;
import com.dev.vacfy.user.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {
    private final ProfileRepository profileRepository;
    private final IamContextFacade iamContextFacade;

    public ProfileCommandServiceImpl(ProfileRepository profileRepository, IamContextFacade iamContextFacade) {
        this.profileRepository = profileRepository;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    public ImmutablePair<Boolean, Exception> handle(CreateProfileCommand createProfileCommand) {
        if (profileRepository.existsByProfileDni_ProfileDni(createProfileCommand.profileDni())) return new ImmutablePair<>(false, new RuntimeException("DNI already registered"));
        var profile = new Profile(createProfileCommand.profileId(), createProfileCommand.profileDni());
        try {
            profileRepository.save(profile);
            return new ImmutablePair<>(true, null);
        } catch (Exception e) {
            return new ImmutablePair<>(false, e);
        }
    }

    @Override
    public void handle(UpdateDniCommand updateDniCommand) {
        var profile = profileRepository.findByProfileId_ProfileId(UUID.fromString(updateDniCommand.profileId()));
        if (profile.isEmpty()) throw new ProfileNotFoundException(updateDniCommand.profileId());
        if (profileRepository.existsByProfileDni_ProfileDni(updateDniCommand.profileDni())) throw new DniAlreadyRegisteredException(updateDniCommand.profileDni());

        var result = iamContextFacade.changeCredentialDni(updateDniCommand.profileId(), updateDniCommand.profileDni());
        if (!result.left) {
            throw new ContextFacadeException(result.right);
        }

        var profileToUpdate = profile.get();
        profileRepository.save(profileToUpdate.updateProfileDni(updateDniCommand.profileDni()));
    }

    @Override
    public void handle(UpdateProfileCommand updateProfileCommand) {
        var profile = profileRepository.findByProfileId_ProfileId(UUID.fromString(updateProfileCommand.profileId()));
        if (profile.isEmpty()) throw new ProfileNotFoundException(updateProfileCommand.profileId());
        var profileToUpdate = profile.get();
        profileRepository.save(profileToUpdate.updateProfile(updateProfileCommand.profileName(), updateProfileCommand.profileLastName(), updateProfileCommand.profileCompany()));
    }
}
