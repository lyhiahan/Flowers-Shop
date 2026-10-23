package com.finalproject.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevLocalSettingsTests {
    @TempDir Path root;

    @Test void devReadsLocalDatabaseSettingsAndSharedAccountRestrictions() throws Exception {
        writeSettings();
        try (var context = start(Map.of(), "dev")) {
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlserver://example.invalid:53735;databaseName=Fixture;");
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("fixture_user");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("fixture-only-password");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("none");
        }
    }

    @Test void explicitEnvironmentOverridesLocalSettings() throws Exception {
        writeSettings();
        try (var context = start(Map.of("SPRING_DATASOURCE_USERNAME", "terminal_user",
                "SPRING_DATASOURCE_PASSWORD", "terminal-only-password", "SPRING_FLYWAY_ENABLED", "true"), "dev")) {
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("terminal_user");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("terminal-only-password");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        }
    }

    @Test void absentLocalFileKeepsDevDefaults() throws Exception {
        try (var context = start(Map.of(), "dev")) {
            assertThat(context.getEnvironment().getProperty("spring.datasource.url")).startsWith("jdbc:sqlserver://127.0.0.1:1433;");
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        }
    }

    @Test void productionDoesNotImportLocalFile() throws Exception {
        writeSettings();
        try (var context = start(Map.of(), "prod")) {
            assertThat(context.getEnvironment().getProperty("SPRING_DATASOURCE_PASSWORD")).isNull();
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        }
    }

    private void writeSettings() throws Exception {
        Files.writeString(root.resolve(".env"), """
                SPRING_DATASOURCE_URL=jdbc:sqlserver://example.invalid:53735;databaseName=Fixture;
                SPRING_DATASOURCE_USERNAME=fixture_user
                SPRING_DATASOURCE_PASSWORD=fixture-only-password
                SPRING_FLYWAY_ENABLED=false
                SPRING_JPA_HIBERNATE_DDL_AUTO=none
                """);
    }

    private ConfigurableApplicationContext start(Map<String, Object> variables, String profile) throws Exception {
        // Use the real configuration in an isolated directory, never the developer's credentials.
        for (String name : new String[]{"application.properties", "application-dev.properties"}) {
            try (var stream = new ClassPathResource(name).getInputStream()) {
                var contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (name.equals("application-dev.properties")) {
                    assertThat(contents).contains("spring.config.import=optional:file:./.env[.properties]");
                    contents = contents.replace("optional:file:./.env[.properties]",
                            "optional:" + root.resolve(".env").toUri() + "[.properties]");
                }
                Files.writeString(root.resolve(name), contents);
            }
        }
        var environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, Map.of()));
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
        return new SpringApplicationBuilder(EmptyConfiguration.class).environment(environment)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .registerShutdownHook(false).run("--spring.config.location=" + root.toUri(),
                        "--spring.profiles.active=" + profile);
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {}
}
