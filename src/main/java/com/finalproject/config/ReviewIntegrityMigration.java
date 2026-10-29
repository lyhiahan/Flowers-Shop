package com.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keep legacy reviews without an order, while enforcing one review per purchased item/order. */
@Component @Order(31) @RequiredArgsConstructor
@org.springframework.context.annotation.Profile("test & !sql-integration")
public class ReviewIntegrityMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        String database = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                connection -> connection.getMetaData().getDatabaseProductName());
        if (database == null || !(database.contains("Microsoft SQL Server") || database.equals("H2"))) return;
        Integer duplicates = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (SELECT user_id, product_id, order_id FROM reviews
                WHERE order_id IS NOT NULL GROUP BY user_id, product_id, order_id HAVING COUNT(*) > 1) conflicts
                """, Integer.class);
        if (duplicates != null && duplicates > 0)
            throw new IllegalStateException("Có đánh giá trùng đơn hàng. Cần rà soát dữ liệu trước khi tạo ràng buộc; không tự xóa lịch sử.");
        if (database.equals("H2")) {
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS UK_review_user_product_order ON reviews(user_id, product_id, order_id)");
        } else {
            jdbc.execute("""
                    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('reviews')
                        AND name='UK_review_user_product_order')
                    CREATE UNIQUE INDEX UK_review_user_product_order ON reviews(user_id, product_id, order_id)
                        WHERE order_id IS NOT NULL
                    """);
            jdbc.execute("""
                    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('orders') AND name='IX_order_payment_expiry')
                    CREATE INDEX IX_order_payment_expiry ON orders(payment_status, status, payment_expires_at)
                    """);
        }
    }
}
