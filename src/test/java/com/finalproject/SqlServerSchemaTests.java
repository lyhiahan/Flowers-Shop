package com.finalproject;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Generate SQL Server DDL from all mapped entities without connecting to a real DB. */
class SqlServerSchemaTests {
    @Test
    void migrationsCoverEveryMappedTable() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", false);
        settings.put("hibernate.physical_naming_strategy", new PhysicalNamingStrategySnakeCaseImpl());
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        boolean export = Boolean.getBoolean("app.export-schema");
        if (export) {
            settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
            settings.put("jakarta.persistence.schema-generation.scripts.create-target", ".tools/sqlserver-schema.sql");
        }
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var sources = new MetadataSources(registry);
            for (var bean : scanner.findCandidateComponents("com.finalproject.entity"))
                sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            var metadata = sources.buildMetadata();
            if (export) {
                SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> {});
            } else {
                String baseline = Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V1__initial_schema.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V5__chat_monitor_access_audit.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V6__chat_product_context.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V7__product_categories.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V8__branch_catalog_visibility.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V17__customer_demo_wallet.sql"))
                        + Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V19__order_aftercare.sql"));
                String pendingFunds = Files.readString(Path.of("src/main/java/com/finalproject/db/migration/V10__demo_pending_funds.java"));
                for (var table : metadata.collectTableMappings()) {
                    var pattern = java.util.regex.Pattern.compile("create\\s+table\\s+(?:dbo\\.)?"
                            + java.util.regex.Pattern.quote(table.getName()) + "\\s*\\((.*?)\\);",
                            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
                    var match = pattern.matcher(baseline);
                    assertThat(match.find()).as("table %s", table.getName()).isTrue();
                    String definitions = match.group(1);
                    var additions = java.util.regex.Pattern.compile("alter\\s+table\\s+(?:dbo\\.)?"
                            + java.util.regex.Pattern.quote(table.getName()) + "\\s+add\\s+(.*?);",
                            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(baseline);
                    while (additions.find()) definitions += additions.group(1);
                    for (var column : table.getColumns()) {
                        if (!definitions.contains(column.getName() + " ")) {
                            assertThat(pendingFunds).as("additive migration for %s.%s", table.getName(), column.getName())
                                    .contains("addColumn(jdbc, \"" + table.getName() + "\", \"" + column.getName() + "\"");
                        }
                    }
                }
                assertThat(baseline.toLowerCase(Locale.ROOT)).doesNotContain("drop table", "drop database", "truncate table");
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
