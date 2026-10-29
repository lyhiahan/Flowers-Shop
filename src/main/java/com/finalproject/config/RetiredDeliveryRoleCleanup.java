package com.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Disables retired delivery accounts without deleting accounts or order history. */
@Component
@org.springframework.context.annotation.Profile("test & !sql-integration")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RetiredDeliveryRoleCleanup implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        jdbc.update("UPDATE users SET is_active = ?, retired_delivery_account = ? WHERE role = ? OR retired_delivery_account = ?",
                false, true, "SHIPPER", true);
        jdbc.update("UPDATE role_requests SET status = ?, rejection_reason = ? WHERE target_role = ? AND status = ?",
                "REJECTED", "Vai trò giao hàng đã được ngừng sử dụng.", "SHIPPER", "PENDING");
    }
}
