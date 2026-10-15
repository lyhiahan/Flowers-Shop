package com.finalproject.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

/** Backfill only verifiable demo receipts; never re-credit an existing store income. */
public class V10__demo_pending_funds extends BaseJavaMigration {
    private record LegacyOrder(long id, long storeId, String status, String paymentStatus, BigDecimal amount, BigDecimal rate) {}
    private record Receipt(String method, String status, BigDecimal amount, String environment, String code) {}
    private record Income(long storeId, BigDecimal amount) {}

    @Override
    public void migrate(Context context) throws Exception {
        var jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        addColumn(jdbc, "stores", "pending_balance", "DECIMAL(18,2) DEFAULT 0 NOT NULL");
        addColumn(jdbc, "orders", "funds_status", "VARCHAR(20) DEFAULT 'NONE' NOT NULL");
        addColumn(jdbc, "orders", "funds_amount", "DECIMAL(18,2) NULL");
        addColumn(jdbc, "orders", "commission_rate_snapshot", "DECIMAL(9,4) NULL");
        addColumn(jdbc, "transactions", "wallet_bucket", "VARCHAR(20) DEFAULT 'AVAILABLE' NOT NULL");
        var orders = jdbc.query("""
                SELECT o.id, o.pickup_store_id, o.status, o.payment_status, o.total_amount,
                       CASE WHEN s.commission_id IS NULL THEN 10 ELSE c.cost END AS rate
                FROM orders o JOIN stores s ON s.id = o.pickup_store_id
                LEFT JOIN commissions c ON c.id = s.commission_id
                WHERE o.payment_method = 'MOMO_DEMO' AND o.funds_status = 'NONE'
                  AND o.payment_status IN ('PAID', 'REFUNDED') ORDER BY o.id
                """, (rs, i) -> new LegacyOrder(rs.getLong("id"), rs.getLong("pickup_store_id"), rs.getString("status"),
                rs.getString("payment_status"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("rate")));
        for (LegacyOrder order : orders) backfill(jdbc, order);
    }

    private void backfill(JdbcTemplate jdbc, LegacyOrder order) {
        var receipts = jdbc.query("SELECT method, status, amount, momo_environment, transaction_code FROM payments WHERE order_id = ?",
                (rs, i) -> new Receipt(rs.getString("method"), rs.getString("status"), rs.getBigDecimal("amount"),
                        rs.getString("momo_environment"), rs.getString("transaction_code")), order.id());
        if (order.amount() == null || order.amount().signum() <= 0 || receipts.size() != 1) {
            review(jdbc, order.id()); return;
        }
        Receipt receipt = receipts.getFirst();
        boolean refunded = "REFUNDED".equals(order.paymentStatus());
        if (!refunded && jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE order_id = ? AND type = 'REFUND'",
                Integer.class, order.id()) > 0) {
            review(jdbc, order.id()); return;
        }
        if (!"MOMO_DEMO".equals(receipt.method()) || !(refunded ? "REFUNDED" : "SUCCESS").equals(receipt.status())
                || receipt.amount() == null || order.amount().compareTo(receipt.amount()) != 0
                || !"demo".equals(receipt.environment()) || !("DEMO-MOMO-" + order.id()).equals(receipt.code())) {
            review(jdbc, order.id()); return;
        }
        var incomes = jdbc.query("""
                SELECT store_id, amount FROM transactions WHERE order_id = ?
                  AND type IN ('ORDER_PAYMENT', 'ORDER_PAYOUT') AND status = 'COMPLETED' AND is_up = ?
                """, (rs, i) -> new Income(rs.getLong("store_id"), rs.getBigDecimal("amount")), order.id(), true);
        if (incomes.size() > 1 || (!incomes.isEmpty() && (incomes.getFirst().storeId() != order.storeId()
                || incomes.getFirst().amount() == null || incomes.getFirst().amount().signum() < 0
                || incomes.getFirst().amount().compareTo(order.amount()) > 0))) {
            review(jdbc, order.id()); return;
        }
        if (refunded) {
            snapshot(jdbc, order, "REFUNDED", validRate(order.rate()) ? order.rate() : BigDecimal.ZERO);
            return;
        }
        if (!incomes.isEmpty()) {
            BigDecimal historicalRate = BigDecimal.ONE.subtract(incomes.getFirst().amount().divide(order.amount(), 12, RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
            snapshot(jdbc, order, "SETTLED", historicalRate);
            return;
        }
        if (!validRate(order.rate()) || !Set.of("PENDING", "CONFIRMED", "PREPARING", "DELIVERING", "PICKED_UP", "REFUND_REQUESTED").contains(order.status())) {
            review(jdbc, order.id()); return;
        }
        // Existing PAID orders without recognized income become held, not available to withdraw.
        snapshot(jdbc, order, "HELD", order.rate());
        jdbc.update("UPDATE stores SET pending_balance = pending_balance + ? WHERE id = ?", order.amount(), order.storeId());
        jdbc.update("""
                INSERT INTO transactions (store_id, order_id, amount, is_up, type, status, wallet_bucket, request_key, description, created_at)
                VALUES (?, ?, ?, ?, 'PAYMENT_HOLD', 'COMPLETED', 'PENDING', ?, ?, ?)
                """, order.storeId(), order.id(), order.amount(), true, "demo-funds:PAYMENT_HOLD:" + order.id(),
                "Doi soat tien cho don MoMo demo #" + order.id(), Timestamp.valueOf(LocalDateTime.now()));
    }

    private void snapshot(JdbcTemplate jdbc, LegacyOrder order, String status, BigDecimal rate) {
        jdbc.update("UPDATE orders SET funds_status = ?, funds_amount = ?, commission_rate_snapshot = ? WHERE id = ?",
                status, order.amount(), rate, order.id());
    }

    private void review(JdbcTemplate jdbc, long id) {
        jdbc.update("UPDATE orders SET funds_status = 'REVIEW_REQUIRED' WHERE id = ?", id);
    }

    private boolean validRate(BigDecimal rate) {
        return rate != null && rate.signum() >= 0 && rate.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    private void addColumn(JdbcTemplate jdbc, String table, String column, String definition) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?
                """, Integer.class, table, column);
        if (count != null && count == 0) jdbc.execute("ALTER TABLE " + table + " ADD " + column + " " + definition);
    }
}
