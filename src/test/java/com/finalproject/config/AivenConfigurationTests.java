package com.finalproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;

class AivenConfigurationTests {
    @Test void mavenRunDefaultsToAivenWithoutChangingOtherSpringProfiles() throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse("pom.xml");
        assertThat(document.getElementsByTagName("spring-boot.run.profiles").item(0).getTextContent()).isEqualTo("aiven");
        var properties = new Properties();
        try (var stream = getClass().getResourceAsStream("/application.properties")) { properties.load(stream); }
        assertThat(properties.getProperty("spring.profiles.active")).isEqualTo("${SPRING_PROFILES_ACTIVE:dev}");
    }
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
        assertThat(properties.getProperty("app.demo-payment.auto-tunnel.enabled"))
                .isEqualTo("${DEMO_PAYMENT_AUTO_TUNNEL_ENABLED:true}");
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
