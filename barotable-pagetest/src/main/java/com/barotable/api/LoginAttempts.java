package com.barotable.api;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
class LoginAttempts {

    static final String BLOCKED_MESSAGE = "로그인 시도가 너무 많아요. 5분 뒤에 다시 시도해 주세요";

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int failures, Instant blockedUntil) {}

    synchronized boolean isBlocked(String email) {
        Attempt attempt = attempts.get(email);
        if (attempt == null || attempt.blockedUntil() == null) return false;
        if (Instant.now().isBefore(attempt.blockedUntil())) return true;
        attempts.remove(email);
        return false;
    }

    synchronized void recordFailure(String email) {
        Attempt previous = attempts.get(email);
        int failures = previous == null ? 1 : previous.failures() + 1;
        attempts.put(email, new Attempt(failures, failures == 5 ? Instant.now().plus(5, ChronoUnit.MINUTES) : null));
    }

    synchronized void reset(String email) {
        attempts.remove(email);
    }
}
