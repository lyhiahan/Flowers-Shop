package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
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
class BusinessRegressionTests {
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventory;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired TransactionRepository transactions;
    @Autowired VoucherRepository vouchers;
    @Autowired OtpVerificationRepository otps;
    @Autowired OrderService orderService;
    @Autowired OrderWorkflowService workflow;
    @Autowired ShipperService shipperService;
    @Autowired WalletService wallet;
    @Autowired VoucherService voucherService;
    @Autowired AuthService auth;
    @Autowired AnalyticsService analytics;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    Store store, otherStore;
    User customer, manager, shipper, outsider;
    Product product;
    Cart cart;
    CartItem item;

    @BeforeEach
    void setup() {
        store = stores.save(Store.builder().name("Quận 1").address("Quận 1").eWallet(BigDecimal.ZERO).build());
        otherStore = stores.save(Store.builder().name("Quận 7").address("Quận 7").build());
        customer = createUser("customer", Role.CUSTOMER, null);
        manager = createUser("manager", Role.MANAGER, store);
        shipper = createUser("shipper", Role.SHIPPER, store);
        outsider = createUser("outsider", Role.MANAGER, otherStore);
        Category category = categories.save(Category.builder().name("Hoa").build());
        product = products.save(Product.builder().name("Hoa hồng").category(category).store(store)
                .importPrice(new BigDecimal("50000")).priceBuy(new BigDecimal("100000"))
                .promotionalPrice(new BigDecimal("80000")).priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        inventory.save(StoreInventory.builder().store(store).product(product).quantity(10).build());
        cart = carts.save(Cart.builder().user(customer).items(new ArrayList<>()).build());
        item = items.save(CartItem.builder().cart(cart).product(product).store(store).quantity(2).orderType(OrderType.BUY).build());
        cart.getItems().add(item);
        login(customer);
    }

    User createUser(String name, Role role, Store branch) {
        return users.save(User.builder().username(name).email(name + "@test.local").fullName(name)
                .password(encoder.encode("oldpassword")).role(role).store(branch).isActive(true)
                .address("Quận 1").phone("0900000000").createdDate(LocalDate.now()).build());
    }

    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(), "", List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    Order checkout(PaymentMethod method) {
        return orderService.checkout(customer.getUsername(), List.of(item.getId()), method, "Khách", "0900000000",
                "Quận 1", null, null, LocalDate.now(), "PICKUP", store.getId()).get(0);
    }

    Order deliveryOrder() {
        Order order = checkout(PaymentMethod.COD);
        order.setShippingMethod("DELIVERY");
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        orderService.assignShipper(order.getId(), shipper.getId());
        login(shipper);
        workflow.transition(order.getId(), OrderStatus.DELIVERING);
        return order;
    }

    @Test void checkoutReservesRealStockAndLeavesBankPaymentPending() {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payments.findByOrderId(order.getId())).singleElement().satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(p.getPaidAt()).isNull();
        });
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        assertThat(cart.getItems()).isEmpty();
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).isInstanceOf(IllegalStateException.class);
    }

    @Test void insufficientStockNeverCreatesAnOrderOrRestocks() {
        item.setQuantity(11);
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).hasMessageContaining("tồn kho");
        assertThat(orders.count()).isZero();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void missingInventoryIsRejected() {
        inventory.deleteAll(); em.flush();
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).hasMessageContaining("không có hàng");
        assertThat(inventory.count()).isZero();
    }

    @Test void paymentGatewaysAndClosedStoresAreRejected() {
        assertThatThrownBy(() -> checkout(PaymentMethod.VNPAY)).isInstanceOf(IllegalArgumentException.class);
        store.setIsOpen(false);
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).hasMessageContaining("không nhận đơn");
    }

    @Test void shipperDeliveryAndManagerCompletionCreditExactlyOnce() {
        Order order = deliveryOrder();
        shipperService.updateDeliveryStatus(order.getId(), shipper.getId(), OrderStatus.DELIVERED);
        shipperService.updateDeliveryStatus(order.getId(), shipper.getId(), OrderStatus.DELIVERED);
        login(manager);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        assertThat(store.getEWallet()).isEqualByComparingTo("144000");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).hasSize(1);
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.PENDING)).isInstanceOf(IllegalStateException.class);
    }

    @Test void managerCannotChangeAnotherStoresOrder() {
        Order order = checkout(PaymentMethod.COD); login(outsider);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.CONFIRMED))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test void cannotAssignCustomerAsShipper() {
        Order order = checkout(PaymentMethod.COD); login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        order.setShippingMethod("DELIVERY");
        assertThatThrownBy(() -> orderService.assignShipper(order.getId(), customer.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void bankTransferNeedsExplicitConfirmationBeforePickupCompletion() {
        Order order = checkout(PaymentMethod.BANK_TRANSFER); login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.COMPLETED)).hasMessageContaining("xác nhận");
    }

    @Test void confirmedBankPaymentCanCompletePickup() {
        Order order = checkout(PaymentMethod.BANK_TRANSFER); login(manager);
        workflow.confirmPayment(order.getId());
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(store.getEWallet()).isEqualByComparingTo("144000");
    }

    @Test void cancellationRestoresStockOnce() {
        Order order = checkout(PaymentMethod.COD);
        orderService.cancelOrder(customer.getUsername(), order.getId());
        orderService.cancelOrder(customer.getUsername(), order.getId());
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test void refundRequestDoesNotPretendThatMoneyHasBeenReturned() {
        Order order = deliveryOrder(); workflow.transition(order.getId(), OrderStatus.DELIVERED);
        login(customer); orderService.requestRefund(customer.getUsername(), order.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(store.getEWallet()).isEqualByComparingTo("144000");
        login(manager); workflow.transition(order.getId(), OrderStatus.REFUNDED);
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(store.getEWallet()).isEqualByComparingTo("0");
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test void voucherValidationChecksExpiryUsageAndShipping() {
        Voucher voucher = Voucher.builder().code("TEST").name("Test").discountType("FREE_SHIP")
                .discountValue(BigDecimal.ZERO).startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1)).build();
        assertThat(voucherService.calculateDiscount(voucher, new BigDecimal("100000"), BigDecimal.ZERO)).isEqualByComparingTo("0");
        voucher.setEndDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> voucherService.calculateDiscount(voucher, BigDecimal.TEN, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        voucher.setEndDate(LocalDate.now().plusDays(1)); voucher.setUsedCount(voucher.getQuantity());
        assertThatThrownBy(() -> voucherService.calculateDiscount(voucher, BigDecimal.TEN, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void resetPasswordRejectsMissingAndIncorrectOtp() {
        otps.save(OtpVerification.builder().user(customer).type(OtpType.RESET_PASSWORD).code("123456")
                .createdAt(LocalDateTime.now()).expiredAt(LocalDateTime.now().plusMinutes(5)).build());
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), null, "newpassword")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "000000", "newpassword")).isInstanceOf(IllegalArgumentException.class);
        assertThat(encoder.matches("oldpassword", customer.getPassword())).isTrue();
    }

    @Test void validOtpIsConsumedAndNeverExposedByPublicPages() throws Exception {
        otps.save(OtpVerification.builder().user(customer).type(OtpType.RESET_PASSWORD).code("987654")
                .createdAt(LocalDateTime.now()).expiredAt(LocalDateTime.now().plusMinutes(5)).build());
        mvc.perform(get("/reset-password").param("email", customer.getEmail()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("987654"))));
        auth.resetPassword(customer.getEmail(), "987654", "newpassword");
        assertThat(encoder.matches("newpassword", customer.getPassword())).isTrue();
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "987654", "anotherpassword")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void withdrawalReservesFundsAndRejectionRestoresThem() {
        store.setEWallet(new BigDecimal("100000")); login(manager);
        wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123", "Manager");
        assertThat(store.getEWallet()).isEqualByComparingTo("40000");
        Transaction tx = transactions.findAll().get(0);
        assertThat(tx.getStatus()).isEqualTo("PENDING");
        login(createUser("admin", Role.ADMIN, null)); wallet.resolveWithdrawal(tx.getId(), false);
        assertThat(store.getEWallet()).isEqualByComparingTo("100000");
        assertThat(tx.getStatus()).isEqualTo("REJECTED");
    }

    @Test void withdrawalCannotExceedAvailableFunds() {
        store.setEWallet(new BigDecimal("50000")); login(manager);
        assertThatThrownBy(() -> wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123", "Manager"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(transactions.count()).isZero();
    }

    @Test void routesRenderAndRequireCsrfAndCorrectRoles() throws Exception {
        mvc.perform(get("/manager/orders").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(view().name("manager/orders"));
        mvc.perform(post("/manager/staff/remove/1").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/manager/products").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test void crossStoreProductMutationIsForbidden() throws Exception {
        mvc.perform(post("/manager/products/toggle-selling/" + product.getId())
                        .with(user(outsider.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(product.getIsSelling()).isTrue();
    }

    @Test void newStoreHasNoInventedRevenue() {
        assertThat(analytics.getRevenueChart("day", otherStore.getId()).getTotal()).isEqualByComparingTo("0");
        assertThat(analytics.getProductRevenueChart(otherStore.getId()).getData()).isEmpty();
        checkout(PaymentMethod.COD);
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("0");
    }

    @Test void renderedOrderPagesShowPendingRefundAndPaymentActions() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        mvc.perform(get("/manager/orders").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("confirm-payment/" + order.getId())));
        login(manager); workflow.confirmPayment(order.getId());
        login(customer); orderService.cancelOrder(customer.getUsername(), order.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chờ hoàn tiền")));
    }

    @Test void staffAndCheckoutTemplatesRenderWithCsrf() throws Exception {
        User staff = createUser("staff", Role.STAFF, store);
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/checkout").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/transactions").with(user(createUser("admin", Role.ADMIN, null).getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test void resetOtpLocksAfterFiveFailedAttempts() {
        OtpVerification otp = otps.save(OtpVerification.builder().user(customer).type(OtpType.RESET_PASSWORD).code("123456")
                .createdAt(LocalDateTime.now()).expiredAt(LocalDateTime.now().plusMinutes(5)).build());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "000000", "newpassword")).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(otp.getFailedAttempts()).isEqualTo(5);
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "123456", "newpassword")).hasMessageContaining("bị khóa");
    }

    @Test void checkoutRejectsAnotherStoresVoucher() {
        vouchers.save(Voucher.builder().code("OTHER").name("Other store").store(otherStore).discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.TEN).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build());
        assertThatThrownBy(() -> orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, "OTHER", LocalDate.now(), "PICKUP", store.getId()))
                .hasMessageContaining("chi nhánh");
    }
}
