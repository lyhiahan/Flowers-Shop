package com.finalproject;

import com.finalproject.config.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;

class CredentialConfigurationTests {
    @Test void trackedCloudinaryDefaultsNeverContainCredentials() throws Exception {
        var expected = Map.of(
                "cloudinary.cloud-name", "${CLOUDINARY_CLOUD_NAME:}",
                "cloudinary.api-key", "${CLOUDINARY_API_KEY:}",
                "cloudinary.api-secret", "${CLOUDINARY_API_SECRET:}");
        for (String file : new String[]{"application.properties", "application-dev.properties"}) {
            var configuration = new Properties();
            try (var input = Files.newBufferedReader(Path.of("src/main/resources", file))) {
                configuration.load(input);
            }
            for (var setting : expected.entrySet()) {
                // Assert a boolean so a failing test never prints a leaked value.
                assertThat(setting.getValue().equals(configuration.getProperty(setting.getKey())))
                        .as("%s: %s must have an empty environment-only default", file, setting.getKey())
                        .isTrue();
            }
        }
    }

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
