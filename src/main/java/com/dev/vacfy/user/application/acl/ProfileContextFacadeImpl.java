package com.dev.vacfy.user.application.acl;

import com.dev.vacfy.iam.domain.exceptions.GenericException;
import com.dev.vacfy.user.domain.model.commands.CreateProfileCommand;
import com.dev.vacfy.user.domain.services.ProfileCommandService;
import com.dev.vacfy.user.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.dev.vacfy.user.interfaces.acl.ProfileContactData;
import com.dev.vacfy.user.interfaces.acl.ProfileContextFacade;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileContextFacadeImpl implements ProfileContextFacade {
    private static final String UNDEFINED = "Undefined";

    private final ProfileCommandService profileCommandService;
    private final ProfileRepository profileRepository;

    public ProfileContextFacadeImpl(ProfileCommandService profileCommandService, ProfileRepository profileRepository) {
        this.profileCommandService = profileCommandService;
        this.profileRepository = profileRepository;
    }

    public ImmutablePair<Boolean, Exception> createProfile(String profileId, String profileDni) {
        var createProfileCommand = new CreateProfileCommand(profileId, profileDni);
        var result = profileCommandService.handle(createProfileCommand);
        if (result == null) throw new GenericException("The action could not be completed");
        return new ImmutablePair<>(result.left, result.right);
    }

    @Override
    public Map<String, ProfileContactData> getContactData(Collection<String> userIds) {
        List<UUID> ids = userIds.stream().map(ProfileContextFacadeImpl::toUuid).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return Map.of();
        return profileRepository.findByProfileId_ProfileIdIn(ids).stream()
                .collect(Collectors.toMap(profile -> profile.getProfileId().profileId().toString(), profile -> new ProfileContactData(
                        profile.getProfileId().profileId().toString(),
                        profile.getProfileDni().profileDni(),
                        fullName(profile.getProfileName().profileName(), profile.getProfileLastName().profileLastName()))));
    }

    private static String fullName(String name, String lastName) {
        String full = (UNDEFINED.equals(name) ? "" : name) + " " + (UNDEFINED.equals(lastName) ? "" : lastName);
        return full.isBlank() ? null : full.trim();
    }

    private static UUID toUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
