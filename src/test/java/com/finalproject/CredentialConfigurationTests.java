package com.finalproject;

import com.finalproject.config.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class CredentialConfigurationTests {
    JwtProperties properties(String secret) {
        return new JwtProperties(secret, "flower-shop-chain", "flower-shop-api", Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test void productionRejectsAbsentShortAndMalformedJwtSecrets() {
        var env = new MockEnvironment(); env.setActiveProfiles("prod");
        for (String secret : new String[]{"", "not base64!", "c2hvcnQ="})
            assertThatThrownBy(() -> new JwtConfig().jwtSigningKey(properties(secret), env)).isInstanceOf(IllegalStateException.class);
    }

    @Test void developmentWithoutSecretGetsRandomKeyRatherThanSharedDefault() {
        var env = new MockEnvironment(); env.setActiveProfiles("dev");
        var config = new JwtConfig();
        byte[] first = config.jwtSigningKey(properties(""), env).getEncoded();
        assertThat(first).hasSize(32);
        assertThat(config.jwtSigningKey(properties(""), env).getEncoded()).isNotEqualTo(first);
    }

    @Test void productionRequiresCompleteCloudinaryConfiguration() {
        var config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "required", true);
        ReflectionTestUtils.setField(config, "cloudName", "");
        ReflectionTestUtils.setField(config, "apiKey", "");
        ReflectionTestUtils.setField(config, "apiSecret", "");
        assertThatThrownBy(config::cloudinary).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(config, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "test");
        ReflectionTestUtils.setField(config, "apiSecret", "test-secret");
        assertThatThrownBy(config::cloudinary).isInstanceOf(IllegalStateException.class);
    }
}
