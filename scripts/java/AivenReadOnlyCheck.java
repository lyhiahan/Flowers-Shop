import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Local configuration stays private. This probe only issues SELECTs in a read-only transaction. */
public class AivenReadOnlyCheck {
    public static void main(String[] args) {
        var settings = new Properties();
        try {
            Path file = Path.of(args.length == 0 ? ".env.aiven" : args[0]);
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { settings.load(reader); }
            var connectionSettings = new Properties();
            connectionSettings.setProperty("user", value(settings, "AIVEN_USER", "avnadmin"));
            connectionSettings.setProperty("password", value(settings, "AIVEN_PASSWORD", null));
            connectionSettings.setProperty("sslmode", "verify-full");
            connectionSettings.setProperty("sslrootcert", value(settings, "AIVEN_CA_CERT", "../ca.pem"));
            connectionSettings.setProperty("connectTimeout", "15");
            connectionSettings.setProperty("socketTimeout", "15");
            connectionSettings.setProperty("ApplicationName", "flower-shop-readonly-audit");
            String url = "jdbc:postgresql://" + value(settings, "AIVEN_HOST", "pg-2679153a-rental-retail-shop.c.aivencloud.com")
                    + ":" + value(settings, "AIVEN_PORT", "16325") + "/" + value(settings, "AIVEN_DATABASE", "defaultdb");
            try (var connection = DriverManager.getConnection(url, connectionSettings)) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    statement.setQueryTimeout(15);
                    try (var rows = statement.executeQuery("select current_setting('transaction_read_only'), ssl from pg_stat_ssl where pid=pg_backend_pid()")) {
                        if (!rows.next() || !"on".equals(rows.getString(1)) || !rows.getBoolean(2))
                            throw new SQLException("Read-only TLS required");
                        System.out.println("PASS: PostgreSQL TLS verify-full; transaction_read_only=on");
                    }
                    try (var rows = statement.executeQuery("select version from flyway_schema_history where success order by installed_rank desc limit 1")) {
                        if (!rows.next()) throw new SQLException("Missing migration history");
                        System.out.println("PASS: Flyway schema version=" + rows.getString(1));
                    }
                    // Validate that the current business tables are readable without exposing personal records.
                    for (String table : new String[]{"users", "stores", "products", "store_inventories", "orders", "payments", "customer_wallets", "customer_wallet_entries"}) {
                        try (var rows = statement.executeQuery("select count(*) from " + table)) {
                            rows.next();
                            System.out.println("PASS: " + table + " readable");
                        }
                    }
                    try (var rows = statement.executeQuery("select count(*) from customer_wallets where balance < 0")) {
                        rows.next();
                        if (rows.getLong(1) != 0) throw new SQLException("Negative wallet balance");
                        System.out.println("PASS: no negative customer wallet balances");
                    }
                } finally { connection.rollback(); }
            } finally { connectionSettings.remove("password"); }
            System.out.println("PASS: read-only check finished; no migration, seed, order or payment writes");
        } catch (SQLException failure) {
            System.err.println("FAIL: PostgreSQL read-only check (SQLSTATE " + failure.getSQLState() + "). Check connection/schema; credentials were not printed.");
            System.exit(1);
        } catch (Exception failure) {
            System.err.println("FAIL: local Aiven configuration, CA certificate or JDBC driver is unavailable; credentials were not printed.");
            System.exit(1);
        } finally { settings.remove("AIVEN_PASSWORD"); }
    }

    private static String value(Properties settings, String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) value = settings.getProperty(key, fallback);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
}
