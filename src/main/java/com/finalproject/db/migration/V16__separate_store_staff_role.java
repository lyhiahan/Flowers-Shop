package com.finalproject.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Restore least-privilege staff accounts without changing stores, locks or business history. */
public class V16__separate_store_staff_role extends BaseJavaMigration {
    @Override
    public void migrate(Context context) {
        separate(new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true)));
    }

    public void separate(JdbcTemplate jdbc) {
        boolean sqlServer = Boolean.TRUE.equals(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)
                c -> c.getMetaData().getDatabaseProductName().contains("Microsoft SQL Server")));
        if (sqlServer) {
            var checks = jdbc.queryForList("""
                    SELECT SCHEMA_NAME(t.schema_id) AS schema_name, t.name AS table_name, cc.name AS constraint_name
                    FROM sys.check_constraints cc JOIN sys.tables t ON t.object_id=cc.parent_object_id
                    WHERE t.name IN ('users','user_roles','role_requests')
                      AND (cc.name LIKE '%canonical_role' OR cc.definition LIKE '%''MANAGER''%')
                    """);
            for (var check : checks) jdbc.execute("ALTER TABLE " + identifier(check.get("schema_name")) + "."
                    + identifier(check.get("table_name")) + " DROP CONSTRAINT " + identifier(check.get("constraint_name")));
        }
        // Preserve admins and every store's designated manager. Other assigned MANAGER accounts
        // came from the old staff/partner flow and must no longer administer the branch.
        jdbc.update("""
                UPDATE users SET role='STAFF'
                WHERE role='MANAGER'
                  AND NOT EXISTS (SELECT 1 FROM user_roles r WHERE r.user_id=users.id AND r.role='ADMIN')
                  AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.owner_id=users.id)
                  AND (store_id IS NOT NULL OR EXISTS (SELECT 1 FROM store_memberships m WHERE m.user_id=users.id))
                """);
        jdbc.update("DELETE FROM user_roles WHERE role IN ('MANAGER','USER') AND user_id IN (SELECT id FROM users WHERE role='STAFF')");
        jdbc.update("""
                INSERT INTO user_roles (user_id,role)
                SELECT u.id,'STAFF' FROM users u WHERE u.role='STAFF'
                  AND NOT EXISTS (SELECT 1 FROM user_roles r WHERE r.user_id=u.id AND r.role='STAFF')
                """);
        jdbc.update("""
                UPDATE store_memberships SET access_role='STAFF'
                WHERE user_id IN (SELECT id FROM users WHERE role='STAFF')
                """);
        if (sqlServer) {
            for (String table : java.util.List.of("users", "user_roles", "role_requests")) {
                String column = table.equals("role_requests") ? "target_role" : "role";
                String values = table.equals("user_roles") ? "'ADMIN','MANAGER','STAFF','USER'" : "'ADMIN','MANAGER','STAFF','USER','SHIPPER'";
                for (String schema : jdbc.queryForList("SELECT SCHEMA_NAME(schema_id) FROM sys.tables WHERE name=?", String.class, table)) {
                    jdbc.execute("ALTER TABLE " + identifier(schema) + "." + identifier(table)
                            + " WITH CHECK ADD CONSTRAINT " + identifier("CK_" + table + "_canonical_role")
                            + " CHECK (" + identifier(column) + " IN (" + values + "))");
                }
            }
        }
    }

    private static String identifier(Object value) { return "[" + value.toString().replace("]", "]]") + "]"; }
}
