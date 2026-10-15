package com.finalproject.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class DemoPaymentTunnelTests {
    @TempDir Path root;

    MockEnvironment environment(String... profiles) {
        var environment = new MockEnvironment().withProperty("app.demo-payment.enabled", "true")
                .withProperty("app.demo-payment.auto-tunnel.enabled", "true")
                .withProperty("app.demo-payment.auto-tunnel.project-dir", root.toString())
                .withProperty("local.server.port", "9080");
        environment.setActiveProfiles(profiles);
        return environment;
    }

    @Test void testAndProductionNeverLaunchEvenWhenBothFlagsAreEnabled() {
        for (String profile : new String[]{"test", "prod"}) {
            var service = new DemoPaymentTunnel(environment("dev", profile)) {
                @Override protected Process launch(ProcessBuilder builder) { throw new AssertionError("Must not launch"); }
            };
            assertThat(service.automatic()).isFalse();
            service.start();
            assertThat(service.connection().state()).isEqualTo("DISABLED");
            service.stop();
        }
    }

    @Test void explicitOptOutAndDisabledDemoDoNotLaunch() {
        var environment = environment("dev");
        environment.setProperty("app.demo-payment.auto-tunnel.enabled", "false");
        assertThat(new DemoPaymentTunnel(environment).automatic()).isFalse();
        environment.setProperty("app.demo-payment.auto-tunnel.enabled", "true");
        environment.setProperty("app.demo-payment.enabled", "false");
        assertThat(new DemoPaymentTunnel(environment).automatic()).isFalse();
    }

    @Test void onlyValidatedSupervisorOriginsCanBeAdvertisedAndAnErrorRevokesThem() {
        var service = new DemoPaymentTunnel(environment("dev"));
        for (String url : new String[]{"http://phone.trycloudflare.com", "https://phone.trycloudflare.com/admin",
                "https://phone.trycloudflare.com.evil.test", "https://user@phone.trycloudflare.com", "https://phone.trycloudflare.com:443",
                "https://phone.trycloudflare.com?next=admin", "https://phone.trycloudflare.com#x"}) {
            service.acceptEvent("{\"state\":\"READY\",\"url\":\"" + url + "\"}");
            assertThat(service.connection().baseUrl()).isNull();
        }
        service.acceptEvent("not json");
        service.acceptEvent("{\"state\":\"READY\",\"url\":\"https://phone.trycloudflare.com\"}");
        assertThat(service.connection().baseUrl()).isEqualTo("https://phone.trycloudflare.com");
        service.acceptEvent("{\"state\":\"ERROR\",\"code\":\"TUNNEL_EXPIRED\"}");
        assertThat(service.connection().baseUrl()).isNull();
        assertThat(service.connection().state()).isEqualTo("ERROR");
        service.stop();
    }

    @Test void missingToolsProduceAnActionableErrorWithoutFailingWebsiteStartup() {
        var service = new DemoPaymentTunnel(environment("dev"));
        service.start();
        assertThat(service.connection().state()).isEqualTo("ERROR");
        assertThat(service.connection().message()).contains("cloudflared.exe");
        assertThat(service.connection().baseUrl()).isNull();
        service.stop();
    }

    @Test void repeatedReadyEventsLaunchOnceWithActualPortAndStopOnlyTheOwnedProcess() throws Exception {
        Files.createDirectories(root.resolve("scripts")); Files.createDirectories(root.resolve(".tools"));
        Files.createFile(root.resolve("scripts/demo-phone-tunnel.cjs")); Files.createFile(root.resolve(".tools/cloudflared.exe"));
        var owned = new FakeProcess();
        var launches = new AtomicInteger();
        var service = new DemoPaymentTunnel(environment("dev")) {
            @Override protected Process launch(ProcessBuilder builder) {
                launches.incrementAndGet();
                assertThat(builder.command()).containsSubsequence("--app-port", "9080");
                assertThat(builder.environment().keySet()).noneMatch(key -> key.startsWith("SPRING_") || key.contains("API_KEY"));
                return owned;
            }
        };
        try {
            service.start(); service.start();
            assertThat(launches.get()).isEqualTo(1);
        } finally { service.stop(); }
        assertThat(owned.commands.toString(StandardCharsets.UTF_8)).contains("stop\n");
        assertThat(owned.isAlive()).isFalse();
        service.start();
        assertThat(launches.get()).isEqualTo(1);
    }

    private static class FakeProcess extends Process {
        final ByteArrayOutputStream commands = new ByteArrayOutputStream();
        final PipedInputStream input = new PipedInputStream();
        final PipedOutputStream events;
        boolean alive = true;
        FakeProcess() throws IOException { events = new PipedOutputStream(input); }
        @Override public OutputStream getOutputStream() {
            return new FilterOutputStream(commands) {
                @Override public void close() throws IOException { alive = false; events.close(); }
            };
        }
        @Override public InputStream getInputStream() { return input; }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() { return 0; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) { return !alive; }
        @Override public int exitValue() { if (alive) throw new IllegalThreadStateException(); return 0; }
        @Override public void destroy() { alive = false; }
        @Override public boolean isAlive() { return alive; }
    }
}
