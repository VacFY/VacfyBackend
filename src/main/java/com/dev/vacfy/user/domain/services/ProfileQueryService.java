package com.dev.vacfy.user.domain.services;

import com.dev.vacfy.user.domain.model.aggregates.Profile;
import com.dev.vacfy.user.domain.model.queries.GetProfileByIdQuery;

import java.util.Optional;

public interface ProfileQueryService {
    Optional<Profile> handle(GetProfileByIdQuery getProfileByIdQuery);
}
