package com.finalproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;

class AivenConfigurationTests {
    @Test void profileNeverImportsSqlServerSecretsAndUsesVerifiedTls() throws Exception {
        var properties = new Properties();
        try (var stream = getClass().getResourceAsStream("/application-aiven.properties")) {
            properties.load(stream);
        }
        assertThat(properties.getProperty("spring.config.import")).isEqualTo("optional:file:./.env.aiven[.properties]");
        assertThat(properties.getProperty("spring.datasource.url")).startsWith("jdbc:postgresql:");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${AIVEN_PASSWORD}");
        assertThat(properties.getProperty("spring.datasource.hikari.data-source-properties.sslmode")).isEqualTo("verify-full");
        assertThat(properties.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("3");
        assertThat(properties.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration/postgresql");
        assertThat(properties.getProperty("spring.flyway.default-schema")).isEqualTo("public");
        assertThat(properties.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
        assertThat(properties.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test void mixedProfilesAreRejectedBeforeConnectingToADatabase() {
        for (String profile : new String[]{"dev", "test"}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles("aiven", profile);
            assertThatThrownBy(() -> AivenProfileConfiguration.isolatedAivenProfile(environment))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("aiven profile alone");
        }
        var environment = new MockEnvironment();
        environment.setActiveProfiles("aiven");
        assertThatCode(() -> AivenProfileConfiguration.isolatedAivenProfile(environment)).doesNotThrowAnyException();
    }
}
