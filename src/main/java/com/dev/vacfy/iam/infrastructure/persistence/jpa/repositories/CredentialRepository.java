package com.dev.vacfy.iam.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.iam.domain.model.aggregates.Credential;
import com.dev.vacfy.iam.domain.model.values.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, UserId> {
    Optional<Credential> findByUserDni_UserDni(String userEmail);
    Optional<Credential> findByUserId_UserId(UUID userId);
    boolean existsByUserDni_UserDni(String userEmail);
}
