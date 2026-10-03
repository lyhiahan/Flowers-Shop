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
    @Autowired CartService cartService;
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
    @Autowired DeliveryRepository deliveries;
    @Autowired ProductWishlistRepository wishlists;
    @Autowired UserFollowStoreRepository follows;
    @Autowired UserAddressRepository addresses;
    @Autowired RoleRequestRepository roleRequests;
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

    @Test void quoteAndOrderUseTheSameShippingPriceWithoutMutatingCart() {
        Delivery delivery = deliveries.save(Delivery.builder().name("Quote delivery").price(new BigDecimal("30000")).build());
        CheckoutQuote quote = orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, delivery.getId());
        assertThat(quote.subtotal()).isEqualByComparingTo("160000");
        assertThat(quote.shippingFee()).isEqualByComparingTo("30000");
        assertThat(quote.total()).isEqualByComparingTo("190000");
        assertThat(items.findById(item.getId())).isPresent();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        Order order = orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "DELIVERY", null, delivery.getId()).get(0);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(quote.total());
        assertThat(order.getShippingFee()).isEqualByComparingTo(quote.shippingFee());
        assertThat(order.getDeliveryUnitId()).isEqualTo(delivery.getId());
        delivery.setPrice(new BigDecimal("99000"));
        deliveries.save(delivery);
        assertThat(order.getShippingFee()).isEqualByComparingTo("30000");
    }

    @Test void freeShippingVoucherAlsoWorksWhenProductHasNoMargin() {
        product.setImportPrice(product.getSellingPrice());
        products.save(product);
        Delivery delivery = deliveries.save(Delivery.builder().name("Free ship delivery").price(new BigDecimal("30000")).build());
        Voucher voucher = vouchers.save(Voucher.builder().code("SHIPQUOTE").name("Free shipping").discountType("FREE_SHIP")
                .discountValue(BigDecimal.ZERO).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build());
        CheckoutQuote quote = orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", voucher.getCode(), "DELIVERY", null, delivery.getId());
        assertThat(quote.discount()).isEqualByComparingTo("30000");
        assertThat(quote.total()).isEqualByComparingTo("160000");
        assertThat(voucher.getUsedCount()).isZero();
        Order order = orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, voucher.getCode(), LocalDate.now(), "DELIVERY", null, delivery.getId()).get(0);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(quote.total());
        assertThat(order.getDiscountAmount()).isEqualByComparingTo(quote.discount());
        assertThat(voucher.getUsedCount()).isEqualTo(1);
    }

    @Test void quoteRejectsInactiveDeliveryAndWrongBranchVoucherButPickupHasNoShipping() {
        Delivery delivery = deliveries.save(Delivery.builder().name("Inactive quote delivery").isActive(false).price(new BigDecimal("30000")).build());
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, delivery.getId()))
                .hasMessageContaining("không khả dụng");
        CheckoutQuote pickup = orderService.quote(customer.getUsername(), List.of(item.getId()), null, null, "PICKUP", store.getId(), delivery.getId());
        assertThat(pickup.shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        vouchers.save(Voucher.builder().code("QUOTEOTHER").name("Other branch").store(otherStore).discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.TEN).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build());
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), null, "QUOTEOTHER", "PICKUP", store.getId(), null))
                .hasMessageContaining("chi nhánh");
    }

    @Test void quoteEndpointRequiresCsrfAndDeliverySelection() throws Exception {
        Delivery delivery = deliveries.save(Delivery.builder().name("Endpoint delivery").price(new BigDecimal("30000")).build());
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf())
                        .param("selectedItemIds", item.getId().toString()).param("deliveryUnitId", delivery.getId().toString()).param("recipientAddress", "Quận 1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(190000));
    }

    @Test void repairedPagesRenderWithRealModelsAndRetainPaymentActions() throws Exception {
        wishlists.save(ProductWishlist.builder().user(customer).product(product).build());
        follows.save(UserFollowStore.builder().user(customer).store(store).build());
        addresses.save(UserAddress.builder().user(customer).recipientName("Người nhận").phone("0900000000").addressDetail("Quận 1").isDefault(true).build());
        roleRequests.save(RoleRequest.builder().user(customer).targetRole(Role.MANAGER).status(RoleRequestStatus.PENDING).targetStore(store).phone("0900000000").build());
        for (String route : List.of("/", "/customer/profile", "/customer/profile?tab=password", "/customer/profile?tab=roles",
                "/customer/profile/wishlist", "/customer/profile/recently-viewed", "/customer/profile/followed-stores",
                "/users", "/users/" + customer.getId(), "/products/" + product.getId(), "/stores", "/stores/" + store.getId(),
                "/checkout", "/customer/orders", "/customer/following", "/notifications")) {
            String html = mvc.perform(get(route).with(user(customer.getUsername()).roles("CUSTOMER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("viewport")))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Path output = java.nio.file.Path.of("target/frontend-smoke", route.replaceAll("[^a-zA-Z0-9]", "_") + ".html");
            java.nio.file.Files.createDirectories(output.getParent());
            java.nio.file.Files.writeString(output, html);
        }
        Order order = checkout(PaymentMethod.MOMO);
        mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/checkout/momo/" + order.getId())));
        mvc.perform(get("/customer/orders/detail/" + order.getId()).with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Phí giao hàng")));
    }

    @Test void productVietnameseTextSurvivesDatabaseReloadAndHtmlRendering() throws Exception {
        String name = "Bó Hoa Hướng Dương Tốt Nghiệp Khát Vọng Việt";
        String description = "Hộp hoa hồng, thiên thần ngọt ngào — đủ dấu ă â ê ô ơ ư đ và ạ ệ ộ ợ ự.";
        product.setName(name);
        product.setDescription(description);
        products.saveAndFlush(product);
        Long id = product.getId();
        em.clear();
        Product loaded = products.findById(id).orElseThrow();
        assertThat(loaded.getName()).isEqualTo(name);
        assertThat(loaded.getDescription()).isEqualTo(description);
        mvc.perform(get("/products/" + id).with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(name)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(description)));
    }

    @Test void deliveryFallsBackToAStoreWithStockAndQuoteMatchesCheckout() {
        inventory.deleteAll();
        inventory.saveAndFlush(StoreInventory.builder().store(otherStore).product(product).quantity(5).build());
        CheckoutQuote quote = orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, null);
        assertThat(quote.storeId()).isEqualTo(otherStore.getId());
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), null, null, "PICKUP", store.getId(), null))
                .hasMessageContaining("không có hàng");
        Order placed = orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "DELIVERY", null, null).get(0);
        assertThat(placed.getPickupStore().getId()).isEqualTo(quote.storeId());
        assertThat(inventory.findByStoreIdAndProductId(otherStore.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test void deliveryRequiresCombinedStockForDuplicateProductLines() {
        item.setQuantity(6);
        items.saveAndFlush(CartItem.builder().cart(cart).store(store).product(product).quantity(6).orderType(OrderType.BUY).build());
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), null, "Quận 1", null, "DELIVERY", null, null))
                .hasMessageContaining("tồn kho");
    }

    @Test void cartAndProductShowRealStockAndServerRejectsExcessQuantity() throws Exception {
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Còn 10 mẫu tại chi nhánh")));
        mvc.perform(get("/products/" + product.getId()).with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Còn 10 mẫu tại chi nhánh")));
        assertThatThrownBy(() -> cartService.updateQuantity(customer.getUsername(), item.getId(), 11)).hasMessageContaining("tồn kho");
        var request = new com.finalproject.dto.request.AddToCartRequest();
        request.setProductId(product.getId()); request.setStoreId(store.getId()); request.setQuantity(9);
        assertThatThrownBy(() -> cartService.addToCart(customer.getUsername(), request)).hasMessageContaining("tồn kho");
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test void demoStoreLookupHandlesDuplicateNamesWithoutDeletingStores() {
        Store duplicate = stores.saveAndFlush(Store.builder().name(store.getName()).address("Other address").build());
        assertThat(stores.findFirstByNameOrderByIdAsc(store.getName()).orElseThrow().getId()).isEqualTo(store.getId());
        assertThat(stores.existsById(duplicate.getId())).isTrue();
    }

    @Test void adminTransactionsHaveStableOrderAndValidatePageSize() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2099, 1, 1, 0, 0);
        Transaction first = transactions.saveAndFlush(Transaction.builder().user(customer).amount(BigDecimal.ONE)
                .description("Unicode transaction test").createdAt(timestamp).build());
        Transaction second = transactions.saveAndFlush(Transaction.builder().user(customer).amount(BigDecimal.TEN)
                .description("Unicode transaction test").createdAt(timestamp).build());
        var page = transactions.findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.PageRequest.of(0, 2));
        assertThat(page.getContent()).extracting(Transaction::getId).containsExactly(second.getId(), first.getId());
        var filtered = transactions.findByDescriptionContainingIgnoreCaseOrderByCreatedAtDescIdDesc(
                "unicode transaction test", org.springframework.data.domain.PageRequest.of(0, 2));
        assertThat(filtered.getContent()).extracting(Transaction::getId).containsExactly(second.getId(), first.getId());
        for (String suffix : List.of("?size=0", "?size=1000", "?keyword=unicode%20transaction%20test")) {
            mvc.perform(get("/admin/transactions" + suffix).with(user("admin-test").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("dataLoadError"));
        }
    }

    @Test void checkoutRejectsAnotherStoresVoucher() {
        vouchers.save(Voucher.builder().code("OTHER").name("Other store").store(otherStore).discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.TEN).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build());
        assertThatThrownBy(() -> orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, "OTHER", LocalDate.now(), "PICKUP", store.getId()))
                .hasMessageContaining("chi nhánh");
    }
}
