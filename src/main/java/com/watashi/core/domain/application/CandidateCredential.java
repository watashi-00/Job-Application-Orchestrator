package com.watashi.core.domain.application;

import java.time.LocalDateTime;
import java.util.Objects;

public record CandidateCredential(
        String domain, String username, String tokenOrSessionCookie, LocalDateTime updatedAt) {

    public CandidateCredential {
        Objects.requireNonNull(domain, "domain cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(tokenOrSessionCookie, "tokenOrSessionCookie cannot be null");
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
