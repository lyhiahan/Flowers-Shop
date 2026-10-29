package com.finalproject;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Opt-in only: caller provisions disposable SQL Server databases, never RentalShop itself. */
@EnabledIfSystemProperty(named = "sqlserver.live", matches = "true")
class SqlServerMigrationLiveTests {
    private String url(String variable) {
        String name = System.getenv(variable);
        if (name == null || !name.matches("CodexRentalShop(?:Fresh|Copy)_[0-9_]+"))
            throw new IllegalArgumentException("Provision a disposable SQL Server test database: " + variable);
        String base = Objects.requireNonNull(System.getenv("SPRING_DATASOURCE_URL"));
        if (!base.matches("(?is).*;databaseName=[^;]+;.*")) throw new IllegalArgumentException("Missing databaseName in test URL");
        return base.replaceFirst("(?i)(;databaseName=)[^;]+", "$1" + name);
    }
    private Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
    }
    private Flyway flyway(String url) {
        return Flyway.configure().dataSource(url, System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"))
                .defaultSchema("dbo").locations("classpath:db/migration/sqlserver", "classpath:com/finalproject/db/migration")
                .baselineVersion("1").baselineOnMigrate(false).cleanDisabled(true).load();
    }
    private Map<String, Long> counts(String url) throws SQLException {
        Map<String, Long> result = new LinkedHashMap<>();
        try (var connection = connect(url); var statement = connection.createStatement()) {
            for (String table : List.of("users", "stores", "products", "orders", "order_details", "store_inventories", "transactions")) {
                try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo." + table)) {
                    rows.next(); result.put(table, rows.getLong(1));
                }
            }
        }
        return result;
    }
    void validateEntities(String url) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", url);
        settings.put("hibernate.connection.username", System.getenv("SPRING_DATASOURCE_USERNAME"));
        settings.put("hibernate.connection.password", System.getenv("SPRING_DATASOURCE_PASSWORD"));
        settings.put("hibernate.connection.driver_class", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        settings.put("hibernate.default_schema", "dbo");
        settings.put("hibernate.physical_naming_strategy", new PhysicalNamingStrategySnakeCaseImpl());
        settings.put("hibernate.hbm2ddl.auto", "validate");
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var metadata = new MetadataSources(registry);
            for (var bean : scanner.findCandidateComponents("com.finalproject.entity"))
                metadata.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            try (var factory = metadata.buildMetadata().buildSessionFactory()) { assertThat(factory.isOpen()).isTrue(); }
        } catch (ClassNotFoundException exception) { throw new IllegalStateException(exception); }
        finally { StandardServiceRegistryBuilder.destroy(registry); }
    }

    @Test void emptyDatabaseMigratesAllVersionsAndValidatesAllEntities() throws Exception {
        String url = url("SQLSERVER_TEST_FRESH_DATABASE");
        var flyway = flyway(url);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(19);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("19");
        validateEntities(url);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test void existingV11CopyMigratesWithoutRepairAndPreservesBusinessRows() throws Exception {
        String url = url("SQLSERVER_TEST_COPY_DATABASE");
        var flyway = flyway(url);
        Map<String, Long> before = counts(url);
        flyway.validateWithResult(); // Pending V12-V19 are expected before migrate.
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(8);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("19");
        assertThat(java.util.Arrays.stream(flyway.info().applied()).filter(m -> "11".equals(m.getVersion().getVersion())).findFirst().orElseThrow().getChecksum())
                .isEqualTo(306107251);
        validateEntities(url);
        assertThat(counts(url)).isEqualTo(before);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    /** Requires an actual pre-Flyway/V2 copy, not a V11 database with its history removed. */
    @Test @EnabledIfSystemProperty(named="sqlserver.legacy-baseline.live", matches="true")
    void existingDatabaseCopyRequiresExplicitBaselineAndPreservesBusinessRows() throws Exception {
        String url = url("SQLSERVER_TEST_COPY_DATABASE");
        Map<String, Long> before = counts(url);
        var flyway = flyway(url);
        assertThatThrownBy(flyway::migrate).hasMessageContaining("non-empty schema");
        flyway.baseline();
        var previousVersion = Flyway.configure().configuration(flyway.getConfiguration()).target("4").load();
        assertThat(previousVersion.migrate().migrationsExecuted).isEqualTo(3);
        assertThat(previousVersion.info().current().getVersion().getVersion()).isEqualTo("4");
        Map<String, Long> beforeV5 = counts(url);
        assertThat(beforeV5).isEqualTo(before);
        long conversationsBefore, messagesBefore;
        try (var connection = connect(url); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo.chat_conversations")) { rows.next(); conversationsBefore = rows.getLong(1); }
            try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo.chat_messages")) { rows.next(); messagesBefore = rows.getLong(1); }
        }
        var auditedVersion = Flyway.configure().configuration(flyway.getConfiguration()).target("5").load();
        assertThat(auditedVersion.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(13);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("19");
        validateEntities(url);
        assertThat(counts(url)).isEqualTo(before);
        try (var connection = connect(url); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo.chat_conversations")) { rows.next(); assertThat(rows.getLong(1)).isEqualTo(conversationsBefore); }
            try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo.chat_messages")) { rows.next(); assertThat(rows.getLong(1)).isEqualTo(messagesBefore); }
            try (var rows = statement.executeQuery("SELECT COUNT_BIG(*) FROM dbo.chat_access_audits")) { rows.next(); assertThat(rows.getLong(1)).isZero(); }
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
