import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Source-file launcher: reads secrets from process environment, never command arguments. */
public class AivenConnectionCheck {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("user", required("AIVEN_USER"));
        properties.setProperty("password", required("AIVEN_PASSWORD"));
        properties.setProperty("sslmode", "verify-full");
        properties.setProperty("sslrootcert", required("AIVEN_CA_CERT"));
        properties.setProperty("connectTimeout", "15");
        properties.setProperty("socketTimeout", "15");
        properties.setProperty("ApplicationName", "flower-shop-readonly-check");
        String url = "jdbc:postgresql://" + required("AIVEN_HOST") + ":" + required("AIVEN_PORT")
                + "/" + required("AIVEN_DATABASE");
        try (var connection = DriverManager.getConnection(url, properties)) {
            connection.setReadOnly(true);
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()")) {
                if (!result.next() || !result.getBoolean(1)) throw new SQLException("TLS required", "08001");
            }
            System.out.println("OK: verified TLS certificate and PostgreSQL login. No data changed.");
        } catch (SQLException failure) {
            // Do not echo provider messages or a URI containing credentials.
            System.err.println(failureMessage(failure.getSQLState()));
            System.exit(1);
        } finally {
            properties.remove("password");
        }
    }

    static String failureMessage(String state) {
        if ("28P01".equals(state)) {
            return "Login rejected (SQLSTATE 28P01): PostgreSQL rejected the user/password. "
                    + "Copy Password for the selected service and user from Aiven, then retry. "
                    + "Use the raw password, not the Service URI, masked stars, Google password or old SQL Server password.";
        }
        if ("28000".equals(state)) {
            return "Login not permitted (SQLSTATE 28000). Check the selected Aiven service, user and access rules.";
        }
        return "Connection failed (SQLSTATE " + state
                + "). Check service status, network and CA certificate.";
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }
}
