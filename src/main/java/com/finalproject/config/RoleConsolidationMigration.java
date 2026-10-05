package com.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent permission migration. Accounts, status, ownership and business history are preserved. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RoleConsolidationMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        jdbc.update("UPDATE users SET role = 'USER' WHERE role = 'CUSTOMER'");
        jdbc.update("UPDATE users SET role = 'MANAGER' WHERE role IN ('VENDOR', 'STAFF')");
        // Insert the merged authority before deleting legacy authorities. Avoid composite-key collisions.
        jdbc.update("INSERT INTO user_roles (user_id, role) " +
                "SELECT DISTINCT legacy.user_id, CASE WHEN legacy.role = 'CUSTOMER' THEN 'USER' ELSE 'MANAGER' END " +
                "FROM user_roles legacy WHERE legacy.role IN ('CUSTOMER', 'VENDOR', 'STAFF') " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles assigned_role WHERE assigned_role.user_id = legacy.user_id " +
                "AND assigned_role.role = CASE WHEN legacy.role = 'CUSTOMER' THEN 'USER' ELSE 'MANAGER' END)");
        jdbc.update("DELETE FROM user_roles WHERE role IN ('CUSTOMER', 'VENDOR', 'STAFF')");
        jdbc.update("UPDATE role_requests SET target_role = 'USER' WHERE target_role = 'CUSTOMER'");
        jdbc.update("UPDATE role_requests SET target_role = 'MANAGER' WHERE target_role IN ('VENDOR', 'STAFF')");
        // Membership is still scoped to the original store. Owner-only operations stay owner-only.
        jdbc.update("UPDATE store_memberships SET access_role = 'PARTNER' WHERE access_role IN ('STAFF', 'MANAGER')");
    }
}
