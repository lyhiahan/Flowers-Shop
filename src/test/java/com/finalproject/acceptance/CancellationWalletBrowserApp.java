package com.finalproject.acceptance;

import com.finalproject.FinalprojectApplication;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.DemoPaymentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/** Disposable browser acceptance app. It cannot select a real database or open a tunnel. */
public class CancellationWalletBrowserApp {
    public static void main(String[] ignored) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        new SpringApplicationBuilder(FinalprojectApplication.class, Fixtures.class).run(
                "--spring.profiles.active=test", "--server.address=127.0.0.1", "--server.port=8083",
                "--spring.datasource.url=jdbc:h2:mem:acceptance-browser-" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.flyway.enabled=false",
                "--app.demo-data.enabled=false", "--app.demo-payment.enabled=true", "--app.demo-payment.auto-tunnel.enabled=false",
                "--app.demo-payment.base-url=http://127.0.0.1:8083", "--momo.mode=mock");
    }

    record Ids(Long store, Long product, Long cancelOrder, Long deliveryOrder) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean AtomicReference<Ids> fixtureIds() { return new AtomicReference<>(); }

        @Bean CommandLineRunner acceptanceData(DataSource dataSource, PlatformTransactionManager transactionManager,
                UserRepository users, StoreRepository stores, CommissionRepository commissions, CategoryRepository categories,
                ProductRepository products, StoreInventoryRepository inventories, OrderRepository orders,
                OrderDetailRepository details, PaymentRepository payments, DemoPaymentService demo,
                PasswordEncoder passwords, AtomicReference<Ids> fixtureIds) {
            return ignored -> {
                try (var connection = dataSource.getConnection()) {
                    if (!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:acceptance-browser-"))
                        throw new IllegalStateException("Browser fixtures require a disposable in-memory database.");
                }
                var ids = new TransactionTemplate(transactionManager).execute(tx -> {
                    var fee = commissions.save(Commission.builder().name("Acceptance fee").cost(BigDecimal.TEN).build());
                    var store = stores.save(Store.builder().name("Acceptance flower branch").address("Test address")
                            .eWallet(new BigDecimal("50000")).pendingBalance(BigDecimal.ZERO).commission(fee)
                            .point(3).isOpen(true).status(true).build());
                    var hash = passwords.encode("Acceptance-only-password-2026");
                    var customer = users.save(User.builder().username("acceptance-customer").email("customer@acceptance.test")
                            .fullName("Acceptance customer").password(hash).role(Role.USER).isActive(true).build());
                    var manager = users.save(User.builder().username("acceptance-manager").email("manager@acceptance.test")
                            .fullName("Acceptance manager").password(hash).role(Role.MANAGER).isActive(true).store(store).build());
                    users.save(User.builder().username("acceptance-admin").email("admin@acceptance.test")
                            .fullName("Acceptance admin").password(hash).role(Role.ADMIN).isActive(true).build());
                    store.setOwner(manager); stores.save(store);
                    var category = categories.save(Category.builder().name("Acceptance flowers").build());
                    var product = products.save(Product.builder().name("Acceptance rose bouquet").category(category).store(store)
                            .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.ONE)
                            .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
                    inventories.save(StoreInventory.builder().store(store).product(product).quantity(8).build());
                    var created = new ArrayList<Order>();
                    for (int index = 0; index < 2; index++) {
                        var order = orders.save(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                                .status(OrderStatus.PENDING).shippingMethod("DELIVERY").paymentMethod("MOMO_DEMO").paymentStatus("UNPAID")
                                .createdAt(LocalDateTime.now()).paymentExpiresAt(LocalDateTime.now().plusHours(2))
                                .recipientName("Acceptance recipient").recipientPhone("0900000000").recipientAddress("Test address")
                                .totalAmount(new BigDecimal("100000")).orderDetails(new ArrayList<>()).build());
                        order.getOrderDetails().add(details.save(OrderDetail.builder().order(order).product(product).quantity(1)
                                .unitPrice(new BigDecimal("100000")).build()));
                        payments.save(Payment.builder().order(order).method(PaymentMethod.MOMO_DEMO).amount(order.getTotalAmount())
                                .status(PaymentStatus.PENDING).build());
                        created.add(order);
                    }
                    orders.flush();
                    demo.confirm(demo.create(List.of(created.getFirst().getId()), customer.getUsername()));
                    return new Ids(store.getId(), product.getId(), created.get(0).getId(), created.get(1).getId());
                });
                fixtureIds.set(ids);
                System.out.println("ACCEPTANCE_FIXTURES_READY store=" + ids.store() + " cancel=" + ids.cancelOrder() + " delivery=" + ids.deliveryOrder());
            };
        }

        @Bean RouterFunction<ServerResponse> acceptanceState(AtomicReference<Ids> fixtureIds, JdbcTemplate jdbc) {
            return route(GET("/api/public/acceptance-test-state"), request -> {
                var ids = fixtureIds.get();
                if (ids == null) return ServerResponse.status(503).body(Map.of("ready", false));
                var state = new LinkedHashMap<String, Object>();
                state.put("database", "DISPOSABLE_H2");
                state.put("ids", ids);
                state.put("wallet", jdbc.queryForMap("select e_wallet, pending_balance from stores where id=?", ids.store()));
                state.put("stock", jdbc.queryForObject("select quantity from store_inventories where store_id=? and product_id=?", Integer.class, ids.store(), ids.product()));
                state.put("orders", jdbc.queryForList("select id,status,payment_status,funds_status,cancellation_reason from orders where pickup_store_id=? order by id", ids.store()));
                state.put("ledger", jdbc.queryForList("select id,order_id,type,amount,status,wallet_bucket from transactions where store_id=? order by id", ids.store()));
                return ServerResponse.ok().body(state);
            });
        }
    }
}
