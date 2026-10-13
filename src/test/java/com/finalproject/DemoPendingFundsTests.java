package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DemoPendingFundsTests {
    @Autowired DemoPaymentService demo;
    @Autowired OrderWorkflowService workflow;
    @Autowired WalletService wallet;
    @Autowired AnalyticsService analytics;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CommissionRepository commissions;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired OrderDetailRepository details;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventories;
    @Autowired TransactionRepository transactions;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    Store store;
    User customer, manager;
    Product product;
    Commission commission;

    @BeforeEach void setup() {
        commission = commissions.save(Commission.builder().name("Pending funds 10").cost(BigDecimal.TEN).build());
        store = stores.save(Store.builder().name("Pending funds branch").address("Quận 1")
                .commission(commission).eWallet(new BigDecimal("50000")).point(3).isOpen(true).status(true).build());
        customer = users.save(User.builder().username("held-customer").email("held-customer@test.local").password("unused")
                .fullName("Khách hàng").role(Role.USER).isActive(true).build());
        manager = users.save(User.builder().username("held-manager").email("held-manager@test.local").password("unused")
                .fullName("Quản lý").role(Role.MANAGER).isActive(true).store(store).build());
        store.setOwner(manager);
        Category category = categories.save(Category.builder().name("Pending flowers").build());
        product = products.save(Product.builder().name("Hoa thử nghiệm").category(category).store(store)
                .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.ONE)
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        inventories.save(StoreInventory.builder().store(store).product(product).quantity(10).build());
        login(customer);
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    void login(User actor) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor.getUsername(), "",
                List.of(new SimpleGrantedAuthority("ROLE_" + actor.getRole()))));
    }

    Order fixture() {
        Order order = orders.save(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.PENDING).shippingMethod("PICKUP").paymentMethod("MOMO_DEMO").paymentStatus("UNPAID")
                .createdAt(LocalDateTime.now(clock)).paymentExpiresAt(LocalDateTime.now(clock).plusMinutes(10))
                .totalAmount(new BigDecimal("100000")).orderDetails(new ArrayList<>()).build());
        OrderDetail detail = details.save(OrderDetail.builder().order(order).product(product).quantity(1)
                .unitPrice(new BigDecimal("100000")).build());
        order.getOrderDetails().add(detail);
        StoreInventory stock = inventories.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow();
        stock.setQuantity(stock.getQuantity() - 1);
        payments.save(Payment.builder().order(order).method(PaymentMethod.MOMO_DEMO).amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING).build());
        em.flush();
        return order;
    }

    String pay(Order order) {
        String token = demo.create(List.of(order.getId()), customer.getUsername());
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
        return token;
    }

    @Test void confirmedPaymentIsHeldOnceAndPersistsWithoutBecomingRevenue() throws Exception {
        Order order = fixture();
        String token = pay(order);
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.SUCCESS);
        em.flush(); em.clear();
        Order reloaded = orders.findById(order.getId()).orElseThrow();
        Store branch = stores.findById(store.getId()).orElseThrow();
        assertThat(branch.getPendingBalance()).isEqualByComparingTo("100000");
        assertThat(branch.getEWallet()).isEqualByComparingTo("50000");
        assertThat(reloaded.getFundsStatus()).isEqualTo(OrderFundsStatus.HELD);
        assertThat(reloaded.getFundsAmount()).isEqualByComparingTo("100000");
        assertThat(reloaded.getCommissionRateSnapshot()).isEqualByComparingTo("10");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "PAYMENT_HOLD")).hasSize(1);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).isEmpty();
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isZero();
        preview("held.html");
        assertThatThrownBy(() -> {
            login(manager);
            wallet.requestWithdrawal(store, new BigDecimal("60000"), "Bank", "123456789", "Manager");
        }).hasMessageContaining("Số dư ví không đủ");
    }

    @ParameterizedTest @ValueSource(strings = {"PICKUP", "DELIVERY"})
    void fulfillmentSettlesOnceUsingThePaymentTimeCommission(String shipping) throws Exception {
        Order order = fixture();
        order.setShippingMethod(shipping);
        pay(order);
        commission.setCost(new BigDecimal("40"));
        commissions.saveAndFlush(commission);
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        if ("DELIVERY".equals(shipping)) {
            workflow.transition(order.getId(), OrderStatus.DELIVERING);
            workflow.transition(order.getId(), OrderStatus.DELIVERED);
        }
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        em.flush(); em.refresh(store);
        assertThat(store.getPendingBalance()).isZero();
        assertThat(store.getEWallet()).isEqualByComparingTo("140000");
        assertThat(store.getPoint()).isEqualTo(13);
        assertThat(order.getFundsStatus()).isEqualTo(OrderFundsStatus.SETTLED);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "HOLD_RELEASE")).singleElement()
                .satisfies(tx -> assertThat(tx.getAmount()).isEqualByComparingTo("100000"));
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).singleElement()
                .satisfies(tx -> assertThat(tx.getAmount()).isEqualByComparingTo("90000"));
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "COMMISSION_FEE")).singleElement()
                .satisfies(tx -> {
                    assertThat(tx.getAmount()).isEqualByComparingTo("10000");
                    assertThat(tx.getWalletBucket()).isEqualTo("PLATFORM");
                });
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("100000");
        preview("settled.html");
    }

    @Test void cancellationRefundsOnlyItsOwnHeldAmountAndNeverDebitsAvailableFunds() throws Exception {
        Order first = fixture(), second = fixture();
        String token = demo.create(List.of(first.getId(), second.getId()), customer.getUsername());
        demo.confirm(token);
        mvc.perform(post("/customer/orders/cancel/" + first.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(flash().attribute("successMessage", "Đã hủy đơn và hoàn tiền thành công."));
        mvc.perform(post("/customer/orders/cancel/" + first.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(store.getPendingBalance()).isEqualByComparingTo("100000");
        assertThat(store.getEWallet()).isEqualByComparingTo("50000");
        assertThat(store.getPoint()).isEqualTo(3);
        assertThat(first.getFundsStatus()).isEqualTo(OrderFundsStatus.REFUNDED);
        assertThat(second.getFundsStatus()).isEqualTo(OrderFundsStatus.HELD);
        assertThat(inventories.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(9);
        assertThat(transactions.findAllByOrderIdAndType(first.getId(), "REFUND")).singleElement()
                .satisfies(tx -> {
                    assertThat(tx.getWalletBucket()).isEqualTo("PENDING");
                    assertThat(tx.getAmount()).isEqualByComparingTo("100000");
                });
        assertThat(transactions.findAllByOrderIdAndType(first.getId(), "COMMISSION_FEE")).isEmpty();
        preview("refunded.html");
    }

    @Test void refundAfterFulfillmentReversesIncomeAndCommissionWithoutTouchingPendingFunds() {
        Order order = fixture(); pay(order);
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        login(customer); workflow.requestRefund(customer.getUsername(), order.getId());
        login(manager); workflow.transition(order.getId(), OrderStatus.REFUNDED);
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(store.getPendingBalance()).isZero();
        assertThat(store.getEWallet()).isEqualByComparingTo("50000");
        assertThat(order.getFundsStatus()).isEqualTo(OrderFundsStatus.REFUNDED);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "COMMISSION_REFUND")).hasSize(1);
    }

    @Test void unpaidCancellationNeverChangesEitherBalance() {
        Order order = fixture();
        workflow.cancelByCustomer(customer.getUsername(), order.getId());
        assertThat(order.getFundsStatus()).isEqualTo(OrderFundsStatus.NONE);
        assertThat(store.getPendingBalance()).isZero();
        assertThat(store.getEWallet()).isEqualByComparingTo("50000");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "PAYMENT_HOLD")).isEmpty();
    }

    @Test void reviewRequiredOrdersShowWarningAndCannotGenerateAnotherPayout() throws Exception {
        Order order = fixture(); pay(order);
        order.setFundsStatus(OrderFundsStatus.REVIEW_REQUIRED);
        em.flush();
        mvc.perform(get("/manager/wallet").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(model().attribute("fundsReviewCount", 1L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cần đối soát")));
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.COMPLETED)).hasMessageContaining("đối soát");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).isEmpty();
    }

    void preview(String name) throws Exception {
        var response = mvc.perform(get("/manager/wallet").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Tiền chờ giao hàng")))
                .andReturn().getResponse();
        Path directory = Path.of("target/pending-funds-preview");
        Files.createDirectories(directory);
        Files.write(directory.resolve(name), response.getContentAsByteArray());
    }
}
