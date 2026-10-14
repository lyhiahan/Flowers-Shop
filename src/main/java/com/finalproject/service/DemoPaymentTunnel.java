package com.finalproject.service;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class DemoPaymentTunnel {
    public record Connection(String state, String baseUrl, String message) {}
    private record TunnelEvent(String state, String code, String url) {}
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DemoPaymentTunnel.class);
    private final Environment environment;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private volatile Connection connection = new Connection("STARTING", null, "Đang bật kết nối điện thoại…");
    private volatile Process process;
    private ExecutorService worker;
    private boolean started;
    private volatile boolean closed;

    public DemoPaymentTunnel(Environment environment) { this.environment = environment; }

    public boolean automatic() {
        return environment.matchesProfiles("dev") && !environment.matchesProfiles("prod", "test")
                && environment.getProperty("app.demo-payment.enabled", Boolean.class, false)
                && environment.getProperty("app.demo-payment.auto-tunnel.enabled", Boolean.class, false);
    }

    public Connection connection() {
        if (!automatic()) return new Connection("DISABLED", null, "");
        Process owned = process;
        if (owned != null && !owned.isAlive()) return new Connection("ERROR", null, "Kết nối điện thoại đã dừng. Vui lòng chạy lại website.");
        return connection;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (started || closed || !automatic()) return;
        started = true;
        Path root = Path.of(environment.getProperty("app.demo-payment.auto-tunnel.project-dir", System.getProperty("user.dir"))).toAbsolutePath().normalize();
        Path script = root.resolve("scripts/demo-phone-tunnel.cjs");
        Path binary = root.resolve(environment.getProperty("app.demo-payment.auto-tunnel.cloudflared", ".tools/cloudflared.exe")).normalize();
        int port = environment.getProperty("local.server.port", Integer.class, 0);
        if (!Files.isRegularFile(script) || !Files.isRegularFile(binary) || port < 1 || port > 65535
                || !environment.getProperty("server.servlet.context-path", "").isBlank()) {
            fail("Thiếu chương trình kết nối hoặc cấu hình cổng không hợp lệ. Kiểm tra .tools/cloudflared.exe và thư mục scripts.");
            return;
        }
        try {
            var builder = new ProcessBuilder(environment.getProperty("app.demo-payment.auto-tunnel.node-command", "node"),
                    script.toString(), "--app-port", Integer.toString(port), "--cloudflared", binary.toString())
                    .directory(root.toFile()).redirectErrorStream(true);
            // Child tools need OS paths, not database passwords, mail keys or payment secrets.
            var osEnvironment = new HashMap<>(builder.environment());
            builder.environment().clear();
            for (var entry : osEnvironment.entrySet()) {
                if (Set.of("PATH", "SYSTEMROOT", "WINDIR", "COMSPEC", "TEMP", "TMP", "USERPROFILE", "HOME", "APPDATA", "LOCALAPPDATA")
                        .contains(entry.getKey().toUpperCase(Locale.ROOT))) builder.environment().put(entry.getKey(), entry.getValue());
            }
            process = launch(builder);
            worker = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "demo-phone-tunnel"); thread.setDaemon(true); return thread;
            });
            worker.submit(this::readEvents);
        } catch (IOException ex) {
            fail("Không bật được kết nối điện thoại. Kiểm tra Node.js và cloudflared trên máy tính.");
        }
    }

    protected Process launch(ProcessBuilder builder) throws IOException { return builder.start(); }

    private void readEvents() {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) acceptEvent(line);
        } catch (IOException ex) {
            if (!closed) fail("Kết nối điện thoại bị gián đoạn. Vui lòng chạy lại website.");
        } finally {
            if (!closed) fail("Kết nối điện thoại đã dừng. Vui lòng chạy lại website.");
        }
    }

    void acceptEvent(String line) {
        try {
            TunnelEvent event = mapper.readValue(line, TunnelEvent.class);
            if (closed) return;
            if ("READY".equals(event.state())) {
                URI url = URI.create(event.url());
                if (!"https".equals(url.getScheme()) || url.getHost() == null
                        || !url.getHost().matches("[a-z0-9]+(?:-[a-z0-9]+)*\\.trycloudflare\\.com")
                        || url.getRawAuthority() == null || !url.getRawAuthority().equals(url.getHost())
                        || !url.getRawPath().isEmpty() || url.getRawQuery() != null || url.getRawFragment() != null) return;
                connection = new Connection("READY", url.toString(), "");
                log.info("Demo phone payment ready: {} (only phone payment routes are public)", url);
            } else if ("STARTING".equals(event.state())) {
                connection = new Connection("STARTING", null, "Đang bật kết nối điện thoại…");
            } else if ("ERROR".equals(event.state())) {
                connection = new Connection("ERROR", null, "Chưa kết nối được Internet cho điện thoại. Hệ thống đang thử lại…");
                log.warn("Demo phone connection unavailable: {}", event.code());
            }
        } catch (RuntimeException ignored) {
            // Only the supervisor's structured messages may change the advertised URL.
        }
    }

    private void fail(String message) {
        connection = new Connection("ERROR", null, message);
        log.warn("Demo phone tunnel: {}", message);
    }

    @PreDestroy
    public synchronized void stop() {
        closed = true;
        connection = new Connection("STOPPED", null, "Kết nối điện thoại đã dừng.");
        if (process != null && process.isAlive()) {
            try {
                process.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().flush();
                process.getOutputStream().close();
                if (!process.waitFor(7, TimeUnit.SECONDS)) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }
            } catch (IOException | InterruptedException ex) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
        if (worker != null) worker.shutdownNow();
    }
}
