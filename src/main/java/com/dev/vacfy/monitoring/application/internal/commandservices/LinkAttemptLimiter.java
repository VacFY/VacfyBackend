package com.dev.vacfy.monitoring.application.internal.commandservices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuenta los intentos fallidos de vinculación por clave ("user:<id>" o "code:<codigo>") en una ventana móvil.
 * Vive en memoria: con una sola instancia del backend es suficiente; con varias, cada una cuenta por separado.
 */
@Component
public class LinkAttemptLimiter {
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LinkAttemptLimiter(@Value("${vacty.linking.max-failed-attempts:5}") int maxFailures,
                              @Value("${vacty.linking.window-minutes:10}") long windowMinutes) {
        this(maxFailures, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    LinkAttemptLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public long windowMinutes() {
        return window.toMinutes();
    }

    public boolean isBlocked(String key) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) return false;
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= maxFailures;
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) attempts.pollFirst();
    }
}
