package com.finalproject.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@lombok.extern.slf4j.Slf4j
public class JwtConfig {
    @Bean
    public SecretKey jwtSigningKey(JwtProperties properties, Environment environment) {
        byte[] bytes;
        if (properties.secret() == null || properties.secret().isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("dev", "test")))
                throw new IllegalStateException("Set JWT_SECRET to Base64 of at least 32 random bytes.");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT uses an ephemeral development key; tokens become invalid after restart. Set JWT_SECRET for a persistent key.");
        } else {
            try { bytes = Base64.getDecoder().decode(properties.secret()); }
            catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET must be Base64."); }
            if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 random bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims = token -> {
            boolean valid = token.getAudience() != null && token.getAudience().contains(properties.audience())
                    && token.getExpiresAt() != null && token.getIssuedAt() != null
                    && token.getSubject() != null && !token.getSubject().isBlank()
                    && "access".equals(token.getClaimAsString("token_type"))
                    && token.getClaimAsString("sid") != null;
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid access token claims.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), claims));
        return decoder;
    }
}
