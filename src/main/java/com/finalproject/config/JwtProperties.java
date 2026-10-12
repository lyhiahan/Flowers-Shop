package com.finalproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, String audience,
                            Duration accessTtl, Duration refreshTtl) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank())
            throw new IllegalArgumentException("JWT issuer/audience must not be blank.");
        if (accessTtl == null || accessTtl.isNegative() || accessTtl.isZero()
                || accessTtl.compareTo(Duration.ofHours(1)) > 0)
            throw new IllegalArgumentException("JWT access TTL must be between 0 and 1 hour.");
        if (refreshTtl == null || refreshTtl.compareTo(accessTtl) <= 0
                || refreshTtl.compareTo(Duration.ofDays(30)) > 0)
            throw new IllegalArgumentException("JWT refresh TTL must exceed access TTL and be at most 30 days.");
    }
}
