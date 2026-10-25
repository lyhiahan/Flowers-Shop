package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.Map;

/** Explicit developer tool only. Never regenerates a checked-in Flyway migration. */
@EnabledIfSystemProperty(named = "postgresql.export-schema", matches = "true")
class PostgreSqlSchemaExportTests {
    @Test
    void exportCurrentMappingsForReview() {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPackagesToScan("com.finalproject.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
                "jakarta.persistence.schema-generation.database.action", "none",
                "jakarta.persistence.schema-generation.scripts.action", "create",
                "jakarta.persistence.schema-generation.scripts.create-target", "target/postgresql-schema.sql"));
        try {
            factory.afterPropertiesSet();
        } finally {
            factory.destroy();
        }
    }
}
