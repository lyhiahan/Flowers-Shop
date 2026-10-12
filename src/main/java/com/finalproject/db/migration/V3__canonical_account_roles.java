package com.finalproject.db.migration;

import com.finalproject.config.RetiredDeliveryRoleCleanup;
import com.finalproject.config.RoleConsolidationMigration;
import org.flywaydb.core.api.migration.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Runs before Hibernate starts, using Flyway's transaction and history table. */
public class V3__canonical_account_roles extends BaseJavaMigration {
    @Override
    public void migrate(Context context) {
        var jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        new RetiredDeliveryRoleCleanup(jdbc).run(null);
        new RoleConsolidationMigration(jdbc).run(null);
        jdbc.update("UPDATE users SET role='ADMIN' WHERE id IN (SELECT user_id FROM user_roles WHERE role='ADMIN')");
        jdbc.update("DELETE FROM user_roles WHERE role <> 'ADMIN' AND user_id IN (SELECT id FROM users WHERE role='ADMIN')");
    }
}
