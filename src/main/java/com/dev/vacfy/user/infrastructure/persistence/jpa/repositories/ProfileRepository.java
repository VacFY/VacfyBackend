package com.dev.vacfy.user.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.user.domain.model.aggregates.Profile;
import com.dev.vacfy.user.domain.model.values.ProfileId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, ProfileId> {
    boolean existsByProfileDni_ProfileDni(String profileDni);
    Optional<Profile> findByProfileId_ProfileId(UUID profileId);
    List<Profile> findByProfileId_ProfileIdIn(Collection<UUID> profileIds);
}
