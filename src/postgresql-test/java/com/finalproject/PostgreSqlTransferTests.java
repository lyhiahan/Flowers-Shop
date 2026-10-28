package com.finalproject;

import com.finalproject.tools.DatabaseTransfer;
import com.finalproject.tools.DatabaseTransfer.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PostgreSqlTransferTests {
    private static EmbeddedPostgres postgres;
    private Connection destination;
    private Snapshot snapshot;
    @TempDir Path temp;

    @BeforeAll static void start() throws Exception {
        postgres = EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses", "127.0.0.1").start();
    }
    @AfterAll static void stop() throws Exception { if (postgres != null) postgres.close(); }
    @AfterEach void close() throws Exception { if (destination != null) destination.close(); }

    private static Connection emptyDatabase() throws Exception {
        String name = "transfer_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = postgres.getPostgresDatabase().getConnection(); var st = connection.createStatement()) {
            st.execute("CREATE DATABASE " + name);
        }
        String url = postgres.getJdbcUrl("postgres", name);
        Flyway.configure().dataSource(url, "postgres", "postgres").defaultSchema("public")
                .locations("classpath:db/migration/postgresql").target("1").cleanDisabled(true).load().migrate();
        return DriverManager.getConnection(url, "postgres", "postgres");
    }

    @BeforeEach void fixture() throws Exception {
        destination = emptyDatabase();
        try (var source = emptyDatabase(); var st = source.createStatement()) {
            st.execute("UPDATE flyway_schema_history SET version='17'");
            st.execute("ALTER TABLE users ADD COLUMN legacy_note text");
            st.execute("CREATE TABLE historical_notes (id bigint, note text)");
            st.execute("INSERT INTO historical_notes VALUES (9,'old audit data')");
            st.execute("INSERT INTO users (id,username,email,full_name,password,role,is_email_active,legacy_note) VALUES (1,'transfer-user','transfer@test.local','Hoa tươi 🌸','unchanged-password-hash','USER',true,'preserved legacy field')");
            st.execute("INSERT INTO user_roles VALUES (1,'USER')");
            st.execute("INSERT INTO stores (id,version,name,address,owner_id,pending_balance,e_wallet) VALUES (5,0,'Cửa hàng','Quận 1',1,0,50000)");
            st.execute("UPDATE users SET store_id=5 WHERE id=1");
            st.execute("INSERT INTO categories (id,name) VALUES (10,'Child'),(20,'Parent')");
            st.execute("UPDATE categories SET parent_id=20 WHERE id=10");
            st.execute("INSERT INTO products (id,name,category_id,store_id,description,price_buy,price_rent_per_day,deposit_fee,import_price,is_active,is_selling) VALUES (30,'Hoa 🌸',10,5,'Unicode & long text',675000,0,0,100000,true,true)");
            st.execute("INSERT INTO orders (id,version,order_type,status,funds_status,payment_status,payment_method,user_id,pickup_store_id,total_amount,created_at) VALUES (36,0,'BUY','REFUNDED','REFUNDED','REFUNDED','MOMO_DEMO',1,5,675000,'2026-10-24 23:59:59.999999')");
            st.execute("INSERT INTO customer_wallets VALUES (675000,1,2)");
            st.execute("INSERT INTO customer_wallet_entries (id,user_id,order_id,type,amount,balance_after,created_at) VALUES (7,1,36,'REFUND',675000,675000,'2026-10-24 17:22:00')");
            st.execute("INSERT INTO otp_verifications (id,failed_attempts,code,user_id,type,expired_at) VALUES (1,0,'123456',1,'ACTIVATE_ACCOUNT','2030-01-01')");
            snapshot = DatabaseTransfer.capture(source, "public", "RentalShop");
        }
    }

    @Test void encryptedSnapshotAuthenticatesBeforeUseAndPreservesUnicode() throws Exception {
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        Path file = temp.resolve("snapshot.enc");
        DatabaseTransfer.writeEncrypted(snapshot, file, key);
        assertThat(DatabaseTransfer.readEncrypted(file, key)).isEqualTo(snapshot);
        byte[] wrong = key.clone(); wrong[0] ^= 1;
        assertThatThrownBy(() -> DatabaseTransfer.readEncrypted(file, wrong)).isInstanceOf(Exception.class);
        byte[] bytes = Files.readAllBytes(file); bytes[bytes.length - 1] ^= 1; Files.write(file, bytes);
        assertThatThrownBy(() -> DatabaseTransfer.readEncrypted(file, key)).isInstanceOf(Exception.class);
    }

    @Test void preflightIsReadOnlyAndReportsArchivedFields() throws Exception {
        Plan plan = DatabaseTransfer.plan(snapshot, destination);
        assertThat(plan.tables()).filteredOn(p -> p.table().equals("users")).singleElement()
                .satisfies(p -> assertThat(p.archivedColumns()).contains("legacy_note"));
        assertThat(plan.archiveOnlyTables()).contains("historical_notes");
        assertThat(plan.skippedAuthenticationTables()).contains("otp_verifications", "jwt_sessions", "jwt_refresh_tokens");
        assertThat(number("SELECT count(*) FROM users")).isZero();
        assertThat(number("SELECT count(*) FROM information_schema.schemata WHERE schema_name='rental_sqlserver_archive'")).isZero();
    }

    @Test void importsCyclesPreservesHashesAndMoneyAndRestartsIdentityWithoutDisablingConstraints() throws Exception {
        var result = DatabaseTransfer.apply(snapshot, destination);
        assertThat(result.verifiedTables()).extracting(Check::table).contains("users", "orders", "customer_wallet_entries");
        assertThat(text("SELECT password FROM users WHERE id=1")).isEqualTo("unchanged-password-hash");
        assertThat(text("SELECT full_name FROM users WHERE id=1")).isEqualTo("Hoa tươi 🌸");
        assertThat(number("SELECT store_id FROM users WHERE id=1")).isEqualTo(5);
        assertThat(number("SELECT owner_id FROM stores WHERE id=5")).isEqualTo(1);
        assertThat(number("SELECT parent_id FROM categories WHERE id=10")).isEqualTo(20);
        assertThat(text("SELECT balance FROM customer_wallets WHERE user_id=1")).isEqualTo("675000.00");
        assertThat(result.financialTotals().get("customer_wallets.balance")).isEqualTo("675000.00");
        assertThat(number("SELECT count(*) FROM otp_verifications")).isZero();
        assertThat(text("SELECT version FROM flyway_schema_history")).isEqualTo("1");
        assertThat(number("SELECT count(*) FROM rental_sqlserver_archive.source_snapshot")).isEqualTo(1);
        assertThat(text("SELECT contents::text FROM rental_sqlserver_archive.source_snapshot"))
                .contains("preserved legacy field", "historical_notes").doesNotContain("otp_verifications");
        assertThat(number("INSERT INTO categories(name) VALUES ('Next') RETURNING id")).isEqualTo(21);
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated")).isZero();
    }

    @Test void refusesNonemptyDestinationAndRepeatedImports() throws Exception {
        DatabaseTransfer.apply(snapshot, destination);
        assertThatThrownBy(() -> DatabaseTransfer.apply(snapshot, destination)).isInstanceOf(IllegalStateException.class);
        assertThat(number("SELECT count(*) FROM users")).isEqualTo(1);
        assertThat(text("SELECT balance FROM customer_wallets")).isEqualTo("675000.00");
    }

    @Test void existingTargetRowsAreNotDeletedOrOverwritten() throws Exception {
        try (var st = destination.createStatement()) { st.execute("INSERT INTO categories (id,name) VALUES (99,'Existing data')"); }
        assertThatThrownBy(() -> DatabaseTransfer.apply(snapshot, destination)).isInstanceOf(IllegalStateException.class).hasMessageContaining("not empty");
        assertThat(text("SELECT name FROM categories WHERE id=99")).isEqualTo("Existing data");
        assertThat(number("SELECT count(*) FROM users")).isZero();
    }

    @Test void foreignKeyFailureRollsBackEveryBusinessRow() throws Exception {
        Snapshot invalid = replace(snapshot, "users", "store_id", "999999");
        assertThatThrownBy(() -> DatabaseTransfer.apply(invalid, destination)).isInstanceOf(SQLException.class);
        assertThat(number("SELECT count(*) FROM users")).isZero();
        assertThat(number("SELECT count(*) FROM stores")).isZero();
        assertThat(number("SELECT count(*) FROM customer_wallet_entries")).isZero();
    }

    @Test void monetaryRoundingIsDetectedAndEarlierIdentityRestartsAlsoRollBack() throws Exception {
        Snapshot invalid = replace(snapshot, "products", "price_buy", "999.999");
        assertThatThrownBy(() -> DatabaseTransfer.apply(invalid, destination)).isInstanceOf(IllegalStateException.class).hasMessageContaining("Reconciliation failed");
        assertThat(number("SELECT count(*) FROM users")).isZero();
        assertThat(number("SELECT count(*) FROM categories")).isZero();
        assertThat(number("INSERT INTO categories(name) VALUES ('After rollback') RETURNING id")).isEqualTo(1);
    }

    @Test void subMicrosecondTimestampsAreTruncatedWithoutCrossingTheDayBoundary() throws Exception {
        Snapshot precise = replace(snapshot, "orders", "created_at", "2026-10-24T23:59:59.9999999");
        DatabaseTransfer.apply(precise, destination);
        assertThat(text("SELECT created_at::text FROM orders WHERE id=36")).isEqualTo("2026-10-24 23:59:59.999999");
    }

    @Test @EnabledIfSystemProperty(named="migration.rehearsal", matches="true")
    void rehearseEncryptedRealSnapshotOnlyOnDisposableLocalPostgres() throws Exception {
        try (var metadata = destination.createStatement(); var rs = metadata.executeQuery("SELECT host(inet_server_addr())")) {
            rs.next(); assertThat(rs.getString(1)).isEqualTo("127.0.0.1");
        }
        byte[] key = Base64.getDecoder().decode(System.getenv("MIGRATION_BACKUP_KEY"));
        Snapshot real;
        try { real = DatabaseTransfer.readEncrypted(Path.of(System.getenv("MIGRATION_SNAPSHOT_FILE")), key); }
        finally { Arrays.fill(key, (byte)0); }
        Plan plan;
        Result imported;
        try {
            plan = DatabaseTransfer.plan(real, destination);
            imported = DatabaseTransfer.apply(real, destination);
        } catch (SQLException failure) {
            throw new AssertionError("Real-snapshot rehearsal failed; SQLSTATE=" + failure.getSQLState() + ", code=" + failure.getErrorCode());
        }
        assertThat(imported.verifiedTables()).hasSameSizeAs(plan.tables());
        var counts = new HashMap<String,Integer>(); real.tables().forEach(t -> counts.put(t.name(), t.rows().size()));
        for (var checked : imported.verifiedTables()) assertThat(checked.rows()).isEqualTo(counts.get(checked.table()).longValue());
        System.out.println("REHEARSAL VERIFIED: " + imported.verifiedTables().size() + " tables; encrypted source snapshot " + real.id());
    }

    private static Snapshot replace(Snapshot input, String table, String column, String value) {
        return new Snapshot(input.format(),input.id(),input.createdAt(),input.sourceDatabase(),input.sourceVersion(),input.tables().stream().map(t -> {
            if (!t.name().equals(table)) return t;
            int position = java.util.stream.IntStream.range(0,t.columns().size()).filter(i -> t.columns().get(i).name().equals(column)).findFirst().orElseThrow();
            return new Table(t.name(),t.columns(),t.rows().stream().map(row -> { List<String> copy=new ArrayList<>(row);copy.set(position,value);return copy; }).toList());
        }).toList());
    }
    private long number(String sql) throws Exception { try(var st=destination.createStatement();var rs=st.executeQuery(sql)){rs.next();return rs.getLong(1);} }
    private String text(String sql) throws Exception { try(var st=destination.createStatement();var rs=st.executeQuery(sql)){rs.next();return rs.getString(1);} }
}
