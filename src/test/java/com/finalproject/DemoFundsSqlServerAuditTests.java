package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.*;

/** Opt-in read-only audit of RentalShop; explicit connection settings prevent fallback to a stale database. */
@EnabledIfSystemProperty(named = "demo.funds.live", matches = "true")
class DemoFundsSqlServerAuditTests {
    @Test void migratedBalancesMatchHeldOrderSnapshots() throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        assertThat(url).as("Explicit RentalShop JDBC URL is required")
                .containsIgnoringCase(";databaseName=RentalShop;");
        assertThat(username).as("Explicit SQL login is required").isNotBlank();
        assertThat(password).as("Explicit SQL password is required").isNotBlank();
        try (var connection = DriverManager.getConnection(url, username, password)) {
            connection.setReadOnly(true);
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '10' AND success = 1", Integer.class)).isEqualTo(1);
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
