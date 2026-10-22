package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@EnabledIfSystemProperty(named = "cart.sqlserver.live", matches = "true")
class CartLineSqlServerTests {
    @Test void incompleteLegacyRowsArePreservedOnRepeatedRuns() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE #legacy_cart (id bigint PRIMARY KEY, cart_id bigint, product_id bigint, store_id bigint, order_type varchar(10), quantity int, delivery_date date, greeting_message nvarchar(500))");
            statement.execute("INSERT INTO #legacy_cart VALUES (1,1,1,NULL,'BUY',2,'2026-10-22',N'First'),(2,1,1,NULL,'BUY',3,'2026-10-23',N'Latest')");
            String sql = Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V15__merge_cart_lines_by_product_and_branch.sql")).replace("dbo.cart_items", "#legacy_cart");
            for (int run = 0; run < 2; run++) {
                statement.execute(sql);
                try (var rows = statement.executeQuery("SELECT COUNT(*), SUM(quantity) FROM #legacy_cart")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(2); assertThat(rows.getInt(2)).isEqualTo(5);
                }
                try (var rows = statement.executeQuery("SELECT greeting_message FROM #legacy_cart ORDER BY id")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getNString(1)).isEqualTo("First");
                    assertThat(rows.next()).isTrue(); assertThat(rows.getNString(1)).isEqualTo("Latest");
                }
            }
        }
    }
    @Test void migrationMergesOnlyMatchingBuyRowsPreservesUnitsAndCanRunTwice() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE #cart_line_fixture (id bigint PRIMARY KEY, cart_id bigint, product_id bigint, "
                    + "store_id bigint, order_type varchar(10), quantity int, delivery_date date, greeting_message nvarchar(500))");
            statement.executeUpdate("INSERT INTO #cart_line_fixture VALUES "
                    + "(1,1,1,1,'BUY',1,'2026-10-14',N'Old greeting'),"
                    + "(2,1,1,1,'BUY',2,'2026-10-20',N'Chúc mừng'),"
                    + "(3,1,1,1,'BUY',3,NULL,N' '),"
                    + "(4,1,1,2,'BUY',4,'2026-10-21',N'Other branch'),"
                    + "(5,2,1,1,'BUY',5,'2026-10-22',N'Other cart'),"
                    + "(6,1,1,1,'RENT',6,'2026-10-23',N'Legacy rental'),"
                    + "(7,1,2,1,'BUY',7,NULL,NULL)");
            String migration = Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V15__merge_cart_lines_by_product_and_branch.sql"))
                    .replace("dbo.cart_items", "#cart_line_fixture");
            for (int run = 0; run < 2; run++) {
                statement.execute(migration);
                try (var rows = statement.executeQuery("SELECT id,quantity,delivery_date,greeting_message FROM #cart_line_fixture ORDER BY id")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getLong(1)).isEqualTo(1);
                    assertThat(rows.getInt(2)).isEqualTo(6);
                    assertThat(rows.getDate(3).toString()).isEqualTo("2026-10-20");
                    assertThat(rows.getNString(4)).isEqualTo("Chúc mừng");
                    for (int id : new int[]{4,5,6,7}) {
                        assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(id);
                        assertThat(rows.getInt(2)).isEqualTo(id);
                    }
                    assertThat(rows.next()).isFalse();
                }
                try (var rows = statement.executeQuery("SELECT SUM(quantity) FROM #cart_line_fixture")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(28);
                }
            }
        }
    }
}
