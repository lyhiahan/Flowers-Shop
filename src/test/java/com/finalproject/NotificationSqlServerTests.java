package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@EnabledIfSystemProperty(named = "notifications.sqlserver.live", matches = "true")
class NotificationSqlServerTests {
    private Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"));
        connection.setReadOnly(true);
        return connection;
    }

    @Test void persistedNotificationTitlesAreReadableWithTheEntityNationalizedMapping() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT title,content FROM dbo.notifications")) {
                while (rows.next()) {
                    assertThat(rows.getNString("title")).isNotNull();
                    rows.getNString("content");
                }
            }
            try (var rows = statement.executeQuery("SELECT TYPE_NAME(system_type_id),max_length FROM sys.columns "
                    + "WHERE object_id=OBJECT_ID('dbo.notifications') AND name='title'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("nvarchar");
                assertThat(rows.getInt(2)).isGreaterThanOrEqualTo(400);
            }
        }
    }

    @Test void driverRequiresAUnicodeColumnForNationalizedStringReads() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT CAST('notification' AS varchar(200)) AS title")) {
                assertThat(rows.next()).isTrue();
                assertThatThrownBy(() -> rows.getNString("title")).isInstanceOf(SQLException.class);
            }
            try (var rows = statement.executeQuery("SELECT CAST(N'notification' AS nvarchar(200)) AS title")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getNString("title")).isEqualTo("notification");
            }
        }
    }

    @Test void legacyTitleRecoveryRepairsOnlyExactTemplatesAndIsIdempotent() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            // Local temporary table: this test never updates persistent notifications.
            statement.execute("CREATE TABLE #notification_title_fixture (id int PRIMARY KEY, title nvarchar(200), "
                    + "type varchar(50), related_order_id bigint, content nvarchar(200), is_read bit)");
            String[][] cases = {
                    {"ORDER_CREATED", "🌸 Đặt hoa thành công!"},
                    {"ORDER_NEW", "🔔 Đơn hoa mới #30"},
                    {"ORDER_CONFIRMED", "✅ Đơn hoa #30 đã được xác nhận"},
                    {"ORDER_PREPARING", "🌺 Đang cắm hoa đơn #30"},
                    {"ORDER_DELIVERING", "🚀 Đơn hoa #30 đang được giao"},
                    {"ORDER_DELIVERED", "🎉 Giao hoa thành công đơn #30"},
                    {"ORDER_COMPLETED", "✨ Đơn hoa #30 đã hoàn tất"},
                    {"ORDER_CANCELLED", "❌ Đơn hoa #30 đã bị hủy"},
                    {"ORDER_CANCELLED", "⚠️ Đơn hoa #30 đã bị hủy"},
                    {"REFUND_COMPLETED", "💰 Hoàn tiền thành công đơn #30"}
            };
            Map<Integer, String> expected = new LinkedHashMap<>();
            try (var insert = connection.prepareStatement("INSERT INTO #notification_title_fixture VALUES "
                    + "(?, CONVERT(nvarchar(200),CONVERT(varchar(200),CAST(? AS nvarchar(200)) COLLATE SQL_Latin1_General_CP1_CI_AS)), ?, 30, N'Preserved content', 1)")) {
                for (int i = 0; i < cases.length; i++) {
                    insert.setInt(1, i + 1);
                    insert.setNString(2, cases[i][1]);
                    insert.setString(3, cases[i][0]);
                    insert.executeUpdate();
                    expected.put(i + 1, cases[i][1]);
                }
            }
            statement.executeUpdate("INSERT INTO #notification_title_fixture VALUES "
                    + "(20, N'Ghi chú riêng của khách?', 'ORDER_DELIVERED', 30, N'Preserved content', 1),"
                    + "(21, N'🎉 Giao hoa thành công đơn #30', 'ORDER_DELIVERED', 30, N'Preserved content', 1),"
                    + "(22, N'?? Giao hoa thành công don #30', 'SYSTEM', 30, N'Preserved content', 1),"
                    + "(23, N'?? Giao hoa thành công don #30', 'ORDER_DELIVERED', NULL, N'Preserved content', 1),"
                    + "(24, N'?? Giao hoa thành công don #30', 'ORDER_DELIVERED', 31, N'Preserved content', 1)");
            expected.put(20, "Ghi chú riêng của khách?");
            expected.put(21, "🎉 Giao hoa thành công đơn #30");
            for (int id = 22; id <= 24; id++) expected.put(id, "?? Giao hoa thành công don #30");
            String migration = Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V13__restore_legacy_notification_titles.sql"))
                    .replace("dbo.notifications", "#notification_title_fixture");
            for (int run = 0; run < 2; run++) {
                statement.execute(migration);
                try (var rows = statement.executeQuery("SELECT id,title,content,is_read FROM #notification_title_fixture ORDER BY id")) {
                    Map<Integer, String> actual = new LinkedHashMap<>();
                    while (rows.next()) {
                        actual.put(rows.getInt(1), rows.getNString(2));
                        assertThat(rows.getNString(3)).isEqualTo("Preserved content");
                        assertThat(rows.getBoolean(4)).isTrue();
                    }
                    assertThat(actual).isEqualTo(expected);
                }
            }
        }
    }
}
