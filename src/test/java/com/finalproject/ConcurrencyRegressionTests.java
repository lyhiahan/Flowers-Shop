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
                        .role(i == 2 ? Role.MANAGER : Role.CUSTOMER).isActive(true).store(i == 2 ? store : null).build());
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
                    catch (org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException | IllegalStateException ex) { return false; }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(20, TimeUnit.SECONDS)) successes++;
            return successes;
        }
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
                wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123", "Manager");
            });
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("select e_wallet from stores where id=?", BigDecimal.class, f.storeId())).isEqualByComparingTo("40000");
            assertThat(jdbc.queryForObject("select count(*) from transactions where store_id=?", Integer.class, f.storeId())).isEqualTo(1);
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
                    .createdAt(java.time.LocalDateTime.now()).expiredAt(java.time.LocalDateTime.now().plusMinutes(5)).build()));
            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> auth.resetPassword(f.customers().get(0), "000000", "newpassword"))
                        .isInstanceOf(IllegalArgumentException.class);
            }
            assertThat(jdbc.queryForObject("select failed_attempts from otp_verifications where user_id=?", Integer.class, f.userIds().get(0))).isEqualTo(5);
            assertThatThrownBy(() -> auth.resetPassword(f.customers().get(0), "123456", "newpassword")).hasMessageContaining("bị khóa");
        } finally { cleanup(f); }
    }
}
