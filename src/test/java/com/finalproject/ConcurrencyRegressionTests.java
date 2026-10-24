package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyRegressionTests {
    @Autowired StoreRepository stores;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired StoreInventoryRepository inventories;
    @Autowired OrderService orders;
    @Autowired WalletService wallet;
    @Autowired AuthService auth;
    @Autowired OtpVerificationRepository otps;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired VoucherRepository vouchers;
    @Autowired OrderWorkflowService workflow;
    @Autowired CustomerAccountClosureService accountClosure;
    @Autowired DemoPaymentService demo;
    @Autowired Clock clock;

    record Fixture(Long storeId, Long productId, Long categoryId, String manager, List<String> customers, List<Long> userIds, List<Long> itemIds) {}

    Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            String prefix = "race" + UUID.randomUUID().toString().substring(0, 8);
            Store store = stores.save(Store.builder().name(prefix).address("Quận 1").eWallet(new BigDecimal("100000")).build());
            Category category = categories.save(Category.builder().name(prefix).build());
            Product product = products.save(Product.builder().name(prefix).category(category).store(store).priceBuy(new BigDecimal("50000"))
                    .importPrice(BigDecimal.ONE).priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
            inventories.save(StoreInventory.builder().store(store).product(product).quantity(1).build());
            List<Long> userIds = new ArrayList<>(), itemIds = new ArrayList<>();
            List<String> customerNames = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String name = prefix + i;
                User user = users.save(User.builder().username(name).email(name + "@test.local").fullName(name).password("unused")
                        .role(i == 2 ? Role.MANAGER : Role.USER).isActive(true).store(i == 2 ? store : null).build());
                if (i == 2) { store.setOwner(user); stores.save(store); }
                userIds.add(user.getId());
                if (i < 2) {
                    customerNames.add(name);
                    Cart cart = carts.save(Cart.builder().user(user).items(new ArrayList<>()).build());
                    itemIds.add(items.save(CartItem.builder().cart(cart).store(store).product(product).quantity(1).orderType(OrderType.BUY).build()).getId());
                }
            }
            return new Fixture(store.getId(), product.getId(), category.getId(), prefix + "2", customerNames, userIds, itemIds);
        });
    }

    void cleanup(Fixture f) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            for (Long userId : f.userIds()) {
                jdbc.update("delete from customer_wallet_entries where user_id=?", userId);
                jdbc.update("delete from customer_wallets where user_id=?", userId);
            }
            jdbc.update("delete from payments where order_id in (select id from orders where pickup_store_id=?)", f.storeId());
            jdbc.update("delete from order_details where order_id in (select id from orders where pickup_store_id=?)", f.storeId());
            jdbc.update("delete from transactions where store_id=?", f.storeId());
            jdbc.update("delete from orders where pickup_store_id=?", f.storeId());
            jdbc.update("delete from cart_items where store_id=?", f.storeId());
            for (Long id : f.userIds()) jdbc.update("delete from carts where user_id=?", id);
            jdbc.update("delete from store_inventories where store_id=?", f.storeId());
            jdbc.update("delete from products where id=?", f.productId());
            jdbc.update("delete from categories where id=?", f.categoryId());
            for (Long id : f.userIds()) {
                jdbc.update("delete from notifications where user_id=?", id);
                jdbc.update("delete from otp_verifications where user_id=?", id);
                jdbc.update("update stores set owner_id=null where id=?", f.storeId());
            jdbc.update("delete from users where id=?", id);
            }
            jdbc.update("delete from stores where id=?", f.storeId());
        });
    }

    int race(java.util.function.IntConsumer action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int worker = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for start");
                    try { action.accept(worker); return true; }
                    catch (org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException | IllegalStateException | IllegalArgumentException ex) { return false; }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(20, TimeUnit.SECONDS)) successes++;
            return successes;
        }
    }

    @Test void accountLockAndCheckoutCannotLeaveAnOpenOrderForDisabledCustomer() throws Exception {
        Fixture f = fixture();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                User admin = users.findByUsername(f.manager()).orElseThrow(); admin.setRole(Role.ADMIN); users.save(admin);
            });
            race(i -> {
                if (i == 0) {
                    try {
                        orders.checkout(f.customers().get(0), List.of(f.itemIds().get(0)), PaymentMethod.COD,
                                "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId());
                    } catch (org.springframework.security.access.AccessDeniedException expected) { }
                } else {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.manager(), "",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                    accountClosure.disable(f.userIds().get(0), false);
                }
            });
            assertThat(jdbc.queryForObject("select is_active from users where id=?", Boolean.class, f.userIds().get(0))).isFalse();
            assertThat(jdbc.queryForObject("select count(*) from orders where user_id=? and status <> 'CANCELLED'", Integer.class, f.userIds().get(0))).isZero();
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void simultaneousCheckoutCannotSellTheSameLastItemTwice() throws Exception {
        Fixture f = fixture();
        try {
            int successes = race(i -> orders.checkout(f.customers().get(i), List.of(f.itemIds().get(i)), PaymentMethod.COD,
                    "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId()));
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isZero();
            assertThat(jdbc.queryForObject("select count(*) from orders where pickup_store_id=?", Integer.class, f.storeId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from cart_items where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void simultaneousWithdrawalsCannotOverdrawWallet() throws Exception {
        Fixture f = fixture();
        try {
            int successes = race(i -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.manager(), "",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
                wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123456789", "Manager");
            });
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("40000");
            assertThat(jdbc.queryForObject("select count(*) from transactions where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void twoCheckoutsCannotConsumeTheLastVoucherTwice() throws Exception {
        Fixture f = fixture();
        String code = "RACE" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                var inventory = inventories.findByStoreIdAndProductId(f.storeId(), f.productId()).orElseThrow();
                inventory.setQuantity(2); inventories.save(inventory);
                vouchers.save(Voucher.builder().code(code).name("Last voucher").quantity(1).discountType("FIXED_AMOUNT")
                        .discountValue(BigDecimal.ONE).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build());
            });
            int successes = race(i -> orders.checkout(f.customers().get(i), List.of(f.itemIds().get(i)), PaymentMethod.COD,
                    "Khách", "0900000000", "Quận 1", null, code, LocalDate.now(), "PICKUP", f.storeId()));
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("select used_count from vouchers where code=?", Integer.class, code)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from orders where pickup_store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); jdbc.update("delete from vouchers where code=?", code); }
    }

    @Test void simultaneousCancellationsRestoreAllReservedStock() throws Exception {
        Fixture f = fixture();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                var stock = inventories.findByStoreIdAndProductId(f.storeId(), f.productId()).orElseThrow();
                stock.setQuantity(2); inventories.save(stock);
            });
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 2; i++) ids.add(orders.checkout(f.customers().get(i), List.of(f.itemIds().get(i)), PaymentMethod.COD,
                    "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId()).get(0).getId());
            assertThat(race(i -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.customers().get(i), "",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                workflow.cancelByCustomer(f.customers().get(i), ids.get(i));
            })).isEqualTo(2);
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isEqualTo(2);
        } finally { cleanup(f); }
    }

    @Test void cancellationAndStartingDeliveryCannotBothSucceed() throws Exception {
        Fixture f = fixture();
        try {
            Long orderId = orders.checkout(f.customers().get(0), List.of(f.itemIds().get(0)), PaymentMethod.COD,
                    "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId()).getFirst().getId();
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                var order = jdbc.update("update orders set status='PREPARING', shipping_method='DELIVERY' where id=?", orderId);
                assertThat(order).isEqualTo(1);
            });
            assertThat(race(i -> {
                String actor = i == 0 ? f.manager() : f.customers().get(0);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "",
                        List.of(new SimpleGrantedAuthority(i == 0 ? "ROLE_MANAGER" : "ROLE_USER"))));
                if (i == 0) workflow.transition(orderId, OrderStatus.DELIVERING);
                else orders.cancelOrder(actor, orderId);
            })).isEqualTo(1);
            String state = jdbc.queryForObject("select status from orders where id=?", String.class, orderId);
            assertThat(state).isIn("CANCELLED", "DELIVERING");
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId()))
                    .isEqualTo("CANCELLED".equals(state) ? 1 : 0);
        } finally { cleanup(f); }
    }

    Long demoOrder(Fixture f, int customer) {
        return orders.checkout(f.customers().get(customer), List.of(f.itemIds().get(customer)), PaymentMethod.MOMO_DEMO,
                "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId()).getFirst().getId();
    }

    void login(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, "",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void concurrentDemoConfirmationsAggregateHeldFundsWithoutLostUpdates() throws Exception {
        Fixture f = fixture();
        try {
            jdbc.update("update store_inventories set quantity=2 where store_id=?", f.storeId());
            Long first = demoOrder(f, 0), second = demoOrder(f, 1);
            List<String> tokens = List.of(demo.create(List.of(first), f.customers().get(0)), demo.create(List.of(second), f.customers().get(1)));
            assertThat(race(i -> demo.confirm(tokens.get(i)))).isEqualTo(2);
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("100000");
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("100000");
            assertThat(jdbc.queryForObject("select count(*) from transactions where store_id=? and type='PAYMENT_HOLD'", Integer.class, f.storeId())).isEqualTo(2);
        } finally { cleanup(f); }
    }

    @Test void duplicateDemoPaymentAndCancellationEachApplyOnceUnderConcurrency() throws Exception {
        Fixture f = fixture();
        try {
            Long id = demoOrder(f, 0);
            String token = demo.create(List.of(id), f.customers().get(0));
            assertThat(race(i -> demo.confirm(token))).isEqualTo(2);
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("50000");
            assertThat(race(i -> {
                login(f.customers().get(0), "USER");
                workflow.cancelByCustomer(f.customers().get(0), id);
            })).isEqualTo(2);
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isZero();
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("100000");
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from transactions where order_id=? and type='REFUND'", Integer.class, id)).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void paymentRacingCancellationCannotLeaveHeldMoneyForACancelledOrder() throws Exception {
        Fixture f = fixture();
        try {
            Long id = demoOrder(f, 0);
            String token = demo.create(List.of(id), f.customers().get(0));
            race(i -> {
                if (i == 0) demo.confirm(token);
                else {
                    login(f.customers().get(0), "USER");
                    workflow.cancelByCustomer(f.customers().get(0), id);
                }
            });
            assertThat(jdbc.queryForObject("select status from orders where id=?", String.class, id)).isIn("CANCELLED", "REFUNDED");
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isZero();
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("100000");
            assertThat(jdbc.queryForObject("select quantity from store_inventories where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void simultaneousFulfillmentSettlesHeldMoneyOnlyOnce() throws Exception {
        Fixture f = fixture();
        try {
            Long id = demoOrder(f, 0);
            demo.confirm(demo.create(List.of(id), f.customers().get(0)));
            login(f.manager(), "MANAGER");
            workflow.transition(id, OrderStatus.CONFIRMED);
            workflow.transition(id, OrderStatus.PREPARING);
            SecurityContextHolder.clearContext();
            assertThat(race(i -> {
                login(f.manager(), "MANAGER");
                workflow.transition(id, OrderStatus.COMPLETED);
            })).isEqualTo(2);
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isZero();
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("145000");
            assertThat(jdbc.queryForObject("select count(*) from transactions where order_id=? and type='HOLD_RELEASE'", Integer.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from transactions where order_id=? and type='ORDER_PAYMENT'", Integer.class, id)).isEqualTo(1);
        } finally { SecurityContextHolder.clearContext(); cleanup(f); }
    }

    @Test void multiBranchPaymentHoldsMoneyInTheCorrectBranches() {
        Fixture first = fixture(), second = fixture();
        try {
            Long firstId = demoOrder(first, 0), secondId = demoOrder(second, 0);
            jdbc.update("update orders set user_id=? where id=?", first.userIds().getFirst(), secondId);
            String token = demo.create(List.of(secondId, firstId), first.customers().getFirst());
            assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
            for (Fixture branch : List.of(first, second)) {
                assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, branch.storeId())).isEqualByComparingTo("50000");
                assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, branch.storeId())).isEqualByComparingTo("100000");
            }
        } finally { cleanup(second); cleanup(first); }
    }

    @Test void multiBranchHoldFailureRollsBackPreviouslyUpdatedBranchAndAllReceipts() {
        Fixture first = fixture(), second = fixture();
        try {
            Long firstId = demoOrder(first, 0), secondId = demoOrder(second, 0);
            jdbc.update("update orders set user_id=? where id=?", first.userIds().getFirst(), secondId);
            String token = demo.create(List.of(firstId, secondId), first.customers().getFirst());
            jdbc.update("update orders set funds_status='REVIEW_REQUIRED' where id=?", secondId);
            assertThatThrownBy(() -> demo.confirm(token)).hasMessageContaining("đối soát");
            for (Long id : List.of(firstId, secondId)) {
                assertThat(jdbc.queryForObject("select payment_status from orders where id=?", String.class, id)).isEqualTo("UNPAID");
                assertThat(jdbc.queryForObject("select status from payments where order_id=?", String.class, id)).isEqualTo("PENDING");
                assertThat(jdbc.queryForObject("select count(*) from transactions where order_id=?", Integer.class, id)).isZero();
            }
            for (Fixture branch : List.of(first, second))
                assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, branch.storeId())).isZero();
        } finally { cleanup(second); cleanup(first); }
    }

    @Test void failedHoldRollsBackPaymentReceiptAndAllBalanceChanges() {
        Fixture f = fixture();
        try {
            Long id = demoOrder(f, 0);
            String token = demo.create(List.of(id), f.customers().get(0));
            jdbc.update("update orders set funds_status='REVIEW_REQUIRED' where id=?", id);
            assertThatThrownBy(() -> demo.confirm(token)).hasMessageContaining("đối soát");
            assertThat(jdbc.queryForObject("select payment_status from orders where id=?", String.class, id)).isEqualTo("UNPAID");
            assertThat(jdbc.queryForObject("select status from payments where order_id=?", String.class, id)).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select pending_balance from stores where id=?", BigDecimal.class, f.storeId())).isZero();
            assertThat(jdbc.queryForObject("select count(*) from transactions where order_id=?", Integer.class, id)).isZero();
        } finally { cleanup(f); }
    }

    @Test void submittingTheSameCartTwiceCreatesOneOrder() throws Exception {
        Fixture f = fixture();
        try {
            int successes = race(i -> orders.checkout(f.customers().get(0), List.of(f.itemIds().get(0)), PaymentMethod.COD,
                    "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", f.storeId()));
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from orders where pickup_store_id=?", Integer.class, f.storeId())).isEqualTo(1);
        } finally { cleanup(f); }
    }

    @Test void failedOtpAttemptsRemainRecordedAfterTheRequestRollsBack() {
        Fixture f = fixture();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> otps.save(OtpVerification.builder()
                    .user(users.findById(f.userIds().get(0)).orElseThrow()).code("123456").type(OtpType.RESET_PASSWORD)
                    .createdAt(java.time.LocalDateTime.now(clock)).expiredAt(java.time.LocalDateTime.now(clock).plusMinutes(5)).build()));
            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> auth.resetPassword(f.customers().get(0), "000000", "newpassword"))
                        .isInstanceOf(IllegalArgumentException.class);
            }
            assertThat(jdbc.queryForObject("select failed_attempts from otp_verifications where user_id=?", Integer.class, f.userIds().get(0))).isEqualTo(5);
            assertThatThrownBy(() -> auth.resetPassword(f.customers().get(0), "123456", "newpassword")).hasMessageContaining("bị khóa");
        } finally { cleanup(f); }
    }
}
