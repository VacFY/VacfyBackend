package com.dev.vacfy.iam.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.SessionUser;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.TokenSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenSessionRepository extends JpaRepository<TokenSession, String> {
    /** Sesión y rol actual del usuario en una sola consulta (el rol puede cambiar sin volver a iniciar sesión). */
    @Query("select new com.dev.vacfy.iam.infrastructure.tokens.opaque.models.SessionUser(s.userId, s.expiresAt, c.role) "
            + "from TokenSession s, Credential c where s.tokenHash = :tokenHash and c.userId.userId = s.userId")
    Optional<SessionUser> findSessionUser(@Param("tokenHash") String tokenHash);

    @Transactional
    long deleteByExpiresAtBefore(Instant instant);
}
