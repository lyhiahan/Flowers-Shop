import com.finalproject.FinalprojectApplication;
import com.finalproject.config.OrderExpirySchedule;
import org.springframework.boot.builder.SpringApplicationBuilder;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Starts the current app with Aiven and enforces read-only connections before any business code runs. */
public class AivenApplicationSmoke {
    public static void main(String[] ignored) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        try (var context = new SpringApplicationBuilder(FinalprojectApplication.class).run(
                "--spring.profiles.active=aiven", "--server.address=127.0.0.1", "--server.port=0",
                "--spring.flyway.enabled=false", "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.datasource.hikari.read-only=true", "--spring.datasource.hikari.maximum-pool-size=1",
                "--spring.datasource.hikari.minimum-idle=0", "--app.order-expiry.enabled=false",
                "--app.demo-data.enabled=false", "--app.demo-payment.auto-tunnel.enabled=false",
                "--spring.main.banner-mode=off", "--logging.level.root=ERROR")) {
            if (!context.getBeansOfType(OrderExpirySchedule.class).isEmpty())
                throw new IllegalStateException("Expiry runner must be disabled");
            try (var connection = context.getBean(DataSource.class).getConnection()) {
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("select current_setting('transaction_read_only')")) {
                    if (!rows.next() || !"on".equals(rows.getString(1))) throw new IllegalStateException("Read-only required");
                } finally { connection.rollback(); }
            }
            System.out.println("PASS: current Aiven application starts; JPA schema validates; connections are read-only; migration, seed, expiry and tunnel disabled");
            int port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
            for (String route : new String[]{"/", "/products", "/stores", "/css/home.css", "/js/home-navigation.js", "/js/home-follow.js", "/js/home-filters.js"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route))
                        .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new IllegalStateException("Unexpected HTTP status for " + route);
                if (route.equals("/") && (!response.body().contains("/js/home-filters.js") || !response.body().contains("id=\"product-catalog\"")))
                    throw new IllegalStateException("Homepage fragments did not render");
                System.out.println("PASS: HTTP 200 " + route);
            }
            System.out.println("PASS: read-only application smoke complete; context closes automatically; no order/payment created");
        } catch (Exception failure) {
            System.err.println("FAIL: read-only application smoke (" + failure.getClass().getSimpleName() + "); credentials were not printed");
            System.exit(1);
        }
    }
}
