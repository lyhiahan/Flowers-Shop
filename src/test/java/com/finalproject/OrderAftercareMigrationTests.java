package com.finalproject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** DDL and constraints in H2 compatibility modes; not a substitute for vendor integration. */
class OrderAftercareMigrationTests {
    @ParameterizedTest
    @CsvSource({"sqlserver,V19,MSSQLServer,dbo.", "postgresql,V3,PostgreSQL,''"})
    void additiveTablePreservesOrdersAndRejectsInvalidRecords(String vendor, String version, String mode, String prefix) throws Exception {
        try (var db = DriverManager.getConnection("jdbc:h2:mem:care-ddl-" + UUID.randomUUID() + ";MODE=" + mode, "sa", "");
             var st = db.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS dbo");
            st.execute("CREATE TABLE " + prefix + "orders(id bigint PRIMARY KEY, total_amount decimal(18,2))");
            st.execute("INSERT INTO " + prefix + "orders VALUES (1, 123000)");
            st.execute(Files.readString(Path.of("src/main/resources/db/migration/" + vendor + "/" + version + "__order_aftercare.sql")));
            st.execute("INSERT INTO " + prefix + "order_aftercare(order_id, florist_note) VALUES (1, 'Hoa đã chuẩn bị')");
            try (var rows = st.executeQuery("SELECT a.photo_status, a.complaint_status, a.photo_revision, o.total_amount FROM " + prefix + "orders o JOIN " + prefix + "order_aftercare a ON a.order_id=o.id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("NONE");
                assertThat(rows.getString(2)).isEqualTo("NONE");
                assertThat(rows.getInt(3)).isZero();
                assertThat(rows.getBigDecimal(4)).isEqualByComparingTo("123000");
            }
            assertThatThrownBy(() -> st.execute("INSERT INTO " + prefix + "order_aftercare(order_id) VALUES (999)")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute("UPDATE " + prefix + "order_aftercare SET photo_status='INVALID'")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute("UPDATE " + prefix + "order_aftercare SET complaint_status='INVALID'")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute("UPDATE " + prefix + "order_aftercare SET photo_revision=-1")).isInstanceOf(SQLException.class);
        }
    }
}
