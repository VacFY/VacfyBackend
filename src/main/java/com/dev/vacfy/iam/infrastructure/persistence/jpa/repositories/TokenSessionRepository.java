package com.dev.vacfy.iam.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.TokenSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface TokenSessionRepository extends JpaRepository<TokenSession, String> {
    @Transactional
    long deleteByExpiresAtBefore(Instant instant);
}
