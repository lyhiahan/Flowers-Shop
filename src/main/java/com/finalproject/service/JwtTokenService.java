package com.finalproject.service;

import com.finalproject.config.JwtProperties;
import com.finalproject.entity.*;
import com.finalproject.repository.*;
import com.finalproject.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtSessionRepository sessions;
    private final JwtRefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final CustomUserDetailsService userDetails;
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record Tokens(String accessToken, String tokenType, long expiresIn,
                         String refreshToken, long refreshExpiresIn) {}

    @Transactional
    public Tokens issue(Authentication authentication) {
        User user = users.findByUsername(authentication.getName()).orElseThrow(JwtTokenService::invalidCredentials);
        requireEnabled(user);
        JwtSession session = sessions.save(JwtSession.builder().id(UUID.randomUUID().toString())
                .user(user).credentialHash(hash(user.getPassword()))
                .expiresAt(clock.instant().plus(properties.refreshTtl())).build());
        return createTokens(session);
    }

    // A replay revokes the family and must commit the revocation even when returning 401.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Tokens refresh(String rawToken) {
        if (rawToken == null || rawToken.length() != 64) throw invalidCredentials();
        JwtRefreshToken token = refreshTokens.findById(hash(rawToken)).orElseThrow(JwtTokenService::invalidCredentials);
        JwtSession session = sessions.lockById(token.getSession().getId()).orElseThrow(JwtTokenService::invalidCredentials);
        // Locking the session serializes all rotation/logout requests for this token family.
        entityManager.refresh(token);
        if (token.isConsumed()) {
            session.setRevoked(true);
            throw invalidCredentials();
        }
        if (session.isRevoked() || !session.getExpiresAt().isAfter(clock.instant())
                || !token.getExpiresAt().isAfter(clock.instant())) throw invalidCredentials();
        User user = session.getUser();
        requireEnabled(user);
        if (!hash(user.getPassword()).equals(session.getCredentialHash())) {
            session.setRevoked(true);
            throw invalidCredentials();
        }
        token.setConsumed(true);
        return createTokens(session);
    }

    @Transactional(readOnly = true)
    public JwtAuthenticationToken authenticate(Jwt jwt) {
        JwtSession session = sessions.findById(jwt.getClaimAsString("sid"))
                .orElseThrow(() -> new InvalidBearerTokenException("Token session no longer exists."));
        User user = session.getUser();
        if (session.isRevoked() || !session.getExpiresAt().isAfter(clock.instant())
                || !user.getUsername().equals(jwt.getSubject())
                || !hash(user.getPassword()).equals(session.getCredentialHash()))
            throw new InvalidBearerTokenException("Token has been revoked.");
        var details = userDetails.loadUserByUsername(user.getUsername());
        if (!details.isEnabled()) throw new InvalidBearerTokenException("Account is disabled.");
        // Read authorities from the database on every request so role changes apply immediately.
        return new JwtAuthenticationToken(jwt, details.getAuthorities(), details.getUsername());
    }

    @Transactional
    public void revoke(Jwt jwt) {
        sessions.lockById(jwt.getClaimAsString("sid")).ifPresent(s -> s.setRevoked(true));
    }

    private Tokens createTokens(JwtSession session) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer())
                .audience(List.of(properties.audience())).subject(session.getUser().getUsername())
                .issuedAt(now).notBefore(now).expiresAt(now.plus(properties.accessTtl()))
                .id(UUID.randomUUID().toString()).claim("sid", session.getId()).claim("token_type", "access").build();
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(JwtRefreshToken.builder().tokenHash(hash(refresh)).session(session)
                .expiresAt(session.getExpiresAt()).build());
        return new Tokens(access, "Bearer", properties.accessTtl().toSeconds(), refresh,
                java.time.Duration.between(now, session.getExpiresAt()).toSeconds());
    }

    private static void requireEnabled(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin())
                || user.isRetiredDeliveryAccount()) throw invalidCredentials();
    }

    private static BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Thông tin đăng nhập hoặc token không hợp lệ.");
    }

    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
