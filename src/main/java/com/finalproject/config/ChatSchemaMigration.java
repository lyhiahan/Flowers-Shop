package com.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

/** Chat must install its schema explicitly because production uses ddl-auto=none. */
@Component
@org.springframework.context.annotation.Profile("test")
@Order(3)
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ChatSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws java.io.IOException {
        String database = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                connection -> connection.getMetaData().getDatabaseProductName());
        if (database == null || !database.contains("Microsoft SQL Server")) return;
        try (var stream = new ClassPathResource("db/chat-schema.sql").getInputStream()) {
            // Execute the entire batch: splitting on semicolons would break IF/BEGIN/END blocks.
            jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        log.info("Đã cập nhật schema chat; giữ nguyên dữ liệu hội thoại hiện có.");
    }
}
