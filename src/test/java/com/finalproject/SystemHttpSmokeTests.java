package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

/** Starts a real HTTP server against a disposable database; never touches the user's SQL Server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:system-http-smoke;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemHttpSmokeTests {
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventory;
    @Autowired PasswordEncoder passwords;
    @Autowired Clock clock;
    private Product product;
    private Store store;

    @BeforeAll void fixtures() {
        var password = passwords.encode("smoke-password");
        User manager = null;
        for (Role role : List.of(Role.USER, Role.MANAGER, Role.ADMIN)) {
            var user = users.saveAndFlush(User.builder().username("smoke_" + role.name().toLowerCase())
                    .email("smoke_" + role.name().toLowerCase() + "@http.test").fullName("Smoke " + role)
                    .role(role).password(password).isActive(true).build());
            if (role == Role.MANAGER) manager = user;
        }
        store = stores.saveAndFlush(Store.builder().name("Smoke branch").address("123 Smoke Street")
                .phone("0901234567").owner(manager).status(true).isOpen(true).build());
        manager.setStore(store); users.saveAndFlush(manager);
        var category = categories.saveAndFlush(Category.builder().name("Smoke flowers").build());
        product = products.saveAndFlush(Product.builder().name("Smoke bouquet").store(store).category(category)
                .importPrice(BigDecimal.valueOf(10000)).priceBuy(BigDecimal.valueOf(20000))
                .depositFee(BigDecimal.ZERO).priceRentPerDay(BigDecimal.ZERO).isActive(true).isSelling(true).build());
        inventory.saveAndFlush(StoreInventory.builder().store(store).product(product).quantity(8).build());
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test void failedLoginRetainsSubmittedEmailThroughRealRedirectAndSessionCookie() throws Exception {
        var browser = client();
        String login = get(browser, "/login").body();
        var csrf = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>").matcher(login);
        assertThat(csrf.find()).isTrue();
        String attempted = "smoke_user@http.test";
        String wrongPassword = "wrong-smoke-login-password";
        String body = "username=" + URLEncoder.encode(attempted, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(wrongPassword, StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrf.group(1), StandardCharsets.UTF_8);
        var response = browser.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/perform_login"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElse("")).endsWith("/login?error=true").doesNotContain(attempted);
        var retry = get(browser, "/login?error=true");
        assertThat(retry.statusCode()).isEqualTo(200);
        assertThat(retry.body()).contains("value=\"" + attempted + "\"", "data-login-failed=\"true\"")
                .doesNotContain(wrongPassword, "value=\"lygiahan\"");
        assertThat(get(browser, "/login?error=true").body()).contains("value=\"" + attempted + "\"");
        assertThat(get(client(), "/login?error=true").body()).doesNotContain("value=\"" + attempted + "\"");
    }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private HttpClient login(String username) throws Exception {
        var client = client();
        var page = get(client, "/login");
        var csrf = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>").matcher(page.body());
        assertThat(csrf.find()).as("login supplies CSRF").isTrue();
        var body = "username=" + username + "&password=smoke-password&_csrf=" + URLEncoder.encode(csrf.group(1), StandardCharsets.UTF_8);
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/perform_login"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElse("")).doesNotContain("error");
        return client;
    }
    private void pages(HttpClient client, List<String> paths) throws Exception {
        for (String path : paths) {
            var response = get(client, path);
            assertThat(response.statusCode()).as("HTTP GET %s", path).isEqualTo(200);
            assertThat(response.body()).as("HTML GET %s", path).contains("</html>")
                    .doesNotContain("Whitelabel Error Page", "Exception evaluating SpringEL");
        }
    }

    @Test void publicPagesRenderThroughRealHttpAndServeDateFilterScript() throws Exception {
        var client = client();
        pages(client, List.of("/", "/products", "/product/" + product.getId(), "/stores", "/stores/" + store.getId(),
                "/users", "/login", "/register", "/forgot-password"));
        assertThat(get(client, "/").body()).contains("Smoke bouquet", "Còn 8");
        var script = get(client, "/js/report-period.js");
        assertThat(script.statusCode()).isEqualTo(200);
        assertThat(script.body()).contains("data-report-period-controls");
    }

    @Test void customerScreensRenderAndPrivilegedScreensRequireAuthentication() throws Exception {
        var customer = login("smoke_user");
        pages(customer, List.of("/cart", "/customer/orders", "/customer/profile", "/customer/wishlist"));
        assertThat(get(customer, "/admin/dashboard").statusCode()).isEqualTo(403);
        assertThat(get(client(), "/admin/dashboard").statusCode()).isEqualTo(302);
        assertThat(get(client(), "/api/auth/me").statusCode()).isEqualTo(401);
    }

    @Test void managerAndAdminScreensRenderAndValidateDateFiltersThroughRealHttp() throws Exception {
        var manager = login("smoke_manager");
        pages(manager, List.of("/manager/dashboard", "/manager/analytics?type=product&timeframe=30days",
                "/manager/products", "/manager/products/create", "/manager/products/edit/" + product.getId(),
                "/manager/products?tab=stored", "/manager/products?tab=all", "/manager/products?tab=deleted",
                "/manager/orders", "/manager/stores", "/manager/vouchers", "/manager/staff", "/manager/profile", "/manager/wallet"));
        var admin = login("smoke_admin");
        var today = LocalDate.now(clock);
        var query = "?timeframe=custom&startDate=" + today.minusDays(6) + "&endDate=" + today;
        pages(admin, List.of("/admin/dashboard" + query, "/admin/users", "/admin/stores", "/admin/orders", "/admin/products",
                "/admin/deliveries", "/admin/deliveries?tab=tariffs", "/admin/deliveries/create",
                "/admin/categories", "/admin/styles", "/admin/store-levels", "/admin/user-levels", "/admin/vouchers", "/admin/chat"));
        var dashboard = get(admin, "/admin/dashboard" + query);
        assertThat(dashboard.body()).contains("Hôm nay", "30 ngày gần nhất", "value=\"" + today + "\"");
        assertThat(get(admin, "/admin/dashboard?timeframe=custom").statusCode()).isEqualTo(400);
    }
}
