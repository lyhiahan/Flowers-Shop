package com.finalproject.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;

/** One-time local configuration; never logs credentials or changes database data. */
public final class AivenLocalSetup {
    private AivenLocalSetup() {}

    public static void main(String[] args) {
        try {
            var settings = new Properties();
            for (String key : List.of("AIVEN_HOST", "AIVEN_PORT", "AIVEN_DATABASE", "AIVEN_USER",
                    "AIVEN_PASSWORD", "AIVEN_CA_CERT", "SERVER_PORT")) {
                String value = System.getenv(key);
                if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
                settings.setProperty(key, value);
            }
            writeConfiguration(Path.of(".env.aiven"), settings);
            System.out.println("Local Aiven configuration ready. Credentials were not printed. Run: mvn spring-boot:run");
        } catch (Exception failure) {
            System.err.println("Could not save local configuration. Check file permissions and retry; no database data was changed.");
            System.exit(1);
        }
    }

    public static void writeConfiguration(Path destination, Properties settings) throws IOException {
        if (settings.getProperty("AIVEN_PASSWORD", "").isBlank()) {
            throw new IllegalArgumentException("An Aiven password is required.");
        }
        destination = destination.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(destination)) throw new IOException("Configuration must not be a symbolic link.");
        var configuration = new Properties();
        if (Files.exists(destination)) {
            try (var reader = Files.newBufferedReader(destination, StandardCharsets.UTF_8)) {
                configuration.load(reader);
            }
        }
        configuration.putAll(settings);
        configuration.setProperty("SERVER_ADDRESS", "127.0.0.1");
        if (configuration.getProperty("JWT_SECRET", "").isBlank()) {
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            configuration.setProperty("JWT_SECRET", Base64.getEncoder().encodeToString(secret));
            Arrays.fill(secret, (byte) 0);
        } else if (Base64.getDecoder().decode(configuration.getProperty("JWT_SECRET")).length < 32) {
            throw new IllegalArgumentException("Existing JWT key is too short.");
        }
        Path temporary = Files.createTempFile(destination.getParent(), ".aiven-setup-", ".tmp");
        try {
            // Restrict the empty file before writing any secret, then atomically replace it.
            var acl = Files.getFileAttributeView(temporary, AclFileAttributeView.class);
            if (acl != null) {
                var owner = Files.getOwner(temporary);
                acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                        .setPrincipal(owner).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
            } else {
                Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
            }
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                configuration.store(writer, "Private local configuration. Never commit or share this file.");
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
