package com.dev.vacfy.iam.infrastructure.tokens.opaque.services;

import com.dev.vacfy.iam.domain.exceptions.SecretBytesException;
import com.dev.vacfy.iam.domain.exceptions.TokenBytesException;
import com.dev.vacfy.iam.domain.model.values.UserRole;
import com.dev.vacfy.iam.infrastructure.persistence.jpa.repositories.TokenSessionRepository;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.AuthorizationResponse;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.models.TokenSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OpaqueTokenServiceImpl implements OpaqueTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpaqueTokenServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private final TokenSessionRepository tokenSessionRepository;

    @Value("${authorization.opaque.secret}")
    private String secret;

    @Value("${authorization.opaque.token.bytes}")
    private int tokenBytes;

    @Value("${authorization.opaque.ttl-hours:12}")
    private long ttlHours;

    public OpaqueTokenServiceImpl(TokenSessionRepository tokenSessionRepository) {
        this.tokenSessionRepository = tokenSessionRepository;
    }

    @PostConstruct
    public void validateConfiguration() {
        int secretBytes = secret.getBytes(StandardCharsets.UTF_8).length;

        LOGGER.info("Opaque secret bytes: {}", secretBytes);
        LOGGER.info("Token bytes configured: {}", tokenBytes);

        if (secretBytes < 32) throw new SecretBytesException();
        if (tokenBytes < 32) throw new TokenBytesException();
    }

    private String hashToken(String token) {

        try {

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(getSigningKey());

            byte[] bytes = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(bytes);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot hash opaque token", e);
        }

    }

    //-------------------------------------------------------------------------------------------------------------------

    @Override
    public String generateToken(UUID userId) {

        String token = generateSecureToken();

        Instant expiresAt = Instant.now().plus(ttlHours, ChronoUnit.HOURS);

        tokenSessionRepository.save(new TokenSession(hashToken(token), userId, expiresAt));

        return token;
    }

    private String generateSecureToken() {

        byte[] bytes = new byte[tokenBytes];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    //-------------------------------------------------------------------------------------------------------------------

    @Override
    public Optional<AuthorizationResponse> getUserDataFromToken(String token) {
        return tokenSessionRepository.findSessionUser(hashToken(token))
                .filter(session -> Instant.now().isBefore(session.expiresAt()))
                .map(session -> new AuthorizationResponse(session.userId().toString(),
                        (session.role() == null ? UserRole.ENFERMERA : session.role()).name()));
    }

    //-------------------------------------------------------------------------------------------------------------------

    @Override
    public void revokeToken(String token) {
        tokenSessionRepository.deleteById(hashToken(token));
    }

    //-------------------------------------------------------------------------------------------------------------------

    /** Cada hora borra las sesiones vencidas para que la tabla no crezca. */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void deleteExpiredSessions() {
        long deleted = tokenSessionRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) LOGGER.info("Sesiones vencidas eliminadas: {}", deleted);
    }

}