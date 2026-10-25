package com.finalproject;

import com.finalproject.tools.AivenLocalSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AivenLocalSetupTests {
    @TempDir Path directory;

    Properties read(Path path) throws Exception {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
        return properties;
    }

    Properties settings() {
        var properties = new Properties();
        properties.setProperty("AIVEN_PASSWORD", "fixture-only=:#\\password");
        properties.setProperty("AIVEN_CA_CERT", "C:/example/ca.pem");
        properties.setProperty("SERVER_PORT", "8080");
        return properties;
    }

    @Test void setupEscapesPasswordAndGeneratesPersistentRandomKey() throws Exception {
        Path file = directory.resolve(".env.aiven");
        AivenLocalSetup.writeConfiguration(file, settings());
        var first = read(file);
        assertThat(first.getProperty("AIVEN_PASSWORD").equals(settings().getProperty("AIVEN_PASSWORD"))).isTrue();
        assertThat(Base64.getDecoder().decode(first.getProperty("JWT_SECRET"))).hasSize(32);
        assertThat(first.getProperty("SERVER_ADDRESS")).isEqualTo("127.0.0.1");
        AivenLocalSetup.writeConfiguration(file, settings());
        assertThat(read(file).getProperty("JWT_SECRET").equals(first.getProperty("JWT_SECRET"))).isTrue();
        Path secondFile = directory.resolve("second.properties");
        AivenLocalSetup.writeConfiguration(secondFile, settings());
        assertThat(read(secondFile).getProperty("JWT_SECRET").equals(first.getProperty("JWT_SECRET"))).isFalse();
    }

    @Test void setupPreservesOtherLocalSettings() throws Exception {
        Path file = directory.resolve(".env.aiven");
        Files.writeString(file, "MAIL_PROVIDER=custom\nBREVO_API_KEY=fixture-only\n");
        AivenLocalSetup.writeConfiguration(file, settings());
        assertThat(read(file).getProperty("MAIL_PROVIDER")).isEqualTo("custom");
        assertThat(read(file).getProperty("BREVO_API_KEY")).isEqualTo("fixture-only");
    }

    @Test void invalidExistingKeyDoesNotOverwriteConfiguration() throws Exception {
        Path file = directory.resolve(".env.aiven");
        String original = "JWT_SECRET=invalid!\n";
        Files.writeString(file, original);
        assertThatThrownBy(() -> AivenLocalSetup.writeConfiguration(file, settings()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.readString(file)).isEqualTo(original);
    }

    @Test void missingPasswordDoesNotCreateConfiguration() {
        Path file = directory.resolve(".env.aiven");
        assertThatThrownBy(() -> AivenLocalSetup.writeConfiguration(file, new Properties()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(file).doesNotExist();
    }

    @Test void privateConfigurationIsReadableOnlyByItsOwner() throws Exception {
        Path file = directory.resolve(".env.aiven");
        AivenLocalSetup.writeConfiguration(file, settings());
        var acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (acl != null) {
            var owner = Files.getOwner(file);
            assertThat(acl.getAcl()).allSatisfy(entry -> assertThat(entry.principal()).isEqualTo(owner));
        } else {
            assertThat(Files.getPosixFilePermissions(file)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        }
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly(".env.aiven");
        }
    }
}
