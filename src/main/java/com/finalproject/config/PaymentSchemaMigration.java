package com.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Production uses ddl-auto=none: install additive columns before reading mapped entities. */
@Component @Order(2) @RequiredArgsConstructor
@org.springframework.context.annotation.Profile("test & !sql-integration")
public class PaymentSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        String database = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                connection -> connection.getMetaData().getDatabaseProductName());
        if (database == null || !database.contains("Microsoft SQL Server")) return;
        jdbc.execute("IF COL_LENGTH('orders','payment_expires_at') IS NULL ALTER TABLE orders ADD payment_expires_at DATETIME2 NULL");
        jdbc.execute("IF COL_LENGTH('orders','payment_expired') IS NULL ALTER TABLE orders ADD payment_expired BIT NULL");
        jdbc.execute("IF COL_LENGTH('transactions','earned_points') IS NULL ALTER TABLE transactions ADD earned_points INT NULL");
    }
}
