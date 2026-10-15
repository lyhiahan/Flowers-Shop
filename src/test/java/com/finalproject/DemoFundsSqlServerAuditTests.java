package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

/** Opt-in read-only audit of the configured development database; no business rows are changed. */
@EnabledIfSystemProperty(named = "demo.funds.live", matches = "true")
class DemoFundsSqlServerAuditTests {
    @Test void migratedBalancesMatchHeldOrderSnapshots() throws Exception {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-dev.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new PropertiesPropertySource("development", properties));
        String url = environment.getRequiredProperty("spring.datasource.url");
        try (var connection = DriverManager.getConnection(url, environment.getRequiredProperty("spring.datasource.username"),
                environment.getRequiredProperty("spring.datasource.password"))) {
            connection.setReadOnly(true);
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '6' AND success = 1", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM stores s
                    WHERE s.pending_balance < 0 OR s.pending_balance <> COALESCE(
                      (SELECT SUM(o.funds_amount) FROM orders o WHERE o.pickup_store_id = s.id AND o.funds_status = 'HELD'), 0)
                    """, Integer.class)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM orders o WHERE o.funds_status = 'HELD'
                    AND (o.payment_method <> 'MOMO_DEMO' OR o.payment_status <> 'PAID'
                      OR o.funds_amount IS NULL OR o.funds_amount <> o.total_amount
                      OR o.commission_rate_snapshot IS NULL OR o.commission_rate_snapshot < 0 OR o.commission_rate_snapshot > 100)
                    """, Integer.class)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM transactions WHERE type IN ('PAYMENT_HOLD', 'HOLD_RELEASE') AND wallet_bucket <> 'PENDING'
                    """, Integer.class)).isZero();
        }
    }
}
