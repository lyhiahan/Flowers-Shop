package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-wallet-${random.uuid};MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.demo-payment.auto-tunnel.enabled=false"
})
@ActiveProfiles("test") @AutoConfigureMockMvc @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerWalletTests {
    @Autowired CustomerWalletService wallets;
    @Autowired CustomerWalletRepository walletRows;
    @Autowired CustomerWalletEntryRepository ledger;
    @Autowired OrderWorkflowService workflow;
    @Autowired OrderService checkout;
    @Autowired DemoPaymentService demo;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired CommissionRepository commissions;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired StoreInventoryRepository stock;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired PaymentRepository payments;
    @Autowired TransactionRepository storeLedger;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Clock clock;
    @Autowired MockMvc mvc;
    @Autowired ConfigurableEnvironment environment;
    @Autowired javax.sql.DataSource dataSource;
    Long customerId, otherId, managerId, storeId, productId, cartItemId;
    String customer, other, manager;

    @BeforeEach void fixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:customer-wallet-");
        }
        tx(() -> {
            String prefix = "wallet-" + UUID.randomUUID().toString().substring(0, 8);
            customer = prefix + "-customer"; other = prefix + "-other"; manager = prefix + "-manager";
            User owner = users.save(actor(customer, Role.USER)); customerId = owner.getId();
            otherId = users.save(actor(other, Role.USER)).getId();
            User seller = users.save(actor(manager, Role.MANAGER)); managerId = seller.getId();
            Store branch = stores.save(Store.builder().name(prefix).address("Quan 1").owner(seller)
                    .eWallet(new BigDecimal("50000")).isOpen(true).status(true).point(0).build()); storeId = branch.getId();
            seller.setStore(branch); users.save(seller);
            Category category = categories.save(Category.builder().name(prefix).build());
            Product product = products.save(Product.builder().name("Wallet rose bouquet").category(category).store(branch)
                    .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.ONE)
                    .depositFee(BigDecimal.ZERO).priceRentPerDay(BigDecimal.ZERO).build()); productId = product.getId();
            stock.save(StoreInventory.builder().store(branch).product(product).quantity(20).build());
            Cart cart = carts.save(Cart.builder().user(owner).items(new ArrayList<>()).build());
            cartItemId = cartItems.save(CartItem.builder().cart(cart).product(product).store(branch).orderType(OrderType.BUY).quantity(1).build()).getId();
            return null;
        });
        login(customer, Role.USER);
    }

    @AfterEach void logout() { SecurityContextHolder.clearContext(); }

    @Test void newUsersHaveIndependentEmptyWalletsAndHistory() throws Exception {
        assertThat(wallets.balance(customer)).isZero();
        assertThat(wallets.history(customer, 0)).isEmpty();
        mvc.perform(get("/customer/wallet").with(user(customer).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("walletBalance", BigDecimal.ZERO))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có giao dịch ví")));
        assertThat(walletRows.findById(customerId)).isEmpty();
    }

    @Test void paidDemoCancellationCreditsExactReceiptOnceAndPersists() {
        Order order = paidDemo("80000");
        workflow.cancelByCustomer(customer, order.getId(), "Khong con nhu cau");
        workflow.cancelByCustomer(customer, order.getId(), "Thu lai");
        assertThat(wallets.balance(customer)).isEqualByComparingTo("80000");
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(1);
        assertThat(ledger.findByOrderIdAndType(order.getId(), "REFUND")).get()
                .satisfies(e -> assertThat(e.getAmount()).isEqualByComparingTo("80000"));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(stores.findById(storeId).orElseThrow().getPendingBalance()).isZero();
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isEqualByComparingTo("50000");
        login(other, Role.USER);
        assertThat(wallets.balance(other)).isZero();
        assertThat(wallets.history(other, 0)).isEmpty();
    }

    @Test void unpaidCancellationNeverCreatesWalletCredits() {
        Order order = demoOrder("100000");
        workflow.cancelByCustomer(customer, order.getId(), "Khong mua nua");
        assertThat(wallets.balance(customer)).isZero();
        assertThat(ledger.findByOrderIdAndType(order.getId(), "REFUND")).isEmpty();
    }

    @Test void managerRefundCreditsTheCustomerOnlyAfterConfirmation() {
        Order order = paidDemo("100000");
        login(manager, Role.MANAGER);
        workflow.transition(order.getId(), OrderStatus.CANCELLED, "Khong du hoa");
        assertThat(walletRows.findById(customerId)).isEmpty();
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        login(customer, Role.USER);
        assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(1);
    }

    @Test void walletCanPayAndRefundANewOrderWithoutMintingCredits() throws Exception {
        refundSeed();
        Order purchase = buyWithWallet();
        assertThat(wallets.balance(customer)).isZero();
        assertThat(purchase.getPaymentStatus()).isEqualTo("PAID");
        assertThat(purchase.getFundsStatus()).isEqualTo(OrderFundsStatus.HELD);
        assertThat(stores.findById(storeId).orElseThrow().getPendingBalance()).isEqualByComparingTo("100000");
        assertThat(ledger.findByOrderIdAndType(purchase.getId(), "PAYMENT")).isPresent();
        assertThat(cartItems.findById(cartItemId)).isEmpty();
        mvc.perform(get("/customer/orders/" + purchase.getId()).with(user(customer).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Ví của tôi")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Ví của tôi (demo)"))));
        login(customer, Role.USER);
        workflow.cancelByCustomer(customer, purchase.getId(), "Doi y");
        workflow.cancelByCustomer(customer, purchase.getId(), "Thu lai");
        assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(3);
        assertThat(stores.findById(storeId).orElseThrow().getPendingBalance()).isZero();
    }

    @Test void insufficientFundsRollBackStockCartOrderAndPayment() {
        int beforeStock = inventory(), beforeOrders = orders.findByUserIdOrderByCreatedAtDesc(customerId).size();
        assertThatThrownBy(this::buyWithWallet).hasMessageContaining("Số dư ví không đủ");
        assertThat(wallets.balance(customer)).isZero();
        assertThat(inventory()).isEqualTo(beforeStock);
        assertThat(cartItems.findById(cartItemId)).isPresent();
        assertThat(orders.findByUserIdOrderByCreatedAtDesc(customerId)).hasSize(beforeOrders);
        assertThat(wallets.history(customer, 0)).isEmpty();
    }

    @Test void failureAfterWalletDebitRollsBackTheWholeCheckout() {
        refundSeed();
        // Holding funds rejects the invalid commission after the wallet debit was persisted.
        tx(() -> { Store branch = stores.findById(storeId).orElseThrow(); branch.setCommission(
                commissions.save(Commission.builder().name("Invalid fee").cost(new BigDecimal("110")).build())); return null; });
        int before = inventory();
        assertThatThrownBy(this::buyWithWallet).isInstanceOf(RuntimeException.class);
        assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        assertThat(inventory()).isEqualTo(before);
        assertThat(cartItems.findById(cartItemId)).isPresent();
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(1);
    }

    @Test void failureReleasingStoreFundsRollsBackWalletCreditAndCancellation() {
        Order order = paidDemo("100000");
        tx(() -> { stores.findById(storeId).orElseThrow().setPendingBalance(BigDecimal.ZERO); return null; });
        int before = inventory();
        assertThatThrownBy(() -> workflow.cancelByCustomer(customer, order.getId(), "Huy don"))
                .hasMessageContaining("Số dư chờ không đủ");
        assertThat(wallets.balance(customer)).isZero();
        assertThat(wallets.history(customer, 0)).isEmpty();
        assertThat(inventory()).isEqualTo(before);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test void walletOrderSettlementAndPostDeliveryRefundReverseIncomeOnce() {
        refundSeed();
        Order purchase = buyWithWallet();
        login(manager, Role.MANAGER);
        workflow.transition(purchase.getId(), OrderStatus.CONFIRMED);
        workflow.transition(purchase.getId(), OrderStatus.PREPARING);
        workflow.transition(purchase.getId(), OrderStatus.COMPLETED);
        workflow.transition(purchase.getId(), OrderStatus.COMPLETED);
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isEqualByComparingTo("140000");
        assertThat(stores.findById(storeId).orElseThrow().getPendingBalance()).isZero();
        login(customer, Role.USER); workflow.requestRefund(customer, purchase.getId());
        login(manager, Role.MANAGER); workflow.transition(purchase.getId(), OrderStatus.REFUNDED);
        workflow.transition(purchase.getId(), OrderStatus.REFUNDED);
        login(customer, Role.USER);
        assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        assertThat(stores.findById(storeId).orElseThrow().getEWallet()).isEqualByComparingTo("50000");
        assertThat(storeLedger.findAllByOrderIdAndType(purchase.getId(), "ORDER_PAYMENT")).hasSize(1);
        assertThat(storeLedger.findAllByOrderIdAndType(purchase.getId(), "COMMISSION_REFUND")).hasSize(1);
    }

    @Test void realPaymentsCannotBeConvertedIntoDemoCredits() {
        Order order = demoOrder("100000");
        tx(() -> { Order real = orders.findById(order.getId()).orElseThrow(); real.setPaymentMethod("BANK_TRANSFER");
            Payment payment = payments.findByOrderId(order.getId()).getFirst(); payment.setMethod(PaymentMethod.BANK_TRANSFER); return null; });
        login(manager, Role.MANAGER); workflow.confirmPayment(order.getId());
        workflow.transition(order.getId(), OrderStatus.CANCELLED, "Manual refund");
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        login(customer, Role.USER);
        assertThat(wallets.balance(customer)).isZero();
        assertThat(wallets.history(customer, 0)).isEmpty();
    }

    @Test void tamperedReceiptAndPreparationCannotCreditTheWallet() {
        Order order = paidDemo("100000");
        tx(() -> { payments.findByOrderId(order.getId()).getFirst().setMomoEnvironment("sandbox"); return null; });
        assertThatThrownBy(() -> workflow.cancelByCustomer(customer, order.getId(), "Cancel"))
                .hasMessageContaining("không khớp");
        assertThat(wallets.balance(customer)).isZero();
        tx(() -> { payments.findByOrderId(order.getId()).getFirst().setMomoEnvironment("demo"); return null; });
        login(manager, Role.MANAGER); workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        login(customer, Role.USER);
        assertThatThrownBy(() -> workflow.cancelByCustomer(customer, order.getId(), "Cancel"))
                .hasMessageContaining("Không thể hủy");
        assertThat(wallets.balance(customer)).isZero();
    }

    @Test void ownershipCannotBeForgedThroughQueryParametersOrServiceArguments() throws Exception {
        Order order = refundSeed();
        login(other, Role.USER);
        assertThatThrownBy(() -> wallets.balance(customer)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> wallets.history(customer, 0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(get("/customer/wallet").param("userId", customerId.toString()).param("username", customer)
                        .with(user(other).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("walletBalance", BigDecimal.ZERO))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/customer/orders/" + order.getId()))));
        mvc.perform(get("/customer/wallet").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/customer/wallet").with(user(manager).roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(post("/customer/wallet").with(user(other).roles("USER"))).andExpect(status().isForbidden());
    }

    @Test void walletHistoryIsPagedAndCheckoutShowsTheRealBalance() throws Exception {
        for (int i = 0; i < 16; i++) { Order order = paidDemo("1000"); workflow.cancelByCustomer(customer, order.getId(), "Huy"); }
        assertThat(wallets.history(customer, 0).getContent()).hasSize(15);
        assertThat(wallets.history(customer, 1).getContent()).hasSize(1);
        mvc.perform(get("/customer/wallet").param("page", "1").with(user(customer).roles("USER"))).andExpect(status().isOk());
        mvc.perform(get("/checkout").param("selectedItemIds", cartItemId.toString()).with(user(customer).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("walletBalance", new BigDecimal("16000.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"WALLET_DEMO\"")));
    }

    @Test void productionModeCannotSpendDemoMoney() {
        refundSeed();
        String[] previous = environment.getActiveProfiles();
        try {
            environment.setActiveProfiles("test", "prod");
            assertThat(wallets.isEnabled()).isFalse();
            assertThatThrownBy(this::buyWithWallet).hasMessageContaining("ví đang tắt");
            assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        } finally { environment.setActiveProfiles(previous); }
    }

    @Test void checkoutPostPaysWithWalletAndRedirectsToOrders() throws Exception {
        refundSeed();
        mvc.perform(post("/checkout").with(user(customer).roles("USER")).with(csrf())
                        .param("selectedItemIds", cartItemId.toString()).param("paymentMethod", "WALLET_DEMO")
                        .param("recipientName", "Customer").param("recipientPhone", "0900000000")
                        .param("shippingMethod", "PICKUP").param("pickupStoreId", storeId.toString())
                        .param("deliveryDate", LocalDate.now(clock).plusDays(1).toString()).param("deliveryTimeSlot", "09:00-11:00"))
                .andExpect(redirectedUrl("/customer/orders")).andExpect(flash().attributeExists("successMessage"));
        login(customer, Role.USER);
        assertThat(wallets.balance(customer)).isZero();
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(2);
    }

    @Test void failedCheckoutKeepsWalletSelectedInsteadOfSilentlySwitchingToCod() throws Exception {
        mvc.perform(post("/checkout").with(user(customer).roles("USER")).with(csrf())
                        .param("selectedItemIds", cartItemId.toString()).param("paymentMethod", "WALLET_DEMO")
                        .param("recipientName", "Customer").param("recipientPhone", "0900000000")
                        .param("shippingMethod", "PICKUP").param("pickupStoreId", storeId.toString())
                        .param("deliveryDate", LocalDate.now(clock).plusDays(1).toString()).param("deliveryTimeSlot", "09:00-11:00"))
                .andExpect(redirectedUrl("/checkout?selectedItemIds=" + cartItemId))
                .andExpect(flash().attribute("prevPaymentMethod", "WALLET_DEMO"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(cartItems.findById(cartItemId)).isPresent();
        assertThat(orders.findByUserIdOrderByCreatedAtDesc(customerId)).isEmpty();
    }

    @Test void simultaneousCancellationCreditsOneRefund() throws Exception {
        Order order = paidDemo("100000");
        assertThat(concurrent(i -> workflow.cancelByCustomer(customer, order.getId(), "Cancel"))).isEqualTo(2);
        assertThat(wallets.balance(customer)).isEqualByComparingTo("100000");
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(1);
    }

    @Test void simultaneousDifferentRefundsDoNotLoseBalanceUpdates() throws Exception {
        List<Order> paid = List.of(paidDemo("100000"), paidDemo("100000"));
        concurrent(i -> workflow.cancelByCustomer(customer, paid.get(i).getId(), "Cancel"));
        assertThat(wallets.balance(customer)).isEqualByComparingTo("200000");
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(2);
    }

    @Test void concurrentPurchasesCannotSpendTheSameBalanceTwice() throws Exception {
        refundSeed();
        Long secondLine = tx(() -> {
            Cart cart = carts.findByUserId(customerId).orElseThrow();
            return cartItems.save(CartItem.builder().cart(cart).store(stores.findById(storeId).orElseThrow())
                    .product(products.findById(productId).orElseThrow()).quantity(1).orderType(OrderType.BUY).build()).getId();
        });
        List<Long> lines = List.of(cartItemId, secondLine);
        int successes = concurrent(i -> checkout.checkout(customer, List.of(lines.get(i)), PaymentMethod.WALLET_DEMO,
                "Customer", "0900000000", "Quan 1", null, null, LocalDate.now(clock), "PICKUP", storeId));
        assertThat(successes).isEqualTo(1);
        assertThat(wallets.balance(customer)).isZero();
        assertThat(wallets.history(customer, 0).getTotalElements()).isEqualTo(2);
        assertThat(cartItems.findByCartId(carts.findByUserId(customerId).orElseThrow().getId())).hasSize(1);
    }

    private User actor(String name, Role role) {
        return User.builder().username(name).email(name + "@test.local").password("unused").fullName(name)
                .phone("0900000000").address("Quan 1").role(role).isActive(true).build();
    }
    private void login(String name, Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name, "",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    private <T> T tx(java.util.function.Supplier<T> action) { return new TransactionTemplate(transactionManager).execute(status -> action.get()); }
    @Test void cancelledFilterIncludesRefundedCancellationsButNotReturnsOrAnotherCustomer() throws Exception {
        Order paid = paidDemo("80000");
        workflow.cancelByCustomer(customer, paid.getId(), "Doi y");
        Order unpaid = demoOrder("20000");
        workflow.cancelByCustomer(customer, unpaid.getId(), "Doi y");
        Order returned = demoOrder("30000");
        Order foreign = demoOrder("40000");
        tx(() -> {
            orders.findById(returned.getId()).orElseThrow().setStatus(OrderStatus.REFUNDED);
            Order notOurs = orders.findById(foreign.getId()).orElseThrow();
            notOurs.setUser(users.findById(otherId).orElseThrow());
            notOurs.setStatus(OrderStatus.CANCELLED);
            Order legacy = orders.findById(paid.getId()).orElseThrow();
            legacy.setCancellationReason(legacy.getCancellationReason().replace("vào ví của bạn", "vào ví demo của bạn"));
            return null;
        });
        assertThat(checkout.getOrdersByUsernameAndStatus(customer, OrderStatus.CANCELLED))
                .extracting(Order::getId).containsExactlyInAnyOrder(paid.getId(), unpaid.getId());
        assertThat(checkout.getOrdersByUsernameAndStatus(customer, OrderStatus.REFUNDED))
                .extracting(Order::getId).containsExactlyInAnyOrder(paid.getId(), returned.getId());
        mvc.perform(get("/customer/orders").param("status", "CANCELLED").param("size", "1")
                        .with(user(customer).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("totalOrders", 2))
                .andExpect(model().attribute("totalPages", 2));
        mvc.perform(get("/customer/orders/" + paid.getId()).with(user(customer).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ví demo"))));
        assertThat(orders.findById(paid.getId()).orElseThrow().getCancellationReason()).contains("ví demo");
        assertThat(orders.findById(paid.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test void cancelledFilterIncludesCancellationAwaitingRefundButNotReturnRequests() {
        Order paid = paidDemo("100000");
        login(manager, Role.MANAGER);
        workflow.transition(paid.getId(), OrderStatus.CANCELLED, "Khong du hoa");
        Order returned = demoOrder("20000");
        tx(() -> { orders.findById(returned.getId()).orElseThrow().setStatus(OrderStatus.REFUND_REQUESTED); return null; });
        assertThat(checkout.getOrdersByUsernameAndStatus(customer, OrderStatus.CANCELLED))
                .extracting(Order::getId).containsExactly(paid.getId());
        assertThat(checkout.getOrdersByUsernameAndStatus(customer, OrderStatus.REFUND_REQUESTED))
                .extracting(Order::getId).containsExactlyInAnyOrder(paid.getId(), returned.getId());
        assertThat(walletRows.findById(customerId)).isEmpty();
    }

    private Order demoOrder(String amount) {
        return tx(() -> {
            Order order = orders.save(Order.builder().user(users.findById(customerId).orElseThrow()).pickupStore(stores.findById(storeId).orElseThrow())
                    .orderType(OrderType.BUY).shippingMethod("PICKUP").status(OrderStatus.PENDING).paymentMethod("MOMO_DEMO")
                    .paymentStatus("UNPAID").totalAmount(new BigDecimal(amount)).createdAt(LocalDateTime.now(clock))
                    .paymentExpiresAt(LocalDateTime.now(clock).plusMinutes(10)).orderDetails(new ArrayList<>()).build());
            order.getOrderDetails().add(details.save(OrderDetail.builder().order(order).product(products.findById(productId).orElseThrow())
                    .quantity(1).unitPrice(new BigDecimal(amount)).build()));
            var inventory = stock.findByStoreIdAndProductId(storeId, productId).orElseThrow(); inventory.setQuantity(inventory.getQuantity() - 1);
            payments.save(Payment.builder().order(order).method(PaymentMethod.MOMO_DEMO).amount(new BigDecimal(amount)).status(PaymentStatus.PENDING).build());
            return order;
        });
    }
    private Order paidDemo(String amount) {
        Order order = demoOrder(amount);
        String token = demo.create(List.of(order.getId()), customer);
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
        return order;
    }
    private Order refundSeed() { Order order = paidDemo("100000"); workflow.cancelByCustomer(customer, order.getId(), "Cancel"); return order; }
    private Order buyWithWallet() {
        return checkout.checkout(customer, List.of(cartItemId), PaymentMethod.WALLET_DEMO,
                "Customer", "0900000000", "Quan 1", null, null, LocalDate.now(clock), "PICKUP", storeId).getFirst();
    }
    private int inventory() { return stock.findByStoreIdAndProductId(storeId, productId).orElseThrow().getQuantity(); }
    private int concurrent(java.util.function.IntConsumer action) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
                final int worker = i;
                futures.add(executor.submit(() -> {
                    login(customer, Role.USER);
                    try { start.await(); action.accept(worker); return true; }
                    catch (IllegalArgumentException | IllegalStateException expected) { return false; }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(20, TimeUnit.SECONDS)) successes++;
            return successes;
        }
    }
}
