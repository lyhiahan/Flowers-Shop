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
@org.springframework.context.annotation.Profile("test & !sql-integration")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RoleConsolidationMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        boolean sqlServer = Boolean.TRUE.equals(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)
                connection -> connection.getMetaData().getDatabaseProductName().contains("Microsoft SQL Server")));
        if (sqlServer) removeLegacyRoleChecks();

        jdbc.update("UPDATE users SET role = 'USER' WHERE role = 'CUSTOMER'");
        jdbc.update("UPDATE users SET role = 'MANAGER' WHERE role IN ('VENDOR', 'STAFF')");
        // Insert the merged authority before deleting legacy authorities. Avoid composite-key collisions.
        jdbc.update("INSERT INTO user_roles (user_id, role) " +
                "SELECT DISTINCT legacy.user_id, CASE WHEN legacy.role = 'CUSTOMER' THEN 'USER' ELSE 'MANAGER' END " +
                "FROM user_roles legacy WHERE legacy.role IN ('CUSTOMER', 'VENDOR', 'STAFF') " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles assigned_role WHERE assigned_role.user_id = legacy.user_id " +
                "AND assigned_role.role = CASE WHEN legacy.role = 'CUSTOMER' THEN 'USER' ELSE 'MANAGER' END)");
        jdbc.update("DELETE FROM user_roles WHERE role IN ('CUSTOMER', 'VENDOR', 'STAFF')");
        jdbc.update("DELETE FROM user_roles WHERE role = 'SHIPPER'");
        jdbc.update("UPDATE role_requests SET target_role = 'USER' WHERE target_role = 'CUSTOMER'");
        jdbc.update("UPDATE role_requests SET target_role = 'MANAGER' WHERE target_role IN ('VENDOR', 'STAFF')");
        // Membership is still scoped to the original store. Owner-only operations stay owner-only.
        jdbc.update("UPDATE store_memberships SET access_role = 'PARTNER' WHERE access_role IN ('STAFF', 'MANAGER')");
        if (sqlServer) restoreRoleChecks();
    }

    private void removeLegacyRoleChecks() {
        var checks = jdbc.queryForList("""
                SELECT SCHEMA_NAME(t.schema_id) AS schema_name, t.name AS table_name, cc.name AS constraint_name
                FROM sys.check_constraints cc JOIN sys.tables t ON t.object_id = cc.parent_object_id
                WHERE t.name IN ('users','user_roles','role_requests')
                  AND (cc.definition LIKE '%''CUSTOMER''%' OR cc.definition LIKE '%''VENDOR''%' OR cc.definition LIKE '%''STAFF''%')
                  AND EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=t.object_id
                    AND c.name IN ('role','target_role') AND (cc.parent_column_id=c.column_id
                      OR EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d
                        WHERE d.referencing_id=cc.object_id AND d.referenced_id=t.object_id AND d.referenced_minor_id=c.column_id)))
                """);
        for (var check : checks) jdbc.execute("ALTER TABLE " + identifier(check.get("schema_name")) + "."
                + identifier(check.get("table_name")) + " DROP CONSTRAINT " + identifier(check.get("constraint_name")));
    }

    private void restoreRoleChecks() {
        for (String table : java.util.List.of("users", "user_roles", "role_requests")) {
            String column = table.equals("role_requests") ? "target_role" : "role";
            // SHIPPER is retained only as an immutable historical marker; it grants no role in Java.
            String values = table.equals("user_roles") ? "'ADMIN','MANAGER','USER'" : "'ADMIN','MANAGER','USER','SHIPPER'";
            String name = "CK_" + table + "_canonical_role";
            var schemas = jdbc.queryForList("SELECT SCHEMA_NAME(schema_id) FROM sys.tables WHERE name=?", String.class, table);
            for (String schema : schemas) {
                Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM sys.check_constraints WHERE name=? AND parent_object_id=OBJECT_ID(?)",
                        Integer.class, name, schema + "." + table);
                if (exists != null && exists == 0) jdbc.execute("ALTER TABLE " + identifier(schema) + "." + identifier(table)
                        + " WITH CHECK ADD CONSTRAINT " + identifier(name) + " CHECK (" + identifier(column) + " IN (" + values + "))");
            }
        }
    }

    private static String identifier(Object value) { return "[" + value.toString().replace("]", "]]") + "]"; }
}
