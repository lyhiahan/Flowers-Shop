package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

/** Read-only verification of the already migrated disposable SQL Server copy. */
@EnabledIfSystemProperty(named="catalog.sqlserver.live", matches="true")
class ProductCatalogMigrationLiveTests {
    @Test void migratedSqlServerCopyValidatesEveryEntityAndHasNoProductsInHiddenCategories() throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || !url.matches("(?is).*;databaseName=CodexCatalogCopy_[0-9_]+;.*"))
            throw new IllegalArgumentException("Only a disposable catalog copy may be verified.");
        new SqlServerMigrationLiveTests().validateEntities(url);
        try (var connection = DriverManager.getConnection(url, System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
                var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM dbo.products p JOIN dbo.categories c ON c.id=p.category_id WHERE c.is_deleted=1")) {
                rows.next(); assertThat(rows.getLong(1)).isZero();
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM dbo.categories WHERE is_deleted=0")) {
                rows.next(); assertThat(rows.getLong(1)).isGreaterThanOrEqualTo(10);
            }
            try (var rows = statement.executeQuery("SELECT TOP 1 version FROM dbo.flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC")) {
                rows.next(); assertThat(rows.getString(1)).isEqualTo("15");
            }
        }
    }
}
