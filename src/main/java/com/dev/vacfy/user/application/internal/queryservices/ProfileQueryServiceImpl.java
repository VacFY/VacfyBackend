package com.dev.vacfy.user.application.internal.queryservices;

import com.dev.vacfy.user.domain.model.aggregates.Profile;
import com.dev.vacfy.user.domain.model.queries.GetProfileByIdQuery;
import com.dev.vacfy.user.domain.services.ProfileQueryService;
import com.dev.vacfy.user.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {
    private final ProfileRepository profileRepository;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> handle(GetProfileByIdQuery getProfileByIdQuery) {
        return profileRepository.findByProfileId_ProfileId(UUID.fromString(getProfileByIdQuery.profileId()));
    }
}
