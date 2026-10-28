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
import org.springframework.mock.web.MockMultipartFile;
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
    @Autowired WalletService wallet;
    @Autowired VoucherService voucherService;
    @Autowired AuthService auth;
    @Autowired AnalyticsService analytics;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired DeliveryRepository deliveries;
    @Autowired ShippingTariffRepository tariffs;
    @Autowired ProductWishlistRepository wishlists;
    @Autowired UserFollowStoreRepository follows;
    @Autowired UserAddressRepository addresses;
    @Autowired RoleRequestRepository roleRequests;
    @Autowired MomoService momo;
    @Autowired DemoPaymentService demo;
    @Autowired NotificationRepository notifications;
    @Autowired ProductService productService;
    @Autowired Clock clock;
    Store store, otherStore;
    User customer, manager, outsider;
    Product product;
    Cart cart;
    CartItem item;

    @BeforeEach
    void setup() {
        store = stores.save(Store.builder().name("Quận 1").address("Quận 1").eWallet(BigDecimal.ZERO).build());
        otherStore = stores.save(Store.builder().name("Quận 7").address("Quận 7").build());
        customer = createUser("customer", Role.USER, null);
        createUser("admin-test", Role.ADMIN, null);
        manager = createUser("manager", Role.MANAGER, store);
        store.setOwner(manager); stores.save(store);
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

        workflow.transition(order.getId(), OrderStatus.DELIVERING);
        return order;
    }

    @Test void lockingPaidCustomerRequiresRefundAndReturnsStockOnlyOnce() throws Exception {
        User admin = createUser("account-admin", Role.ADMIN, null);
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        login(admin); workflow.confirmPayment(order.getId());
        mvc.perform(post("/admin/users/toggle-active/" + customer.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(customer.getIsActive()).isFalse();
        assertThat(customer.getAccountDisabledByAdmin()).isTrue();
        otps.save(OtpVerification.builder().user(customer).code("123456").type(OtpType.ACTIVATE_ACCOUNT)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5)).build());
        assertThat(auth.verifyOtp(customer.getEmail(), "123456")).isFalse();
        assertThatThrownBy(() -> auth.resendOtp(customer.getEmail())).hasMessageContaining("quản trị viên");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getCancellationReason()).contains("khóa", "Vui lòng hoàn tiền");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        long notificationCount = notifications.count();
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(users.findById(customer.getId())).isPresent();
        assertThat(notifications.count()).isEqualTo(notificationCount);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        mvc.perform(get("/admin/orders/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng hoàn tiền")));
        login(admin); workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test void deletingPaidMomoCustomerIsBlockedWithoutCancellingOrRefunding() throws Exception {
        User admin = createUser("account-admin", Role.ADMIN, null);
        Order order = checkout(PaymentMethod.MOMO);
        momo.simulatePayment(order.getId(), customer.getUsername());
        OrderStatus before = order.getStatus();
        int stock = inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity();
        login(admin);
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Không thể xóa tài khoản còn đơn đang xử lý hoặc chờ hoàn tiền."))
                .andExpect(flash().attributeCount(1));
        assertThat(customer.getIsActive()).isTrue();
        assertThat(users.findById(customer.getId())).isPresent();
        assertThat(order.getStatus()).isEqualTo(before);
        assertThat(order.getCancellationReason()).isNull();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(stock);
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = OrderStatus.class,
            names = {"COMPLETED", "CANCELLED", "REFUNDED", "RETURNED"}, mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE)
    void deletingCustomerWithUnfinishedOrderIsBlocked(OrderStatus state) throws Exception {
        Order order = checkout(PaymentMethod.COD);
        order.setStatus(state); orders.saveAndFlush(order);
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(customer.getIsActive()).isTrue();
        assertThat(customer.getAccountDisabledByAdmin()).isFalse();
        assertThat(order.getStatus()).isEqualTo(state);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = OrderStatus.class, names = {"COMPLETED", "CANCELLED", "REFUNDED", "RETURNED"})
    void deletingCustomerWithFinishedOrderPreservesInvoice(OrderStatus state) throws Exception {
        Order order = checkout(PaymentMethod.COD);
        order.setStatus(state); orders.saveAndFlush(order);
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã xóa tài khoản [customer] thành công."));
        assertThat(customer.getIsActive()).isFalse();
        assertThat(customer.getAccountDisabledByAdmin()).isTrue();
        assertThat(orders.findById(order.getId())).isPresent();
        assertThat(order.getStatus()).isEqualTo(state);
    }

    @Test void deletingCancelledButPaidCustomerStillRequiresRefund() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        order.setStatus(OrderStatus.CANCELLED); order.setPaymentStatus("PAID"); orders.saveAndFlush(order);
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(customer.getIsActive()).isTrue();
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test void deletingStaffRevokesMembershipWithoutChangingBranchOrders() throws Exception {
        User staff = createUser("staff-delete", Role.STAFF, store);
        StoreMembership membership = em.merge(StoreMembership.builder().user(staff).store(store).accessRole("STAFF").build());
        Order order = checkout(PaymentMethod.COD); em.flush();
        mvc.perform(post("/admin/users/delete/" + staff.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã xóa tài khoản [staff-delete] thành công."));
        assertThat(staff.getIsActive()).isFalse();
        assertThat(staff.getStore()).isNull();
        assertThat(membership.getActive()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test void deletingResponsibleManagerRequiresReplacementFirst() throws Exception {
        mvc.perform(post("/admin/users/delete/" + manager.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(manager.getIsActive()).isTrue();
        assertThat(store.getOwner()).isEqualTo(manager);
        User replacement = createUser("replacement", Role.MANAGER, store);
        store.setOwner(replacement); stores.saveAndFlush(store);
        mvc.perform(post("/admin/users/delete/" + manager.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(manager.getIsActive()).isFalse();
        assertThat(store.getOwner()).isEqualTo(replacement);
    }

    @Autowired CustomerAccountClosureService accountClosure;

    @Test void directDeletionServiceCannotBypassActiveOrderGuard() {
        Order order = checkout(PaymentMethod.COD);
        login(users.findByUsername("admin-test").orElseThrow());
        assertThatThrownBy(() -> accountClosure.disable(customer.getId(), true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đơn đang xử lý");
        assertThat(customer.getIsActive()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = Role.class, names = {"STAFF", "MANAGER"})
    void deletingWorkerWithPersonalPendingOrderIsBlocked(Role role) throws Exception {
        Order order = checkout(PaymentMethod.COD);
        customer.setRole(role); customer.setStore(store); users.saveAndFlush(customer);
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(customer.getIsActive()).isTrue();
        assertThat(customer.getStore()).isEqualTo(store);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test void deletingCustomerWithJwtSessionRetainsReferencesAndBlocksAccess() throws Exception {
        JwtSession session = em.merge(JwtSession.builder().id(UUID.randomUUID().toString()).user(customer)
                .credentialHash("x".repeat(64)).expiresAt(Instant.now().plusSeconds(3600)).build());
        em.flush();
        mvc.perform(post("/admin/users/delete/" + customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(customer.getIsActive()).isFalse();
        assertThat(em.find(JwtSession.class, session.getId())).isNotNull();
        mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test void deletingUnusedCustomerCleansCartAndRemovesAccount() throws Exception {
        Long id = customer.getId();
        mvc.perform(post("/admin/users/delete/" + id).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã xóa tài khoản [customer] thành công."));
        em.flush(); em.clear();
        assertThat(users.findById(id)).isEmpty();
        assertThat(carts.findByUserId(id)).isEmpty();
    }

    @Test void lockingCustomerCancelsUnpaidPickupButNeverCancelsHandedOverDelivery() throws Exception {
        User admin = createUser("account-admin", Role.ADMIN, null);
        Order handed = deliveryOrder();
        Cart cart = carts.findByUserId(customer.getId()).orElseThrow();
        item = items.save(CartItem.builder().cart(cart).store(store).product(product).quantity(1).orderType(OrderType.BUY).build());
        cart.getItems().add(item);
        Order pickup = checkout(PaymentMethod.COD);
        login(manager); workflow.transition(pickup.getId(), OrderStatus.CONFIRMED); workflow.transition(pickup.getId(), OrderStatus.PREPARING);
        login(admin);
        mvc.perform(post("/admin/users/toggle-active/" + customer.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(pickup.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(pickup.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(handed.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test void adminCancellationOfPaidOrderRequiresExplicitRefundAndDoesNotCreateRevenue() throws Exception {
        User admin = createUser("refund-admin", Role.ADMIN, null);
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        login(admin); workflow.confirmPayment(order.getId());
        mvc.perform(post("/admin/orders/update-status/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf())
                .param("status", "CANCELLED").param("cancellationReason", "Không đủ hoa theo mẫu đã chọn")).andExpect(status().is3xxRedirection());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getCancellationReason()).contains("quản trị viên", "Vui lòng hoàn tiền");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("0");
        login(admin); workflow.transition(order.getId(), OrderStatus.REFUNDED); workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(store.getEWallet()).isEqualByComparingTo("0");
        assertThat(order.getCancellationReason()).contains("Đã xác nhận hoàn tiền").doesNotContain("Vui lòng hoàn tiền");
        assertThat(customer.getIsActive()).isTrue();
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> manualCancellationStatesAndPayments() {
        return java.util.stream.Stream.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING)
                .flatMap(state -> java.util.stream.Stream.of(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                        .map(payment -> org.junit.jupiter.params.provider.Arguments.of(state, payment)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("manualCancellationStatesAndPayments")
    void adminCancellationSavesReasonAndNotifiesCustomerOnlyOnce(OrderStatus state, PaymentMethod method) throws Exception {
        Order order = checkout(method);
        User admin = users.findByUsername("admin-test").orElseThrow();
        login(admin);
        if (method == PaymentMethod.BANK_TRANSFER) workflow.confirmPayment(order.getId());
        if (state != OrderStatus.PENDING) workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        if (state == OrderStatus.PREPARING) workflow.transition(order.getId(), OrderStatus.PREPARING);
        String reason = "Không đủ hoa tươi theo mẫu khách đã chọn";
        mvc.perform(post("/admin/orders-manage/update-status/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf())
                        .param("status", "CANCELLED").param("cancellationReason", "  " + reason + "  "))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getCancellationReason()).contains("Đơn hàng của bạn đã bị hủy bởi admin", "với lý do: " + reason)
                .doesNotContain("  " + reason);
        assertThat(order.getCancellationReason().length()).isLessThanOrEqualTo(500);
        assertThat(order.getStatus()).isEqualTo(method == PaymentMethod.COD ? OrderStatus.CANCELLED : OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo(method == PaymentMethod.COD ? "UNPAID" : "PAID");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        var cancellationNotices = notifications.findByUserIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(n -> order.getId().equals(n.getRelatedOrderId())
                        && ("ORDER_CANCELLED".equals(n.getType()) || "REFUND_REQUESTED".equals(n.getType()))).toList();
        assertThat(cancellationNotices).hasSize(1);
        assertThat(cancellationNotices.getFirst().getContent()).isEqualTo(order.getCancellationReason());
        for (String path : List.of("/customer/orders", "/customer/orders/" + order.getId(), "/notifications")) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(reason)));
        }
        mvc.perform(post("/admin/orders-manage/update-status/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf())
                        .param("status", "CANCELLED").param("cancellationReason", "Một lý do khác"))
                .andExpect(status().is3xxRedirection());
        assertThat(order.getCancellationReason()).contains(reason).doesNotContain("Một lý do khác");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(n -> order.getId().equals(n.getRelatedOrderId())
                        && ("ORDER_CANCELLED".equals(n.getType()) || "REFUND_REQUESTED".equals(n.getType()))).count()).isEqualTo(1);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "\t\n"})
    void adminCancellationRequiresNonBlankReasonBeforeTouchingStock(String reason) throws Exception {
        Order order = checkout(PaymentMethod.COD);
        var request = post("/admin/orders-manage/update-status/" + order.getId()).with(user("admin-test").roles("ADMIN")).with(csrf())
                .param("status", "CANCELLED");
        if (reason != null) request.param("cancellationReason", reason);
        long noticeCount = notifications.count();
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("errorMessage",
                org.hamcrest.Matchers.containsString("Vui lòng nhập lý do")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCancellationReason()).isNull();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        assertThat(notifications.count()).isEqualTo(noticeCount);
    }

    @Test void manualCancellationRejectsLongReasonAndEscapesCustomerFacingHtml() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        login(users.findByUsername("admin-test").orElseThrow());
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.CANCELLED, "a".repeat(301)))
                .hasMessageContaining("300 ký tự");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        String reason = "<script>alert(1)</script>";
        workflow.transition(order.getId(), OrderStatus.CANCELLED, reason);
        for (String path : List.of("/customer/orders", "/customer/orders/" + order.getId(), "/notifications")) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(reason))));
        }
    }

    @Test void managerExceptionAlsoRequiresReasonAndRefundPreservesTheOriginalExplanation() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        login(manager); workflow.confirmPayment(order.getId());
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        String reason = "Vui lòng hoàn tiền vì hoa không đạt chất lượng";
        mvc.perform(post("/manager/orders/update-status/" + order.getId()).with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                        .param("status", "CANCELLED").param("cancellationReason", reason))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getCancellationReason()).contains("hủy bởi quản lý cửa hàng", reason);
        login(manager); workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(order.getCancellationReason()).contains(reason, "Đã xác nhận hoàn tiền");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void cancellationReasonFieldIsBoundToTheConfirmationForm() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        for (String role : List.of("admin", "manager")) {
            String route = "admin".equals(role) ? "/admin/orders-manage/" : "/manager/orders/";
            mvc.perform(get(route + order.getId()).with(user("admin".equals(role) ? "admin-test" : manager.getUsername()).roles(role.toUpperCase())))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cancellationReasonInput\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("form=\"formConfirmStatusUpdate\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("maxlength=\"300\"")));
        }
    }

    @Test void refundDeductsRevenueOnceWithoutDeductingOtherOrders() throws Exception {
        Order refunded = deliveryOrder(); login(manager);
        workflow.transition(refunded.getId(), OrderStatus.DELIVERED);
        item = items.save(CartItem.builder().cart(cart).store(store).product(product).quantity(1).orderType(OrderType.BUY).build());
        cart.getItems().add(item);
        Order kept = checkout(PaymentMethod.COD);
        login(manager); workflow.transition(kept.getId(), OrderStatus.CONFIRMED);
        workflow.transition(kept.getId(), OrderStatus.PREPARING); workflow.transition(kept.getId(), OrderStatus.COMPLETED);
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("240000");
        login(customer); workflow.requestRefund(customer.getUsername(), refunded.getId());
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("240000");
        login(manager); workflow.transition(refunded.getId(), OrderStatus.REFUNDED);
        workflow.transition(refunded.getId(), OrderStatus.REFUNDED);
        assertThat(store.getEWallet()).isEqualByComparingTo("72000");
        assertThat(store.getPoint()).isEqualTo(8);
        assertThat(transactions.findAllByOrderIdAndType(refunded.getId(), "ORDER_PAYMENT").get(0).getEarnedPoints()).isEqualTo(16);
        assertThat(transactions.findAllByOrderIdAndType(refunded.getId(), "REFUND")).hasSize(1);
        assertThat(analytics.getStoreRevenueStats(store.getId()).getTotalBuyRevenue()).isEqualByComparingTo("80000");
        var view = mvc.perform(get("/manager/dashboard").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andReturn().getModelAndView();
        assertThat((BigDecimal) view.getModel().get("flowerRevenue")).isEqualByComparingTo("80000");
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

    @Test void managerDeliveryAndCompletionCreditExactlyOnce() {
        Order order = deliveryOrder();
        workflow.transition(order.getId(), OrderStatus.DELIVERED);
        workflow.transition(order.getId(), OrderStatus.DELIVERED);
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

    @Test void customerCannotUpdateDeliveryStatus() {
        Order order = deliveryOrder(); login(customer);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.DELIVERED))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERING);
    }

    @Test void retiredRoleCannotBeRequestedOrUseTheDeliveryPortal() throws Exception {
        mvc.perform(post("/customer/role-request").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("targetRole", "SHIPPER")).andExpect(status().isNotFound());
        mvc.perform(get("/shipper/orders").with(user(customer.getUsername()).roles("SHIPPER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/shipper/orders/update-status/1").with(user(manager.getUsername()).roles("ADMIN")).with(csrf())
                .param("status", "DELIVERED")).andExpect(status().isForbidden());
        mvc.perform(get("/customer/profile").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"SHIPPER\""))));
    }

    @Test void bankTransferNeedsExplicitConfirmationBeforePickupCompletion() {
        Order order = checkout(PaymentMethod.BANK_TRANSFER); login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.COMPLETED)).hasMessageContaining("xác nhận");
    }

    @Test void prepaidDeliveryCannotStartUntilPaymentIsConfirmed() {
        for (PaymentMethod method : List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.MOMO)) {
            Order order = checkout(method); order.setShippingMethod("DELIVERY"); login(manager);
            workflow.transition(order.getId(), OrderStatus.CONFIRMED);
            workflow.transition(order.getId(), OrderStatus.PREPARING);
            assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.DELIVERING)).hasMessageContaining("thanh toán");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
            if (method == PaymentMethod.BANK_TRANSFER) workflow.confirmPayment(order.getId());
            else momo.simulatePayment(order.getId(), customer.getUsername());
            workflow.transition(order.getId(), OrderStatus.DELIVERING);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERING);
            assertThat(order.getPaymentStatus()).isEqualTo("PAID");
            item = items.save(CartItem.builder().cart(cart).product(product).store(store).quantity(2).orderType(OrderType.BUY).build());
            cart.getItems().add(item);
        }
    }

    @Test void lockedAccountCannotUseOldSessionCartCheckoutOrMockPayment() throws Exception {
        Order order = checkout(PaymentMethod.MOMO);
        customer.setIsActive(false); users.saveAndFlush(customer);
        mvc.perform(post("/checkout/momo/" + order.getId() + "/simulate").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> momo.simulatePayment(order.getId(), customer.getUsername()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> checkout(PaymentMethod.BANK_TRANSFER))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> cartService.updateQuantity(customer.getUsername(), item.getId(), 1))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test void removedCategoryCannotBeUsedToCreateProductsAndValidCreationHasStock() throws Exception {
        var form = validProductForm(); Category category = product.getCategory();
        category.setName("Danh mục đã xóa khỏi giao diện");
        category.setIsDeleted(true); categories.save(category); long count = products.count();
        mvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(category.getName()))));
        assertThat(products.findById(product.getId()).orElseThrow().getCategory().getId()).isEqualTo(category.getId());
        assertThatThrownBy(() -> productService.createProduct(form)).hasMessageContaining("Danh mục đã bị xóa");
        assertThatThrownBy(() -> productService.updateProduct(product.getId(), form)).hasMessageContaining("Danh mục đã bị xóa");
        mvc.perform(post("/manager/products/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("name", "Hoa mới").param("categoryId", category.getId().toString()).param("priceBuy", "100000").param("initialStock", "2"))
                .andExpect(status().isBadRequest());
        assertThat(products.count()).isEqualTo(count);
        category.setIsDeleted(false); categories.save(category);
        Product created = productService.createProduct(form);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), created.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test void quoteRejectsForeignItemsDeletedProductsAndInvalidPrices() {
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId(), Long.MAX_VALUE),
                "Quận 1", null, "PICKUP", store.getId(), null)).hasMessageContaining("không thuộc giỏ hàng");
        product.setIsDeleted(true);
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()),
                "Quận 1", null, "PICKUP", store.getId(), null)).hasMessageContaining("ngừng bán");
        product.setIsDeleted(false); product.setPromotionalPrice(null); product.setPriceBuy(BigDecimal.ZERO);
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()),
                "Quận 1", null, "PICKUP", store.getId(), null)).hasMessageContaining("Giá sản phẩm");
        assertThat(orders.count()).isZero();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void newCustomerCanRegisterVerifyLoginAndPlaceABankOrderWithoutDeliveryRole() throws Exception {
        var request = new com.finalproject.dto.request.RegisterRequest();
        request.setUsername("new_after_removal"); request.setEmail("new-after-removal@example.test");
        request.setPassword("newpassword"); request.setFullName("Khách hàng mới"); request.setPhone(customer.getPhone());
        request.setProvince("TP HCM"); request.setDistrict("Quận 1"); request.setDetailAddress("123 Lê Lợi");
        String otp = auth.registerCustomer(request);
        assertThat(auth.verifyOtp(request.getEmail(), otp)).isTrue();
        User created = users.findByUsername(request.getUsername()).orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.USER); assertThat(created.getIsActive()).isTrue();
        assertThat(created.getPhone()).isEqualTo(customer.getPhone());
        var detailsService = new com.finalproject.service.CustomUserDetailsService(users);
        assertThat(detailsService.loadUserByUsername(created.getEmail()).getUsername()).isEqualTo(created.getUsername());
        assertThatThrownBy(() -> detailsService.loadUserByUsername(created.getPhone()))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
        SecurityContextHolder.clearContext();
        mvc.perform(post("/perform_login").with(csrf()).param("username", request.getUsername()).param("password", request.getPassword()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(post("/api/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + created.getEmail() + "\",\"password\":\"newpassword\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        var add = new com.finalproject.dto.request.AddToCartRequest(); add.setProductId(product.getId());
        add.setStoreId(store.getId()); add.setQuantity(1); add.setOrderType(OrderType.BUY);
        cartService.addToCart(created.getUsername(), add);
        Cart newCart = carts.findByUserId(created.getId()).orElseThrow();
        Order order = orderService.checkout(created.getUsername(), newCart.getItems().stream().map(CartItem::getId).toList(),
                PaymentMethod.BANK_TRANSFER, created.getFullName(), created.getPhone(), "Quận 1", null, null,
                LocalDate.now(), "PICKUP", store.getId()).get(0);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("80000");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(9);
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

    @Test void cancellingLegacyMismatchedOrderDoesNotRestoreDuplicatedBranchStock() {
        Order order = checkout(PaymentMethod.COD);
        var duplicate = inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow();
        assertThat(duplicate.getQuantity()).isEqualTo(8);
        product.setStore(otherStore);
        products.saveAndFlush(product);
        inventory.save(StoreInventory.builder().store(otherStore).product(product).quantity(10).build());

        orderService.cancelOrder(customer.getUsername(), order.getId());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(duplicate.getQuantity()).isEqualTo(8);
        assertThat(inventory.findByStoreIdAndProductId(otherStore.getId(), product.getId()).orElseThrow().getQuantity())
                .isEqualTo(10);
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
                .createdAt(LocalDateTime.now(clock)).expiredAt(LocalDateTime.now(clock).plusMinutes(5)).build());
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), null, "newpassword")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "000000", "newpassword")).isInstanceOf(IllegalArgumentException.class);
        assertThat(encoder.matches("oldpassword", customer.getPassword())).isTrue();
    }

    @Test void validOtpIsConsumedAndNeverExposedByPublicPages() throws Exception {
        otps.save(OtpVerification.builder().user(customer).type(OtpType.RESET_PASSWORD).code("987654")
                .createdAt(LocalDateTime.now(clock)).expiredAt(LocalDateTime.now(clock).plusMinutes(5)).build());
        mvc.perform(get("/reset-password").param("email", customer.getEmail()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("987654"))));
        auth.resetPassword(customer.getEmail(), "987654", "newpassword");
        assertThat(encoder.matches("newpassword", customer.getPassword())).isTrue();
        assertThatThrownBy(() -> auth.resetPassword(customer.getEmail(), "987654", "anotherpassword")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void withdrawalReservesFundsAndRejectionRestoresThem() {
        store.setEWallet(new BigDecimal("100000")); login(manager);
        wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123456789", "Manager");
        assertThat(store.getEWallet()).isEqualByComparingTo("40000");
        Transaction tx = transactions.findAll().get(0);
        assertThat(tx.getStatus()).isEqualTo("PENDING");
        login(createUser("admin", Role.ADMIN, null)); wallet.resolveWithdrawal(tx.getId(), false);
        assertThat(store.getEWallet()).isEqualByComparingTo("100000");
        assertThat(tx.getStatus()).isEqualTo("REJECTED");
    }

    @Test void withdrawalCannotExceedAvailableFunds() {
        store.setEWallet(new BigDecimal("50000")); login(manager);
        assertThatThrownBy(() -> wallet.requestWithdrawal(new BigDecimal("60000"), "Bank", "123456789", "Manager"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(transactions.count()).isZero();
    }

    @Test void routesRenderAndRequireCsrfAndCorrectRoles() throws Exception {
        mvc.perform(get("/manager/orders").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(view().name("manager/orders"));
        mvc.perform(post("/manager/staff/remove/1").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders").with(user(customer.getUsername()).roles("USER")))
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

    @Test void pausedProductAlreadyInCartCannotCreateOrderOrReduceStock() throws Exception {
        mvc.perform(post("/manager/products/toggle-selling/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                        .param("selling", "false"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        long beforeOrders = orders.count();
        int beforeStock = inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity();
        assertThatThrownBy(() -> checkout(PaymentMethod.COD))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ngừng bán");
        assertThat(orders.count()).isEqualTo(beforeOrders);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity())
                .isEqualTo(beforeStock);
    }

    @Test void cartHeaderCountsOnlyAvailableRowsAndQuantities() throws Exception {
        Product paused = products.save(Product.builder().name("Hoa ngừng bán").category(product.getCategory()).store(store)
                .priceBuy(new BigDecimal("90000")).importPrice(BigDecimal.ZERO)
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).isSelling(false).build());
        inventory.save(StoreInventory.builder().store(store).product(paused).quantity(20).build());
        CartItem pausedItem = items.save(CartItem.builder().cart(cart).product(paused).store(store)
                .quantity(5).orderType(OrderType.BUY).build());
        cart.getItems().add(pausedItem);
        em.flush();
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectableRowCount", 1))
                .andExpect(model().attribute("totalQuantity", 2))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hoa ngừng bán")));
        inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().setQuantity(0);
        em.flush();
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectableRowCount", 0))
                .andExpect(model().attribute("totalQuantity", 0));
    }

    @Test void pausedProductIsMarkedInCartAndOnDetailBeforeCheckout() throws Exception {
        mvc.perform(post("/manager/products/toggle-selling/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                        .param("selling", "false"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectableRowCount", 0))
                .andExpect(model().attribute("totalQuantity", 0))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm tạm ngưng bán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-paused=\"true\"")));
        mvc.perform(get("/product/" + product.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm tạm ngưng bán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"detailAddToCartBtn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"detailBuyNowBtn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cách chăm sóc hoa tươi bền lâu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"review-section\"")));
        mvc.perform(get("/checkout").param("selectedItemIds", item.getId().toString())
                        .with(user(customer.getUsername()).roles("USER")))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test void availableProductDetailKeepsPurchaseCareAndReviewSections() throws Exception {
        String html = mvc.perform(get("/product/" + product.getId())
                        .with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("id=\"productStock\"", "id=\"detailAddToCartForm\"",
                "id=\"detailAddToCartBtn\"", "id=\"detailBuyNowBtn\"",
                "Cách chăm sóc hoa tươi bền lâu", "id=\"review-section\"");
        assertThat(html).doesNotContain("id=\"detailAddToCartBtn\" disabled",
                "id=\"detailBuyNowBtn\" disabled");
    }

    @Test void customerOrderCardPrioritizesProductAndExplainsTotal() throws Exception {
        product.setImageUrl("https://example.test/rose.jpg");
        Order order = checkout(PaymentMethod.COD);
        order.setShippingFee(new BigDecimal("25000"));
        order.setTotalAmount(order.getTotalAmount().add(new BigDecimal("25000")));
        orders.saveAndFlush(order);

        String html = mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("order-product-image", "https://example.test/rose.jpg",
                "Hoa hồng", "Số lượng: 2", "Tổng thanh toán", "Phí giao hàng: 25,000 đ", "Xem chi tiết");
        assertThat(html.indexOf("order-product-row")).isLessThan(html.indexOf("order-fulfillment-summary"));
    }

    @Test void productWithPendingOrderCannotBeDeletedByManagerOrAdmin() throws Exception {
        checkout(PaymentMethod.COD);
        mvc.perform(get("/admin/products-manage").with(user("admin-test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-open-orders=\"1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chi nhánh: Quận 1")));
        mvc.perform(post("/manager/products/delete/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/admin/products-manage/delete/" + product.getId())
                        .with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(products.findById(product.getId()).orElseThrow().getIsDeleted()).isFalse();
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test void deletedProductRestoresToPausedUntilManagerOpensSelling() throws Exception {
        mvc.perform(post("/manager/products/delete/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(products.findAll(StoreProductService.catalog(store.getId(), "deleted")))
                .extracting(Product::getId).contains(product.getId());
        mvc.perform(post("/manager/products/restore/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(redirectedUrl("/manager/products?tab=stored"));
        assertThat(products.findAll(StoreProductService.catalog(store.getId(), "stored")))
                .extracting(Product::getId).contains(product.getId());
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).isInstanceOf(IllegalStateException.class);
        mvc.perform(post("/manager/products/toggle-selling/" + product.getId())
                        .with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                        .param("selling", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(checkout(PaymentMethod.COD)).isNotNull();
    }

    @Test void adminCanFindDeletedProductAndRestoreItWithoutOpeningSales() throws Exception {
        mvc.perform(post("/admin/products-manage/delete/" + product.getId())
                        .with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(get("/admin/products-manage").param("tab", "DELETED")
                        .with(user("admin-test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/products-manage/restore/" + product.getId())));
        mvc.perform(post("/admin/products-manage/restore/" + product.getId())
                        .with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/products-manage?status=PAUSED"));
        Product restored = products.findById(product.getId()).orElseThrow();
        assertThat(restored.getIsDeleted()).isFalse();
        assertThat(restored.getIsSelling()).isFalse();
        assertThatThrownBy(() -> checkout(PaymentMethod.COD)).isInstanceOf(IllegalStateException.class);
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
        long notificationCount = notifications.count();
        workflow.confirmPayment(order.getId());
        assertThat(notifications.count()).isEqualTo(notificationCount);
        login(customer); orderService.cancelOrder(customer.getUsername(), order.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chờ hoàn tiền")));
    }

    @Test void staffAndCheckoutTemplatesRenderWithCsrf() throws Exception {
        User staff = createUser("staff", Role.MANAGER, store);
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk());
        mvc.perform(get("/checkout").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/transactions").with(user(createUser("admin", Role.ADMIN, null).getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test void resetOtpLocksAfterFiveFailedAttempts() {
        OtpVerification otp = otps.save(OtpVerification.builder().user(customer).type(OtpType.RESET_PASSWORD).code("123456")
                .createdAt(LocalDateTime.now(clock)).expiredAt(LocalDateTime.now(clock).plusMinutes(5)).build());
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
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("selectedItemIds", item.getId().toString()).param("deliveryUnitId", delivery.getId().toString()).param("recipientAddress", "Quận 1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Vui lòng chọn vùng giao hàng demo."));
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("selectedItemIds", item.getId().toString()).param("deliveryUnitId", delivery.getId().toString())
                        .param("deliveryZone", "NEAR").param("recipientAddress", "Quận 1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(190000));
    }

    @Test void demoDeliveryZoneDeterminesTariffWithoutGuessingDistrictName() {
        Delivery delivery = deliveries.save(Delivery.builder().name("Demo zone delivery").price(BigDecimal.ZERO).build());
        tariffs.save(ShippingTariff.builder().store(store).delivery(delivery)
                .destinationArea("Khu vực gần chi nhánh (< 5km)").serviceName("Tiêu chuẩn")
                .baseFee(new BigDecimal("25000")).build());
        tariffs.save(ShippingTariff.builder().store(store).delivery(delivery)
                .destinationArea("Khu vực xa hơn (5 - 15km)").serviceName("Tiêu chuẩn")
                .baseFee(new BigDecimal("40000")).build());
        String address = "Đường hoa, Quận 10";
        CheckoutQuote near = orderService.quote(customer.getUsername(), List.of(item.getId()), address,
                null, "DELIVERY", null, delivery.getId(), "NEAR");
        CheckoutQuote far = orderService.quote(customer.getUsername(), List.of(item.getId()), address,
                null, "DELIVERY", null, delivery.getId(), "FAR");
        assertThat(near.shippingFee()).isEqualByComparingTo("25000");
        assertThat(far.shippingFee()).isEqualByComparingTo("40000");
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), address,
                null, "DELIVERY", null, delivery.getId(), "OUTSIDE")).hasMessageContaining("ngoài phạm vi");
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), address,
                null, "DELIVERY", null, delivery.getId(), "INVALID")).hasMessageContaining("hợp lệ");
        Order order = orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Khách", "0900000000", address, null, null, LocalDate.now().plusDays(1), "DELIVERY", null,
                delivery.getId(), java.time.LocalTime.NOON, "FAR").get(0);
        assertThat(order.getShippingFee()).isEqualByComparingTo(far.shippingFee());
        assertThat(order.getTotalAmount()).isEqualByComparingTo(far.total());
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
            String html = mvc.perform(get(route).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("viewport")))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Path output = java.nio.file.Path.of("target/frontend-smoke", route.replaceAll("[^a-zA-Z0-9]", "_") + ".html");
            java.nio.file.Files.createDirectories(output.getParent());
            java.nio.file.Files.writeString(output, html);
        }
        Order order = checkout(PaymentMethod.MOMO);
        mvc.perform(get("/customer/orders").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/checkout/momo/" + order.getId())));
        mvc.perform(get("/customer/orders/detail/" + order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Phí giao hàng:"))));
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
        mvc.perform(get("/products/" + id).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(name)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(description)));
    }

    @Test void momoMockRequiresOwnerAndCsrfAndDoesNotUsePublicCallbackToPay() throws Exception {
        Order order = checkout(PaymentMethod.MOMO);
        mvc.perform(get("/checkout/momo/callback").param("orderId", order.getId().toString()).param("resultCode", "0"))
                .andExpect(status().is3xxRedirection());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content("{\"resultCode\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/checkout/momo/" + order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Mô phỏng local")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("api.qrserver.com"))));
        mvc.perform(post("/checkout/momo/" + order.getId() + "/simulate").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> momo.simulatePayment(order.getId(), "not-owner"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(post("/checkout/momo/" + order.getId() + "/simulate")
                .with(user(customer.getUsername()).roles("USER")).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        Payment paid = payments.findByOrderId(order.getId()).get(0);
        assertThat(paid.getMomoEnvironment()).isEqualTo("mock");
        assertThat(paid.getTransactionCode()).startsWith("DEV-MOMO-");
        long count = notifications.count();
        momo.simulatePayment(order.getId(), customer.getUsername());
        assertThat(notifications.count()).isEqualTo(count);
    }

    @Test void bankInstructionsUseOrderTotalAndOnlyOwnerCanReadPaymentStatus() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        String route = "/checkout/bank/" + order.getId();
        mvc.perform(get(route).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("3337110941")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BUI THANH PHUC")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("160,000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DH" + order.getId())))
                .andExpect(model().attribute("order", order));
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        for (String suffix : List.of("", "/status")) {
            mvc.perform(get(route + suffix).with(user("another-customer").roles("USER"))).andExpect(status().isForbidden());
        }
        mvc.perform(get(route + "/status").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
        login(manager); workflow.confirmPayment(order.getId());
        mvc.perform(get(route + "/status").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PAID"));
        mvc.perform(get(route).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bank-qr-source"))));
    }

    @Test void customerPaymentCompatibilityRouteCannotSelfConfirmBankTransfer() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        String route = "/customer/payment/" + order.getId();
        mvc.perform(get(route).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/checkout/bank/" + order.getId()));
        mvc.perform(post(route + "/process").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.pending").value(true));
        em.flush(); em.clear();
        assertThat(orders.findById(order.getId()).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payments.findByOrderId(order.getId())).allMatch(p -> p.getStatus() == PaymentStatus.PENDING);
        mvc.perform(post(route + "/process").with(user("another-customer").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        login(manager); workflow.confirmPayment(order.getId());
        mvc.perform(post(route + "/process").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        login(customer); orderService.cancelOrder(customer.getUsername(), order.getId());
        mvc.perform(post(route + "/process").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
    }

    @Test void removedBankCheckoutCannotCreateOrderOrReserveStock() throws Exception {
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds", item.getId().toString()).param("paymentMethod", "BANK_TRANSFER")
                .param("recipientName", "Khách").param("recipientPhone", "0900000000")
                .param("deliveryDate", LocalDate.now().plusDays(1).toString()).param("deliveryTimeSlot", "09:00-11:00")
                .param("shippingMethod", "PICKUP").param("pickupStoreId", store.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.findByUserIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    com.finalproject.dto.request.ProductForm validProductForm() {
        var form = new com.finalproject.dto.request.ProductForm();
        form.setName("Hoa kiểm thử"); form.setCategoryId(product.getCategory().getId());
        form.setImportPrice(new BigDecimal("50000")); form.setPriceBuy(new BigDecimal("80000"));
        form.setStoreId(store.getId()); form.setInitialStock(3); return form;
    }

    @Test void productServiceRejectsBadInputBeforeAnyDatabaseWrite() {
        long before = products.count();
        for (BigDecimal price : List.of(new BigDecimal("-1"), BigDecimal.ZERO, new BigDecimal("1.123"), new BigDecimal("1E30"))) {
            var form = validProductForm(); form.setPriceBuy(price); form.setConfirmSellAtLoss(true);
            assertThatThrownBy(() -> productService.createProduct(form)).isInstanceOf(IllegalArgumentException.class);
        }
        var form = validProductForm(); form.setInitialStock(-1);
        assertThatThrownBy(() -> productService.createProduct(form)).isInstanceOf(IllegalArgumentException.class);
        form.setInitialStock(3); form.setName(" ");
        assertThatThrownBy(() -> productService.createProduct(form)).isInstanceOf(IllegalArgumentException.class);
        assertThat(products.count()).isEqualTo(before);
    }

    @Test void adminProductBindingRejectsNegativeTextAndHugePricesWithoutSaving() throws Exception {
        long before = products.count();
        for (String price : List.of("-100", "0", "abc", "1.234", "999999999999999999999999")) {
            mvc.perform(post("/admin/products/create").with(user("admin-test").roles("ADMIN")).with(csrf())
                    .param("name", "Hoa invalid").param("categoryId", product.getCategory().getId().toString())
                    .param("importPrice", "50000").param("priceBuy", price).param("initialStock", "5"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "priceBuy"));
        }
        assertThat(products.count()).isEqualTo(before);
    }

    @Test void newProductRespectsBranchAndBlocksExternalReturnUrl() throws Exception {
        mvc.perform(post("/admin/products/create").with(user("admin-test").roles("ADMIN")).with(csrf())
                .param("name", "Branch product").param("categoryId", product.getCategory().getId().toString())
                .param("importPrice", "50000").param("priceBuy", "80000").param("initialStock", "3")
                .param("storeId", store.getId().toString()).param("returnUrl", "https://example.invalid/"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/products-manage"));
        Product created = products.findAll().stream().filter(p -> "Branch product".equals(p.getName())).findFirst().orElseThrow();
        assertThat(created.getStore().getId()).isEqualTo(store.getId());
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), created.getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(inventory.findByStoreIdAndProductId(otherStore.getId(), created.getId())).isEmpty();
    }

    @Test void invalidManagerPriceAndCartQuantityDoNotChangeData() throws Exception {
        mvc.perform(post("/manager/products/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("name", "Bad manager price").param("categoryId", product.getCategory().getId().toString()).param("priceBuy", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/manager/products/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("name", "Bad manager price").param("categoryId", product.getCategory().getId().toString()).param("priceBuy", "-100"))
                .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> cartService.updateQuantity(customer.getUsername(), item.getId(), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(items.existsById(item.getId())).isTrue();
        assertThatThrownBy(() -> orderService.checkout(customer.getUsername(), List.of(item.getId(), Long.MAX_VALUE), PaymentMethod.COD,
                "Khách", "0900000000", "Quận 1", null, null, LocalDate.now(), "PICKUP", store.getId())).hasMessageContaining("không thuộc");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED"})
    void customerCancellationAutomaticallyRefundsDemoMomoExactlyOnce(OrderStatus state) throws Exception {
        Order order = checkout(PaymentMethod.MOMO_DEMO);
        String token = demo.create(List.of(order.getId()), customer.getUsername());
        demo.confirm(token);
        if (state == OrderStatus.CONFIRMED) {
            login(manager);
            workflow.transition(order.getId(), state);
        }
        store.setEWallet(new BigDecimal("750000"));
        stores.saveAndFlush(store);
        for (String path : List.of("/customer/orders", "/customer/orders/detail/" + order.getId())) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("hoàn tiền tự động")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("MoMo giả lập"))));
        }
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(redirectedUrl("/customer/orders"))
                .andExpect(flash().attribute("successMessage", "Đã hủy đơn và hoàn tiền thành công."));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");
        Payment receipt = payments.findByOrderId(order.getId()).getFirst();
        assertThat(receipt.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(receipt.getRefundMethod()).isEqualTo("DEMO_AUTO");
        assertThat(receipt.getRefundedAt()).isNotNull();
        assertThat(order.getCancellationReason()).contains("Đã hủy đơn và hoàn tiền thành công.")
                .doesNotContain("chưa ghi nhận", "Vui lòng hoàn tiền");
        LocalDateTime refundedAt = receipt.getRefundedAt();
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attribute("successMessage", "Đã hủy đơn và hoàn tiền thành công."));
        assertThat(receipt.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(store.getEWallet()).isEqualByComparingTo("750000");
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).isEmpty();
        assertThat(store.getPendingBalance()).isZero();
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "REFUND")).singleElement()
                .satisfies(t -> {
                    assertThat(t.getWalletBucket()).isEqualTo("PENDING");
                    assertThat(t.getAmount()).isEqualByComparingTo(order.getTotalAmount());
                });
        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(n -> order.getId().equals(n.getRelatedOrderId()) && "REFUND_COMPLETED".equals(n.getType())).count()).isEqualTo(1);
        assertThat(demo.confirm(token).status()).isEqualTo(DemoPaymentService.Status.FAILED);
        String html = mvc.perform(get("/customer/orders/detail/" + order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("MoMo - Đã hoàn tiền").doesNotContain("Phí giao hàng:", "Giảm giá:", "MoMo giả lập",
                "id=\"confirmCancelOrderModal_" + order.getId() + "\"");
        var preview = java.nio.file.Path.of("target/demo-preview/order-refunded.html");
        java.nio.file.Files.createDirectories(preview.getParent());
        java.nio.file.Files.writeString(preview, html);
    }

    @Test void customerCancellationRequiresAListedReasonAndStoresTheSelection() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        for (String path : List.of("/customer/orders", "/customer/orders/detail/" + order.getId())) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Muốn thay đổi địa chỉ nhận hàng")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"reasonCode\"")));
        }
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "Vui lòng chọn lý do hủy đơn."));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("reasonCode", "UNKNOWN"))
                .andExpect(flash().attribute("errorMessage", "Lý do hủy đơn không hợp lệ."));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("reasonCode", "CHANGE_ADDRESS").param("source", "detail"))
                .andExpect(redirectedUrl("/customer/orders/detail/" + order.getId()))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).contains("Muốn thay đổi địa chỉ nhận hàng");
    }

    @Test void customerCancellationOtherReasonMustHaveDetails() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("reasonCode", "OTHER"))
                .andExpect(flash().attribute("errorMessage", "Vui lòng nhập lý do hủy đơn khác."));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("reasonCode", "OTHER").param("otherReason", "  Dời lịch sự kiện  "))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getCancellationReason()).contains("Lý do khác: Dời lịch sự kiện");
    }

    @Test void unpaidDemoMomoCancellationDoesNotCreateARefund() throws Exception {
        Order order = checkout(PaymentMethod.MOMO_DEMO);
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        Payment receipt = payments.findByOrderId(order.getId()).getFirst();
        assertThat(receipt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(receipt.getRefundedAt()).isNull();
    }

    @Test void automaticDemoRefundCannotBypassOwnershipOrPreparation() throws Exception {
        Order order = checkout(PaymentMethod.MOMO_DEMO);
        demo.confirm(demo.create(List.of(order.getId()), customer.getUsername()));
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(outsider.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(status().isForbidden());
        User otherCustomer = createUser("other_refund_customer", Role.USER, null);
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(otherCustomer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test void mismatchedDemoReceiptCannotRefundOrRestoreStock() throws Exception {
        Order order = checkout(PaymentMethod.MOMO_DEMO);
        demo.confirm(demo.create(List.of(order.getId()), customer.getUsername()));
        payments.findByOrderId(order.getId()).getFirst().setAmount(BigDecimal.ONE);
        em.flush();
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test void realMomoCancellationStillRequiresVerifiedRefund() throws Exception {
        Order order = checkout(PaymentMethod.MOMO);
        order.setPaymentStatus("PAID");
        payments.findByOrderId(order.getId()).getFirst().setStatus(PaymentStatus.SUCCESS);
        em.flush();
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(payments.findByOrderId(order.getId()).getFirst().getRefundedAt()).isNull();
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> cancellableStatesAndPayments() {
        return java.util.stream.Stream.of(OrderStatus.PENDING, OrderStatus.CONFIRMED)
                .flatMap(state -> java.util.stream.Stream.of(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER)
                        .map(payment -> org.junit.jupiter.params.provider.Arguments.of(state, payment)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("cancellableStatesAndPayments")
    void customerCanCancelBeforePreparationRegardlessOfAge(OrderStatus state, PaymentMethod method) throws Exception {
        Order order = checkout(method);
        login(manager);
        if (method == PaymentMethod.BANK_TRANSFER) workflow.confirmPayment(order.getId());
        if (state != OrderStatus.PENDING) workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now().minusDays(2));
        orders.saveAndFlush(order);
        assertThat(order.isCancellable()).isTrue();
        for (String path : List.of("/customer/orders", "/customer/orders/" + order.getId())) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(
                            "id=\"confirmCancelOrderModal_" + order.getId() + "\"")));
        }
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(method == PaymentMethod.BANK_TRANSFER ? OrderStatus.REFUND_REQUESTED : OrderStatus.CANCELLED);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        login(customer);
        orderService.cancelOrder(customer.getUsername(), order.getId());
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        if (method == PaymentMethod.BANK_TRANSFER) {
            assertThat(order.getPaymentStatus()).isEqualTo("PAID");
            assertThat(payments.findByOrderId(order.getId())).allMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);
            assertThat(order.getCancellationReason()).contains("chưa ghi nhận đã hoàn tiền");
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = OrderStatus.class,
            names = {"PREPARING", "DELIVERING", "DELIVERED", "COMPLETED", "PICKED_UP", "RENTING", "RETURNED", "OVERDUE", "REFUNDED"})
    void cancellationIsBlockedOncePreparationStartedOrOrderEnded(OrderStatus state) throws Exception {
        Order order = checkout(PaymentMethod.COD);
        order.setStatus(state); orders.saveAndFlush(order);
        assertThat(order.isCancellable()).isFalse();
        for (String path : List.of("/customer/orders", "/customer/orders/" + order.getId())) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                            "id=\"confirmCancelOrderModal_" + order.getId() + "\""))));
        }
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(state);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"COD,PICKUP", "COD,DELIVERY", "BANK_TRANSFER,PICKUP", "BANK_TRANSFER,DELIVERY"})
    void startingPreparationLocksCustomerCancellationButKeepsManagerFulfillment(PaymentMethod method, String shipping) throws Exception {
        Order order = checkout(method);
        order.setShippingMethod(shipping); orders.saveAndFlush(order);
        login(manager);
        if (method == PaymentMethod.BANK_TRANSFER) workflow.confirmPayment(order.getId());
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        // A customer can still have the cancellable page open while the shop starts work.
        mvc.perform(get("/customer/orders/" + order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"confirmCancelOrderModal_" + order.getId() + "\"")));
        mvc.perform(post("/manager/orders/update-status/" + order.getId()).with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                        .param("status", "PREPARING"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("successMessage",
                        "Đã cập nhật đơn #" + order.getId() + " sang trạng thái “Đang chuẩn bị hoa”."));
        for (String path : List.of("/customer/orders", "/customer/orders/" + order.getId())) {
            mvc.perform(get(path).with(user(customer.getUsername()).roles("USER")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("bạn không thể tự hủy đơn")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                            "id=\"confirmCancelOrderModal_" + order.getId() + "\""))));
        }
        mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("errorMessage",
                        org.hamcrest.Matchers.containsString("đã bắt đầu cắm hoa/chuẩn bị")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        assertThat(order.getPaymentStatus()).isEqualTo(method == PaymentMethod.COD ? "UNPAID" : "PAID");
        login(manager);
        assertThat(workflow.availableTransitions(order)).contains(OrderStatus.CANCELLED,
                "PICKUP".equals(shipping) ? OrderStatus.COMPLETED : OrderStatus.DELIVERING);
        if ("PICKUP".equals(shipping)) workflow.transition(order.getId(), OrderStatus.COMPLETED);
        else {
            workflow.transition(order.getId(), OrderStatus.DELIVERING);
            workflow.transition(order.getId(), OrderStatus.DELIVERED);
            workflow.transition(order.getId(), OrderStatus.COMPLETED);
        }
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test void confirmedOrderStillCannotBeCancelledByAnotherCustomer() {
        Order order = checkout(PaymentMethod.COD);
        login(manager); workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        login(outsider);
        assertThatThrownBy(() -> orderService.cancelOrder(outsider.getUsername(), order.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
    }

    @Test void momoMockCannotPayCancelledOrders() {
        Order order = checkout(PaymentMethod.MOMO);
        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> momo.simulatePayment(order.getId(), customer.getUsername())).hasMessageContaining("không còn chờ");
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void blankAuthFormsDoNotAuthenticateOrCreateUsers() throws Exception {
        SecurityContextHolder.clearContext();
        long count = users.count();
        for (String value : List.of("", " ")) {
            mvc.perform(post("/perform_login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()).with(csrf()).param("username", value).param("password", value))
                    .andExpect(status().is3xxRedirection()).andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
        }
        mvc.perform(post("/register").param("username", "").param("password", "").param("fullName", " ").param("email", "").param("phone", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registerRequest", "username", "password", "fullName", "email", "phone", "province", "district"));
        mvc.perform(post("/forgot-password").param("email", "")).andExpect(status().isOk());
        assertThat(users.count()).isEqualTo(count);
    }

    @Test void checkoutBlankRequiredFieldsAndMalformedSelectionNeverReserveStock() throws Exception {
        long before = orders.count();
        for (String field : List.of("recipientName", "recipientPhone", "recipientAddress", "selectedItemIds", "pickupStoreId", "paymentMethod", "deliveryDate")) {
            var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
            params.add("selectedItemIds", item.getId().toString()); params.add("paymentMethod", "BANK_TRANSFER");
            params.add("recipientName", "Khách"); params.add("recipientPhone", "0900000000"); params.add("recipientAddress", "Quận 1");
            params.add("deliveryDate", LocalDate.now().toString());
            params.add("shippingMethod", "recipientAddress".equals(field) ? "DELIVERY" : "PICKUP"); params.add("pickupStoreId", store.getId().toString());
            params.set(field, "");
            int result = mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf()).params(params))
                    .andReturn().getResponse().getStatus();
            assertThat(result).isIn(302, 400);
        }
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("productId", product.getId().toString()).param("quantity", "abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(get("/cart/remove/" + item.getId()).with(user(customer.getUsername()).roles("USER"))).andExpect(status().isMethodNotAllowed());
        assertThat(orders.count()).isEqualTo(before);
        assertThat(items.existsById(item.getId())).isTrue();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void emptyAdminProductFormAndManagerMissingPricesHaveUsefulErrors() throws Exception {
        long count = products.count();
        mvc.perform(post("/admin/products/create").with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "name", "categoryId", "priceBuy", "importPrice"));
        mvc.perform(post("/manager/products/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/checkout/momo/ipn").contentType("application/json").content("not json"))
                .andExpect(status().isBadRequest());
        assertThat(products.count()).isEqualTo(count);
    }

    @Test void paidPickupOrderOffersCompletionAndRejectsDeliveryWithoutChangingPayment() throws Exception {
        Order order = checkout(PaymentMethod.MOMO);
        momo.simulatePayment(order.getId(), customer.getUsername());
        login(manager); workflow.transition(order.getId(), OrderStatus.CONFIRMED); workflow.transition(order.getId(), OrderStatus.PREPARING);
        for (String route : List.of("/admin/orders-manage/" + order.getId(), "/manager/orders/" + order.getId())) {
            String role = route.startsWith("/admin") ? "ADMIN" : "MANAGER";
            var result = mvc.perform(get(route).with(user("ADMIN".equals(role) ? "admin-test" : manager.getUsername()).roles(role))).andExpect(status().isOk()).andReturn();
            var options = (List<?>) result.getModelAndView().getModel().get("availableStatuses");
            assertThat(options).extracting("value").contains("COMPLETED", "CANCELLED").doesNotContain("DELIVERING", "PICKED_UP", "DELIVERED");
        }
        mvc.perform(post("/admin/orders-manage/update-status/" + order.getId()).with(user("admin-test").roles("ADMIN")).with(csrf())
                .param("status", "DELIVERING")).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("nhận tại tiệm")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        login(manager); workflow.transition(order.getId(), OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test void deliveryDropdownDoesNotOfferSkippingDeliveryOrCancellingAfterHandover() throws Exception {
        Order order = deliveryOrder();
        for (String route : List.of("/admin/orders-manage/" + order.getId(), "/manager/orders/" + order.getId())) {
            String role = route.startsWith("/admin") ? "ADMIN" : "MANAGER";
            var result = mvc.perform(get(route).with(user("ADMIN".equals(role) ? "admin-test" : manager.getUsername()).roles(role))).andExpect(status().isOk()).andReturn();
            var options = (List<?>) result.getModelAndView().getModel().get("availableStatuses");
            assertThat(options).extracting("value").containsExactly("DELIVERED");
        }
        login(manager); workflow.transition(order.getId(), OrderStatus.DELIVERED);
        assertThat(workflow.availableTransitions(order)).containsExactly(OrderStatus.COMPLETED);
    }

    @Test void adminCanConfirmBankPaymentFromDetailAndCustomerSeesUpdatedStatus() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        User admin = createUser("payment-admin", Role.ADMIN, null);
        String detail = "/admin/orders-manage/" + order.getId();
        String confirm = "/admin/orders-manage/confirm-payment/" + order.getId();
        mvc.perform(get(detail).with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bankPaymentConfirmation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận đã nhận tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DH" + order.getId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("160,000")));
        mvc.perform(post(confirm).with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(confirm).with(user(customer.getUsername()).roles("USER")).with(csrf())).andExpect(status().isForbidden());
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        mvc.perform(post(confirm).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()).param("redirect", detail))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(detail)).andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        long count = notifications.count();
        mvc.perform(post(confirm).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()).param("redirect", "https://example.invalid"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/orders-manage"));
        assertThat(notifications.count()).isEqualTo(count);
        mvc.perform(get(detail).with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bankPaymentConfirmation"))));
        mvc.perform(get("/checkout/bank/" + order.getId() + "/status").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test void adminBankConfirmationIsHiddenAndRejectedForCancelledOrders() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        order.setStatus(OrderStatus.CANCELLED);
        User admin = createUser("payment-admin", Role.ADMIN, null);
        mvc.perform(get("/admin/orders-manage/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bankPaymentConfirmation"))));
        mvc.perform(post("/admin/orders-manage/confirm-payment/" + order.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void deliveryFallsBackToAStoreWithStockAndQuoteMatchesCheckout() {
        // Cross-branch fallback is valid only for a chain-wide product, not a branch-owned SKU.
        product.setStore(null); products.saveAndFlush(product);
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
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Còn 10 mẫu tại chi nhánh")));
        mvc.perform(get("/products/" + product.getId()).with(user(customer.getUsername()).roles("USER")))
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

    @Autowired CategoryService categoryService;
    @Autowired UserService userService;

    @Test void onlineOrdersHaveBankDeadlineAndExpiryRestocksOnce() {
        LocalDateTime before = LocalDateTime.now(clock);
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        assertThat(order.getPaymentExpiresAt()).isBetween(before.plusHours(24), LocalDateTime.now(clock).plusHours(24));
        assertThat(workflow.expireUnpaid(order.getId(), before)).isFalse();
        assertThat(workflow.expireUnpaid(order.getId(), order.getPaymentExpiresAt())).isTrue();
        assertThat(workflow.expireUnpaid(order.getId(), order.getPaymentExpiresAt().plusDays(1))).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentExpired()).isTrue();
        assertThat(payments.findByOrderId(order.getId()).get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void lateBankConfirmationRequestsRefundWithoutReservingStockAgain() throws Exception {
        Order order = checkout(PaymentMethod.BANK_TRANSFER);
        order.setPaymentExpiresAt(LocalDateTime.now(clock).minusSeconds(1)); orders.saveAndFlush(order);
        login(manager); workflow.confirmPayment(order.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT")).isEmpty();
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        workflow.transition(order.getId(), OrderStatus.REFUNDED);
        assertThat(store.getEWallet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test void preparingAndPaidOrdersDoNotExpire() {
        Order order = checkout(PaymentMethod.MOMO);
        assertThat(order.getPaymentExpiresAt()).isBefore(LocalDateTime.now(clock).plusMinutes(31));
        order.setStatus(OrderStatus.PREPARING); order.setPaymentExpiresAt(LocalDateTime.now(clock).minusMinutes(1)); orders.saveAndFlush(order);
        assertThat(workflow.expireUnpaid(order.getId(), LocalDateTime.now(clock))).isFalse();
        momo.simulatePayment(order.getId(),customer.getUsername());
        order.setStatus(OrderStatus.PENDING); orders.saveAndFlush(order);
        assertThat(workflow.expireUnpaid(order.getId(), LocalDateTime.now(clock))).isFalse();
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test void mockMomoCannotPayExpiredPendingOrder() {
        Order order = checkout(PaymentMethod.MOMO);
        order.setPaymentExpiresAt(LocalDateTime.now(clock).minusMinutes(1)); orders.saveAndFlush(order);
        assertThatThrownBy(() -> momo.simulatePayment(order.getId(),customer.getUsername())).hasMessageContaining("hết thời hạn");
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
    }

    @Test void codHasNoDeadlineAndApprovedSaleBelowCostIsNotOvercharged() {
        product.setImportPrice(new BigDecimal("150000")); products.saveAndFlush(product);
        Order order = checkout(PaymentMethod.COD);
        assertThat(order.getPaymentExpiresAt()).isNull();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("160000");
    }

    @Test void categoriesRejectCyclesAndHiddenAncestorsAndIncludeGrandchildren() {
        Category root=product.getCategory();
        Category child=categoryService.save(null,"Child",null,root.getId());
        Category grandchild=categoryService.save(null,"Grandchild",null,child.getId());
        assertThat(categoryService.descendants(root.getId())).extracting(Category::getId).contains(child.getId(),grandchild.getId());
        assertThatThrownBy(() -> categoryService.save(root.getId(),root.getName(),null,grandchild.getId())).hasMessageContaining("vòng lặp");
        child.setIsDeleted(true); categories.saveAndFlush(child);
        assertThat(categoryService.visible(grandchild)).isFalse();
        assertThat(categoryService.descendants(root.getId())).isEmpty();
    }

    @Test void categoryLinksRenderFromDatabaseAndLegacyGlobalProductIsSearchableInBranch() throws Exception {
        Style group=em.merge(Style.builder().name("Kiểu dáng").isDeleted(false).build());
        StyleValue value=em.merge(StyleValue.builder().name("Giỏ hoa kiểm thử").style(group).isDeleted(false).build());
        product.setStyleValues(new HashSet<>(List.of(value)));product.setStore(null);products.saveAndFlush(product);
        mvc.perform(get("/products").param("flowerStyle",value.getName()).param("storeId",store.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(product.getName())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(value.getName())));
        value.setIsDeleted(true);em.flush();
        assertThat(products.findAll(ProductSpecification.filter(null,null,null,null,null,store.getId(),value.getName(),(String)null,null,null))).isEmpty();
    }

    @Test void oldSessionCannotRetainAdminPrivilegesAndMissingActorIsRejected() throws Exception {
        mvc.perform(get("/admin/transactions").with(user(customer.getUsername()).roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/customer/profile").with(user("deleted-account").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/vouchers/delete/123").with(user("admin-test").roles("ADMIN"))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/admin/vouchers/delete/123").with(user("admin-test").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test void activationOtpLocksAfterFiveIncorrectAttemptsForPersistedAccount() {
        customer.setIsActive(false); users.saveAndFlush(customer);
        OtpVerification otp=otps.saveAndFlush(OtpVerification.builder().user(customer).code("123456").type(OtpType.ACTIVATE_ACCOUNT)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5)).build());
        for(int i=0;i<5;i++) assertThat(auth.verifyOtp(customer.getEmail(),"000000")).isFalse();
        assertThat(otp.getFailedAttempts()).isEqualTo(5);
        assertThat(auth.verifyOtp(customer.getEmail(),"123456")).isFalse();
        assertThat(customer.getIsActive()).isFalse();
    }

    @Test void atomicViewsPreserveUpdatedPrice() {
        product.setPriceBuy(new BigDecimal("130000"));products.saveAndFlush(product);
        products.incrementViewCount(product.getId());products.incrementViewCount(product.getId()); em.refresh(product);
        assertThat(product.getViewCount()).isEqualTo(2);
        assertThat(product.getPriceBuy()).isEqualByComparingTo("130000");
    }

    @Test void blankProfileFieldsAndInvalidAddressUpdateDoNotPersist() throws Exception {
        String original=customer.getFullName();
        assertThatThrownBy(() -> userService.updateProfileImages(customer.getUsername()," ","0900000000","Address",null,null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(customer.getFullName()).isEqualTo(original);
    }

    @Autowired ReviewService reviewService;
    @Autowired VendorDataSeederService vendorSeeder;

    @Test void sampleReviewsAreClearlyMarkedWithIllustrationCaption() throws Exception {
        User sample = createUser("demo_review_01", Role.USER, null);
        sample.setFullName("Khách mẫu 01 (Demo)"); sample.setIsActive(false); users.save(sample);
        Review review = Review.builder().user(sample).product(product).store(store).rating(4)
                .content("[Đánh giá mẫu] Bố cục hoa hài hòa, ảnh được dùng để minh họa giao diện thử nghiệm.")
                .imageUrl("/images/demo-rose-bouquet.jpg").build();
        em.persist(review); em.flush();
        assertThat(review.isDemo()).isTrue();
        assertThat(Review.builder().user(customer).order(Order.builder().id(99L).build()).content(review.getContent()).build().isDemo()).isFalse();
        assertThat(Review.builder().user(sample).content("Đánh giá thông thường").build().isDemo()).isFalse();
        mvc.perform(get("/product/" + product.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phần này có nội dung đánh giá mô phỏng để minh họa giao diện.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mai Anh")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Demo · Đánh giá mẫu"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Dữ liệu mẫu</small>"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Khách mẫu 01 (Demo)"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ảnh minh họa của sản phẩm, không phải ảnh khách gửi.")));
    }

    @Test void deletingCustomerWithChatPreservesHistoryAndDisablesAccount() throws Exception {
        ChatConversation conversation=em.merge(ChatConversation.builder().conversationKey("ADMIN:"+customer.getId()).kind("ADMIN")
                .customer(customer).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        ChatMessage message=em.merge(ChatMessage.builder().conversation(conversation).sender(customer).clientMessageId(UUID.randomUUID().toString())
                .content("Help with flowers").createdAt(LocalDateTime.now()).build());em.flush();
        mvc.perform(post("/admin/users/delete/"+customer.getId()).with(user("admin-test").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(users.findById(customer.getId())).isPresent();
        assertThat(customer.getIsActive()).isFalse();
        assertThat(em.find(ChatMessage.class,message.getId()).getContent()).isEqualTo("Help with flowers");
    }

    @Test void reviewUsesFulfillmentBranchForGlobalProductAndRejectsOversizedOrDuplicateContent() {
        Order order=checkout(PaymentMethod.COD);login(manager);
        workflow.transition(order.getId(),OrderStatus.CONFIRMED);workflow.transition(order.getId(),OrderStatus.PREPARING);
        workflow.transition(order.getId(),OrderStatus.COMPLETED);
        product.setStore(null);products.saveAndFlush(product);
        assertThatThrownBy(() -> reviewService.addReview(customer.getId(),product.getId(),order.getId(),5,"x".repeat(1001),null)).isInstanceOf(IllegalArgumentException.class);
        Review review=reviewService.addReview(customer.getId(),product.getId(),order.getId(),4,"Hoa đẹp, giao đúng hẹn. ".repeat(4),null);
        assertThat(review.getStore().getId()).isEqualTo(store.getId());
        assertThat(review.getStars()).isEqualTo(4);
        assertThat(store.getRating()).isEqualTo(4.0);
        assertThatThrownBy(() -> reviewService.addReview(customer.getId(),product.getId(),order.getId(),5,"Hoa đẹp, giao đúng hẹn. ".repeat(4),null)).isInstanceOf(IllegalStateException.class);
    }

    @Test void receivedOrderReviewOpensFormAndAppearsOnProductAfterSubmission() throws Exception {
        Order order = checkout(PaymentMethod.COD);
        login(manager);
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        workflow.transition(order.getId(), OrderStatus.COMPLETED);
        String reviewText = "Hoa tươi, đóng gói cẩn thận, cửa hàng chuẩn bị đúng giờ và hỗ trợ rất nhiệt tình.";

        mvc.perform(get("/customer/orders/detail/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("reviewOrderId=" + order.getId() + "#review-form")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("reviewModal_"))));
        mvc.perform(get("/product/" + product.getId()).param("reviewOrderId", order.getId().toString())
                        .with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"review-form\"")));
        mvc.perform(post("/product/" + product.getId() + "/review")
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("orderId", order.getId().toString()).param("rating", "4").param("content", "Quá ngắn"))
                .andExpect(redirectedUrl("/product/" + product.getId() + "?reviewOrderId=" + order.getId() + "#review-form"))
                .andExpect(flash().attributeExists("reviewError"));
        assertThat(reviewService.getReviewsByProduct(product.getId())).isEmpty();
        mvc.perform(multipart("/product/" + product.getId() + "/review")
                        .file(new MockMultipartFile("imageFile", "note.txt", "text/plain", "not an image".getBytes()))
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("orderId", order.getId().toString()).param("rating", "4").param("content", reviewText))
                .andExpect(flash().attributeExists("reviewError"));
        assertThat(reviewService.getReviewsByProduct(product.getId())).isEmpty();
        mvc.perform(post("/product/" + product.getId() + "/review")
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("orderId", order.getId().toString()).param("rating", "4").param("content", reviewText))
                .andExpect(redirectedUrl("/product/" + product.getId() + "#review-list"));
        mvc.perform(get("/product/" + product.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(reviewText)));
        assertThat(reviewService.canReviewOrder(customer.getId(), product.getId(), order.getId())).isFalse();
    }

    @Test void deletingDefaultAddressSelectsRemainingOneAndBlankUpdateCannotOverwrite() {
        UserAddress first=userService.addAddress(customer.getId(),"Customer","0900000000","123 Main street",true);
        UserAddress second=userService.addAddress(customer.getId(),"Customer","0900000000","456 Other street",false);
        assertThatThrownBy(() -> userService.updateAddress(second.getId(),customer.getId()," ","abc"," ",true)).isInstanceOf(IllegalArgumentException.class);
        assertThat(second.getAddressDetail()).isEqualTo("456 Other street");
        userService.deleteAddress(first.getId(),customer.getId());
        assertThat(second.getIsDefault()).isTrue();
    }

    @Test void guestCanBrowseAndSeeNamedDemoChoicesButCannotAccessPrivateChat() throws Exception {
        long accountsBefore=users.count();
        mvc.perform(get("/").with(anonymous())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sản phẩm còn hàng toàn chuỗi"))));
        mvc.perform(get("/login").with(anonymous())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Thanh Minh · Admin")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fillCreds('leminhhoa'")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("fillCreds('khachhang"))));
        mvc.perform(get("/api/chat/conversations").with(anonymous()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/cart").with(anonymous()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        assertThat(users.count()).isEqualTo(accountsBefore);
    }

    @Test void demoSeederCannotReactivateRenamedVendorByStableEmail() {
        User vendor=createUser("tranduclong",Role.MANAGER,store);
        vendor.setEmail("vendor@rentalshop.vn"); vendor.setIsActive(false); users.saveAndFlush(vendor);
        vendorSeeder.seedVendorDemoData();
        assertThat(vendor.getIsActive()).isFalse();
        assertThat(users.findByUsername("vendor_flower")).isEmpty();
    }

    @Test void demoSeederCannotReactivateExistingVendorOrReplenishStock() {
        User vendor=createUser("vendor_flower",Role.MANAGER,store);
        vendor.setIsActive(false);users.saveAndFlush(vendor);
        var stock=inventory.findByStoreIdAndProductId(store.getId(),product.getId()).orElseThrow();stock.setQuantity(0);inventory.saveAndFlush(stock);
        store.setEWallet(new BigDecimal("12345"));stores.saveAndFlush(store);
        vendorSeeder.seedVendorDemoData();
        assertThat(vendor.getIsActive()).isFalse();assertThat(stock.getQuantity()).isZero();
        assertThat(store.getEWallet()).isEqualByComparingTo("12345");
    }

    @Test void legacyNullAttributesRemainVisibleWithoutReactivatingDeletedGroups() {
        var repo=org.mockito.Mockito.mock(StyleValueRepository.class);
        Style group=Style.builder().name("Kiểu dáng cắm").isDeleted(null).build();
        StyleValue value=StyleValue.builder().name("Bó hoa").style(group).isDeleted(null).build();
        org.mockito.Mockito.when(repo.findAll()).thenReturn(List.of(value));
        ProductAttributeService service=new ProductAttributeService(repo, org.mockito.Mockito.mock(StyleRepository.class));
        assertThat(service.catalogGroups().get("Kiểu dáng")).contains(value);
        group.setIsDeleted(true);assertThat(service.activeValues()).isEmpty();
    }

    @Test void checkoutPreservesCommaInAddressAndRecipientName() throws Exception {
        Delivery delivery=deliveries.saveAndFlush(Delivery.builder().name("Comma address delivery").price(BigDecimal.ZERO).build());
        String address="123 Lê Lợi, Quận 1, TP HCM";
        String name="Nguyễn An, người nhận";
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds",item.getId().toString()).param("paymentMethod","MOMO_DEMO")
                .param("recipientName",name," ").param("recipientPhone","0900000000")
                .param("recipientAddress",address).param("shippingMethod","DELIVERY")
                .param("deliveryUnitId",delivery.getId().toString()).param("deliveryZone","NEAR")
                .param("deliveryDate",LocalDate.now().plusDays(1).toString())
                .param("deliveryTimeSlot","09:00-11:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/customer/demo-payment/*"));
        Order created=orders.findByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        assertThat(created.getRecipientAddress()).isEqualTo(address);assertThat(created.getRecipientName()).isEqualTo(name);
    }

    @Test void conflictingRecipientFieldsCannotSilentlyCreateOrder() throws Exception {
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds",item.getId().toString()).param("paymentMethod","MOMO_DEMO")
                .param("recipientName","Nguyễn An","Trần Bình").param("recipientPhone","0900000000")
                .param("shippingMethod","PICKUP").param("pickupStoreId",store.getId().toString())
                .param("deliveryDate",LocalDate.now().plusDays(1).toString()).param("deliveryTimeSlot","09:00-11:00"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.count()).isZero();
        assertThat(inventory.findByStoreIdAndProductId(store.getId(),product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void checkoutRejectsOverlongMobileOrConflictingPhoneNumbers() {
        for(String phone:List.of("09000000000","0283829123","0900000000,0910000000")) {
            assertThatThrownBy(() -> orderService.checkout(customer.getUsername(),List.of(item.getId()),PaymentMethod.COD,
                    "Customer",phone,"Quận 1",null,null,LocalDate.now(),"PICKUP",store.getId())).hasMessageContaining("điện thoại");
        }
        assertThat(orders.count()).isZero();
    }

    @Test void adminCannotCreateOrEditProductUnderHiddenAncestor() throws Exception {
        Category parent=categories.saveAndFlush(Category.builder().name("Hidden ancestor").isDeleted(true).build());
        product.getCategory().setParent(parent);categories.saveAndFlush(product.getCategory());
        var form=validProductForm();
        long count=products.count();String oldName=product.getName();
        assertThatThrownBy(() -> productService.createProduct(form)).hasMessageContaining("Danh mục cha");
        form.setName("Invalid hidden category update");
        assertThatThrownBy(() -> productService.updateProduct(product.getId(),form)).hasMessageContaining("Danh mục cha");
        assertThat(products.count()).isEqualTo(count);assertThat(product.getName()).isEqualTo(oldName);
    }

    @Test void managerCannotAddProductUnderHiddenAncestor() throws Exception {
        Category parent=categories.saveAndFlush(Category.builder().name("Hidden manager ancestor").isDeleted(true).build());
        product.getCategory().setParent(parent);categories.saveAndFlush(product.getCategory());
        long count=products.count();
        mvc.perform(post("/manager/products/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("name","Hidden category product").param("categoryId",product.getCategory().getId().toString())
                .param("priceBuy","100000")).andExpect(status().isBadRequest());
        assertThat(products.count()).isEqualTo(count);
    }

    @Test void legacyDeadlineBackfillNeverChangesPaidPreparingOrExistingDeadlines() {
        Order legacy=orders.saveAndFlush(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PENDING).paymentStatus("UNPAID").paymentMethod("MOMO").build());
        Order paid=orders.saveAndFlush(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PENDING).paymentStatus("PAID").paymentMethod("BANK_TRANSFER").build());
        Order preparing=orders.saveAndFlush(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PREPARING).paymentStatus("UNPAID").paymentMethod("BANK_TRANSFER").build());
        LocalDateTime deadline=LocalDateTime.now(clock).plusMinutes(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        assertThat(orders.initializeLegacyPaymentDeadlines(deadline,deadline.plusHours(24))).isEqualTo(1);
        em.refresh(legacy);em.refresh(paid);em.refresh(preparing);
        assertThat(legacy.getPaymentExpiresAt()).isEqualTo(deadline);
        assertThat(paid.getPaymentExpiresAt()).isNull();assertThat(preparing.getPaymentExpiresAt()).isNull();
        assertThat(orders.initializeLegacyPaymentDeadlines(deadline.plusDays(1),deadline.plusDays(2))).isZero();
        em.refresh(legacy);assertThat(legacy.getPaymentExpiresAt()).isEqualTo(deadline);
    }

    @Test
    void storeBranchDisplayAndAreaNameExtraction() {
        Store s1 = Store.builder().name("Chi Nhánh Phú Mỹ Hưng (Quận 7)").build();
        assertThat(s1.getBranchDisplayName()).isEqualTo("Chi Nhánh Phú Mỹ Hưng");
        assertThat(s1.getBranchAreaName()).isEqualTo("Quận 7");

        Store s2 = Store.builder().name("Chi Nhánh Làng Đại Học (TP. Thủ Đức)").build();
        assertThat(s2.getBranchDisplayName()).isEqualTo("Chi Nhánh Làng Đại Học");
        assertThat(s2.getBranchAreaName()).isEqualTo("TP. Thủ Đức");

        Store s3 = Store.builder().name("Chi Nhánh Bến Thành (Quận 1)").build();
        assertThat(s3.getBranchDisplayName()).isEqualTo("Chi Nhánh Bến Thành");
        assertThat(s3.getBranchAreaName()).isEqualTo("Quận 1");

        Store s4 = Store.builder().name("Cửa hàng hoa tươi").build();
        assertThat(s4.getBranchDisplayName()).isEqualTo("Cửa hàng hoa tươi");
        assertThat(s4.getBranchAreaName()).isEmpty();

        Store s5 = Store.builder().name(null).build();
        assertThat(s5.getBranchDisplayName()).isEmpty();
        assertThat(s5.getBranchAreaName()).isEmpty();
    }

    @Test
    void adminProductCreateFormRendersCorrectly() throws Exception {
        var result = mvc.perform(get("/admin/products-manage/create").with(user("admin-test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("1. Thông tin cơ bản");
        assertThat(html).contains("2. Định giá bán");
        assertThat(html).contains("Hình ảnh sản phẩm");
    }
}
