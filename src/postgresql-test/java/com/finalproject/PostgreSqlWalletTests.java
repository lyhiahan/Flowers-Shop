package com.finalproject;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import static org.assertj.core.api.Assertions.*;

/** Reuses the same checkout/refund/concurrency/ownership cases against actual PostgreSQL. */
@SpringBootTest(properties = {
        "app.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
        "app.mail.mock-enabled=true", "spring.config.import=",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
@ActiveProfiles(value = "aiven", inheritProfiles = false)
@Import(PostgreSqlWalletTests.PostgresLifecycle.class)
class PostgreSqlWalletTests extends CustomerWalletTests {
    private static final EmbeddedPostgres POSTGRES = start();
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.finalproject.service.WalletService storeWallet;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().setPort(0).start();
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresLifecycle {
        @Bean(destroyMethod = "close") EmbeddedPostgres disposablePostgres() { return POSTGRES; }
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "postgres");
        properties.add("spring.datasource.hikari.data-source-properties.sslmode", () -> "disable");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }

    @Override void assertIsolatedDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getURL()).isEqualTo(POSTGRES.getJdbcUrl("postgres", "postgres"));
        }
    }

    @Test void freshMigrationValidatesAndSecondRunDoesNotModifyData() {
        long usersBefore = users.count();
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.info().applied()[0].getVersion().getVersion()).isEqualTo("1");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(users.count()).isEqualTo(usersBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('uk_review_user_product_order','ux_transaction_request','ux_store_creation_request')", Integer.class))
                .isEqualTo(3);
    }

    @Test void unicodeLongTextAndStaffRoleRoundTrip() {
        String text = "Hoa t\u01b0\u01a1i S\u00e0i G\u00f2n \uD83C\uDF38 ".repeat(500);
        txForUnicode(text);
        assertThat(products.findById(productId).orElseThrow().getDescription()).isEqualTo(text);
        assertThat(users.findById(otherId).orElseThrow().getRole()).isEqualTo(com.finalproject.enums.Role.STAFF);
    }

    private void txForUnicode(String text) {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            products.findById(productId).orElseThrow().setDescription(text);
            users.findById(otherId).orElseThrow().setRole(com.finalproject.enums.Role.STAFF);
        });
    }

    @Test void loginWithAnExistingPasswordHashWorksAndWrongPasswordFails() throws Exception {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status ->
                users.findById(customerId).orElseThrow().setPassword(encoder.encode("pg-test-password")));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin("/perform_login")
                        .user(customer).password("pg-test-password"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated().withUsername(customer));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin("/perform_login")
                        .user(customer).password("wrong"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
    }

    @Test void catalogSearchAndBranchPagesRender() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/").param("keyword", "Wallet"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/stores/" + storeId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test void revenueQuerySupportsMissingDateBounds() {
        assertThat(orders.recognizedRevenue(storeId, null, null)).isZero();
        assertThat(orders.recognizedRevenue(null, java.time.LocalDateTime.now().minusDays(1), null)).isNotNull();
        assertThat(orders.recognizedRevenue(storeId, null, java.time.LocalDateTime.now().plusDays(1))).isZero();
    }

    @Test void historicalDeliveryRoleCanBeImportedButNeverLogsIn() throws Exception {
        jdbc.update("UPDATE users SET role='SHIPPER', is_active=false, retired_delivery_account=true WHERE id=?", otherId);
        var historical = users.findById(otherId).orElseThrow();
        assertThat(historical.isRetiredDeliveryAccount()).isTrue();
        assertThat(historical.getIsActive()).isFalse();
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status ->
                users.findById(otherId).orElseThrow().setPassword(encoder.encode("historical-password")));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin("/perform_login")
                        .user(other).password("historical-password"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
    }

    @Test void legacyDeadlineUpdateUsesTimestampParameters() {
        Long id = new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status ->
                orders.save(com.finalproject.entity.Order.builder()
                        .user(users.findById(customerId).orElseThrow()).pickupStore(stores.findById(storeId).orElseThrow())
                        .orderType(com.finalproject.enums.OrderType.BUY).status(com.finalproject.enums.OrderStatus.PENDING)
                        .paymentStatus("UNPAID").paymentMethod("MOMO").build()).getId());
        var deadline = java.time.LocalDateTime.of(2026, 12, 1, 12, 30);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status ->
                orders.initializeLegacyPaymentDeadlines(deadline, deadline.plusDays(1)));
        assertThat(orders.findById(id).orElseThrow().getPaymentExpiresAt()).isEqualTo(deadline);
    }

    @Test void rejectedWithdrawalRestoresStoreFundsExactlyOnce() {
        authenticate(manager, com.finalproject.enums.Role.MANAGER);
        storeWallet.requestWithdrawal(new java.math.BigDecimal("50000"), "Test Bank", "123456789", "Test Holder");
        var request = storeLedger.findAll().stream().filter(t -> t.getStore() != null && t.getStore().getId().equals(storeId)
                && "WITHDRAWAL".equals(t.getType())).findFirst().orElseThrow();
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isZero();
        String admin = "pg-admin-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        users.save(com.finalproject.entity.User.builder().username(admin).email(admin + "@test.local")
                .fullName(admin).password("unused").role(com.finalproject.enums.Role.ADMIN).isActive(true).build());
        authenticate(admin, com.finalproject.enums.Role.ADMIN);
        storeWallet.resolveWithdrawal(request.getId(), false);
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isEqualByComparingTo("50000");
        assertThatThrownBy(() -> storeWallet.resolveWithdrawal(request.getId(), false)).isInstanceOf(IllegalStateException.class);
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isEqualByComparingTo("50000");
    }

    private void authenticate(String name, com.finalproject.enums.Role role) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(name, "",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))));
    }
}
