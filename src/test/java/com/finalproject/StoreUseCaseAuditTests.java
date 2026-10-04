package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regression contracts for UC_017–UC_026. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreUseCaseAuditTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CommissionRepository commissions;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired PasswordEncoder encoder;
    User owner, partner, foreignStaff;
    Store store, foreignStore;
    Commission commission;
    Category category;

    @BeforeEach void setup() {
        owner = account("audit_owner", Role.VENDOR);
        partner = account("audit_partner", Role.VENDOR);
        User otherOwner = account("audit_other", Role.VENDOR);
        store = stores.save(Store.builder().name("Audit flower shop").owner(owner)
                .address("123 Test street").phone("0901234567").status(true).isOpen(true)
                .eWallet(new BigDecimal("200000")).build());
        foreignStore = stores.save(Store.builder().name("Other flower shop").owner(otherOwner).address("456 Test street")
                .status(true).isOpen(true).build());
        owner.setStore(store); users.save(owner);
        partner.setStore(store); users.save(partner);
        foreignStaff = account("audit_foreign_staff", Role.STAFF);
        foreignStaff.setStore(foreignStore); users.save(foreignStaff);
        commission = commissions.save(Commission.builder().name("AUDIT_COMMISSION").cost(BigDecimal.TEN).build());
        category = categories.save(Category.builder().name("Audit category").build());
    }

    User account(String name, Role role) {
        return users.save(User.builder().username(name).email(name + "@test.local").fullName(name)
                .password(encoder.encode("auditPassword123")).role(role).isActive(true).build());
    }
    Product product(String name, boolean selling) {
        return products.save(Product.builder().name(name).store(store).category(category)
                .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.ZERO)
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO)
                .isActive(true).isSelling(selling).build());
    }

    @Test void everyManagerScreenRendersSharedNavigationAndActiveSection() throws Exception {
        Product flower = product("Layout flower", true);
        Order order = orders.save(Order.builder().user(owner).pickupStore(store)
                .orderType(com.finalproject.enums.OrderType.BUY).status(com.finalproject.enums.OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN).createdAt(java.time.LocalDateTime.now()).build());
        var routes = new java.util.LinkedHashMap<String, String>();
        routes.put("/manager/dashboard", "sidebarDashboard");
        routes.put("/manager/analytics", "sidebarAnalytics");
        routes.put("/manager/orders", "sidebarOrders");
        routes.put("/manager/orders/" + order.getId(), "sidebarOrders");
        routes.put("/manager/products", "sidebarProducts");
        routes.put("/manager/products/create", "sidebarProducts");
        routes.put("/manager/products/edit/" + flower.getId(), "sidebarProducts");
        routes.put("/manager/staff", "sidebarStaffs");
        routes.put("/manager/wallet", "sidebarGdCoins");
        routes.put("/manager/vouchers", "sidebarVouchers");
        routes.put("/manager/profile", "sidebarProfile");
        routes.put("/manager/stores", "sidebarStores");
        routes.put("/manager/stores/create", "sidebarStores");
        routes.put("/manager/stores/edit/" + store.getId(), "sidebarStores");
        for (var route : routes.entrySet()) {
            String html = mvc.perform(get(route.getKey()).with(user(owner.getUsername()).roles("VENDOR")))
                    .andExpect(status().isOk()).andExpect(model().attribute("managerStore", store))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).as(route.getKey()).contains("/css/manager-layout.css", "/js/manager-layout.js", "Audit flower shop");
            for (String id : List.of("managerSidebar", "managerMain", "signOutModal", "managerMenuToggle",
                    "sidebarDashboard", "sidebarOrders", "sidebarProducts", "sidebarStaffs", "sidebarGdCoins",
                    "sidebarVouchers", "sidebarAnalytics", "sidebarStores", "sidebarProfile", "sidebarNotifications")) {
                assertThat(html.split("id=\"" + id + "\"", -1).length - 1).as(route.getKey() + " " + id).isEqualTo(1);
            }
            assertThat(html.split("aria-current=\"page\"", -1).length - 1).as(route.getKey() + " active link").isEqualTo(1);
            int linkStart = html.indexOf("id=\"" + route.getValue() + "\"");
            assertThat(html.substring(linkStart, html.indexOf('>', linkStart))).contains("aria-current=\"page\"");
            assertThat(html).contains("action=\"/logout\"", "name=\"_csrf\"");
        }
    }

    @Test void uc017CreatesStoreWithSelectedCommission() throws Exception {
        mvc.perform(post("/manager/stores/create").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "New audit shop").param("phone", "0901234567").param("address", "123 Test street")
                .param("commissionId", commission.getId().toString()))
                .andExpect(redirectedUrl("/manager/dashboard")).andExpect(flash().attributeExists("successMessage"));
        Store created = stores.findByOwnerId(owner.getId()).stream().filter(s -> s.getName().equals("New audit shop")).findFirst().orElseThrow();
        assertThat(created.getCommission().getId()).isEqualTo(commission.getId());
        assertThat(created.getEWallet()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    @Test void uc017RejectsInvalidPhoneWithoutCreatingStore() throws Exception {
        long before = stores.count();
        mvc.perform(post("/manager/stores/create").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Invalid audit shop").param("phone", "123").param("address", "123 Test street"))
                .andExpect(redirectedUrl("/manager/stores/create")).andExpect(flash().attributeExists("errorMessage"));
        assertThat(stores.count()).isEqualTo(before);
    }
    @Test void uc017MustRejectDeletedCommission() throws Exception {
        commission.setIsDeleted(true); commissions.save(commission);
        long before = stores.count();
        mvc.perform(post("/manager/stores/create").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Deleted commission shop").param("phone", "0901234567").param("address", "123 Test street")
                .param("commissionId", commission.getId().toString()));
        assertThat(stores.count()).as("Deleted commission must not create a store").isEqualTo(before);
    }
    @Test void uc017RequiresLoginAndCsrf() throws Exception {
        mvc.perform(get("/manager/stores/create")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/manager/stores/create").with(user(owner.getUsername()).roles("VENDOR")))
                .andExpect(status().isForbidden());
    }
    @Test void uc018ListsOnlyAccessibleStoresAndSupportsPaging() throws Exception {
        stores.save(Store.builder().name("Second owned store").owner(owner).address("789 Test street").build());
        mvc.perform(get("/manager/stores").with(user(owner.getUsername()).roles("VENDOR")).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalElements", 2))
                .andExpect(model().attribute("totalPages", 2)).andExpect(model().attribute("currentPage", 1));
        mvc.perform(get("/manager/stores/switch/" + foreignStore.getId()).with(user(owner.getUsername()).roles("VENDOR")))
                .andExpect(status().isForbidden());
    }
    @Test void uc018BanDisablesStoreAndProducts() throws Exception {
        Product p = product("Banned flower", true);
        mvc.perform(post("/admin/stores/ban/" + store.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/stores"));
        assertThat(stores.findById(store.getId()).orElseThrow().getStatus()).isFalse();
        assertThat(com.finalproject.service.ProductAvailability.isPublic(products.findById(p.getId()).orElseThrow())).isFalse();
        assertThat(p.getIsSelling()).isTrue();
    }
    @Test void uc018LicenseMustPreserveStoredProductChoice() throws Exception {
        Product p = product("Stored flower", false);
        mvc.perform(post("/admin/stores/ban/" + store.getId()).with(user("admin").roles("ADMIN")).with(csrf()));
        mvc.perform(post("/admin/stores/approve/" + store.getId()).with(user("admin").roles("ADMIN")).with(csrf()));
        assertThat(products.findById(p.getId()).orElseThrow().getIsSelling())
                .as("License must not silently sell a previously stored product").isFalse();
    }
    @Test void uc019FeaturedImageAddEditDelete() throws Exception {
        mvc.perform(post("/manager/profile/featured/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("imageUrl", "https://example.com/flower.png")).andExpect(flash().attributeExists("successMessage"));
        assertThat(store.getFeaturedImages()).contains("flower.png");
        mvc.perform(post("/manager/profile/featured/edit").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("index", "0").param("editUrl", "https://example.com/rose.png"));
        assertThat(store.getFeaturedImages()).contains("rose.png").doesNotContain("flower.png");
        mvc.perform(post("/manager/profile/featured/delete").with(user(owner.getUsername()).roles("VENDOR")).with(csrf()).param("index", "0"));
        assertThat(store.getFeaturedImages()).isEmpty();
    }
    @Test void uc019RejectsInvalidImageFile() throws Exception {
        mvc.perform(multipart("/manager/profile/avatar")
                .file(new MockMultipartFile("avatarFile", "bad.txt", "text/plain", "not an image".getBytes()))
                .with(user(owner.getUsername()).roles("VENDOR")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(store.getAvatar()).isNull();
    }
    @Test void uc019BasicInfoSaveMustPreserveOpenStatus() throws Exception {
        mvc.perform(post("/manager/profile/info").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Renamed audit shop").param("bio", "Updated bio").param("phone", "0901234567"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(store.getName()).isEqualTo("Renamed audit shop");
        assertThat(store.getIsOpen()).as("Basic info form does not submit isOpen; saving must preserve it").isTrue();
    }
    @Test void uc020OwnerAddsAndRemovesCandidate() throws Exception {
        User candidate = account("audit_candidate", Role.CUSTOMER);
        mvc.perform(post("/manager/staff/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", candidate.getId().toString())).andExpect(flash().attributeExists("successMessage"));
        assertThat(candidate.getStore().getId()).isEqualTo(store.getId());
        assertThat(candidate.getRole()).isEqualTo(Role.STAFF);
        mvc.perform(post("/manager/staff/remove/" + candidate.getId()).with(user(owner.getUsername()).roles("VENDOR")).with(csrf()));
        assertThat(candidate.getStore()).isNull();
    }
    @Test void uc020PartnerCannotAddStaffAndCanLeave() throws Exception {
        mvc.perform(post("/manager/staff/add").with(user(partner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", foreignStaff.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/manager/staff/out-shop").with(user(partner.getUsername()).roles("VENDOR")).with(csrf()))
                .andExpect(redirectedUrl("/manager/stores"));
        assertThat(partner.getStore()).isNull();
    }
    @Test void uc020MustNotStealStaffFromAnotherStore() throws Exception {
        mvc.perform(post("/manager/staff/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", foreignStaff.getId().toString()));
        assertThat(foreignStaff.getStore().getId()).as("Foreign staff assignment must remain intact").isEqualTo(foreignStore.getId());
    }
    @Test void uc021CreatesEditsAndStoresProduct() throws Exception {
        mvc.perform(post("/manager/products/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Created audit flower").param("categoryId", category.getId().toString())
                .param("priceBuy", "100000").param("initialStock", "5")).andExpect(flash().attributeExists("successMessage"));
        Product p = products.findByStoreIdOrderByCreatedAtDesc(store.getId()).stream().filter(x -> x.getName().equals("Created audit flower")).findFirst().orElseThrow();
        mvc.perform(post("/manager/products/update/" + p.getId()).with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Edited audit flower").param("categoryId", category.getId().toString()).param("priceBuy", "120000"));
        assertThat(p.getName()).isEqualTo("Edited audit flower");
        mvc.perform(post("/manager/products/toggle-selling/" + p.getId()).with(user(owner.getUsername()).roles("VENDOR")).with(csrf()));
        assertThat(p.getIsSelling()).isFalse();
    }
    @Test void uc021SellingTabMustExcludeStoredProducts() throws Exception {
        product("Selling audit flower", true); product("Stored audit flower", false);
        var result = mvc.perform(get("/manager/products").with(user(owner.getUsername()).roles("VENDOR"))
                .param("tab", "selling").param("size", "1").param("page", "0"))
                .andExpect(status().isOk()).andReturn();
        var displayed = (List<?>) result.getModelAndView().getModel().get("products");
        assertThat(displayed).as("Selling tab with size=1 must only return the selling product").hasSize(1);
    }
    @Test void uc023MustNotAcceptStoredPasswordHashAsConfirmation() throws Exception {
        mvc.perform(post("/manager/wallet/withdraw").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("amount", "50000").param("password", owner.getPassword())
                .param("bankName", "Test bank").param("accountNumber", "123456789").param("accountHolder", "Audit owner"));
        assertThat(store.getEWallet()).as("A password hash is not a valid plaintext password").isEqualByComparingTo("200000");
    }

    @Autowired StoreMembershipRepository memberships;

    @Test void uc017RetryCreatesExactlyOneStoreAndDoesNotChangeAssignment() throws Exception {
        String key = java.util.UUID.randomUUID().toString();
        long before = stores.count();
        for (int i = 0; i < 2; i++) mvc.perform(post("/manager/stores/create")
                .with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("name", "Idempotent store").param("phone", "0901234567").param("address", "123 Test street")
                .param("commissionId", commission.getId().toString()).param("requestId", key))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(stores.count()).isEqualTo(before + 1);
        assertThat(owner.getStore().getId()).isEqualTo(store.getId());
    }

    @Test void uc020PartnerCanJoinMultipleStoresWithoutMovingLegacyAssignment() throws Exception {
        mvc.perform(post("/manager/staff/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", partner.getId().toString()).param("role", "PARTNER"))
                .andExpect(flash().attributeExists("successMessage"));
        User otherOwner = foreignStore.getOwner();
        otherOwner.setStore(foreignStore); users.save(otherOwner);
        mvc.perform(post("/manager/staff/add").with(user(otherOwner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", partner.getId().toString()).param("role", "PARTNER"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(memberships.findByUserIdAndActiveTrue(partner.getId())).hasSize(2);
        assertThat(partner.getStore().getId()).isEqualTo(store.getId());
    }

    @Test void uc020MixedInvalidBatchMustNotPartiallyAssignStaff() throws Exception {
        User candidate = account("audit_batch", Role.CUSTOMER);
        mvc.perform(post("/manager/staff/add").with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("selectedUserIds", candidate.getId().toString(), foreignStaff.getId().toString()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(candidate.getStore()).isNull();
        assertThat(memberships.findByUserIdAndStoreId(candidate.getId(), store.getId())).isEmpty();
    }

    @Test void uc018SelectionIsPerSessionAndCannotRestoreRevokedMembership() throws Exception {
        memberships.save(StoreMembership.builder().store(foreignStore).user(partner).accessRole("PARTNER").build());
        var first = new org.springframework.mock.web.MockHttpSession();
        var second = new org.springframework.mock.web.MockHttpSession();
        mvc.perform(post("/manager/stores/switch/" + foreignStore.getId()).with(csrf()).session(first)
                .with(user(partner.getUsername()).roles("VENDOR"))).andExpect(status().is3xxRedirection());
        assertThat(first.getAttribute("activeStoreId")).isEqualTo(foreignStore.getId());
        assertThat(second.getAttribute("activeStoreId")).isNull();
        var member = memberships.findByUserIdAndStoreId(partner.getId(), foreignStore.getId()).orElseThrow();
        member.setActive(false); memberships.save(member);
        mvc.perform(get("/manager/dashboard").session(first).with(user(partner.getUsername()).roles("VENDOR")))
                .andExpect(model().attribute("store", store));
        assertThat(first.getAttribute("activeStoreId")).isNull();
    }

    @Test void uc023RetryDebitsExactlyOnce() throws Exception {
        String key = java.util.UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) mvc.perform(post("/manager/wallet/withdraw")
                .with(user(owner.getUsername()).roles("VENDOR")).with(csrf())
                .param("amount", "50000").param("password", "auditPassword123")
                .param("bankName", "Bank").param("accountNumber", "123456789").param("accountHolder", "Owner")
                .param("requestId", key)).andExpect(flash().attributeExists("successMessage"));
        assertThat(store.getEWallet()).isEqualByComparingTo("150000");
    }

    @Test void uc023PartnerCannotWithdrawEvenWithOwnCorrectPassword() throws Exception {
        mvc.perform(post("/manager/wallet/withdraw").with(user(partner.getUsername()).roles("VENDOR")).with(csrf())
                .param("amount", "50000").param("password", "auditPassword123")
                .param("bankName", "Bank").param("accountNumber", "123456789").param("accountHolder", "Partner"));
        assertThat(store.getEWallet()).isEqualByComparingTo("200000");
    }

    @Test void uc019RejectsFileWithSpoofedImageContentType() throws Exception {
        mvc.perform(multipart("/manager/profile/avatar")
                .file(new MockMultipartFile("avatarFile", "fake.png", "image/png", "not an image".getBytes()))
                .with(user(owner.getUsername()).roles("VENDOR")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(store.getAvatar()).isNull();
    }

    @Test void uc018ClampsOutOfRangePageAndRejectsUnboundedSize() throws Exception {
        mvc.perform(get("/manager/stores").with(user(owner.getUsername()).roles("VENDOR")).param("page", "999"))
                .andExpect(status().isOk()).andExpect(model().attribute("currentPage", 0));
        mvc.perform(get("/manager/stores").with(user(owner.getUsername()).roles("VENDOR")).param("size", "10001"))
                .andExpect(status().isBadRequest());
    }

    @Autowired com.finalproject.service.AnalyticsService analytics;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderDetails;
    @Autowired java.time.Clock clock;

    @Test void uc024CountsActualSoldQuantitiesAndExcludesUnpaidForeignOrders() {
        var p = product("Actual bestseller", true);
        p.setSold(9999); products.save(p);
        var now = java.time.LocalDateTime.now(clock);
        var order = orders.save(Order.builder().orderType(com.finalproject.enums.OrderType.BUY).user(owner).pickupStore(store).totalAmount(new BigDecimal("300000"))
                .paymentStatus("PAID").status(com.finalproject.enums.OrderStatus.COMPLETED).createdAt(now).build());
        var detail = orderDetails.save(OrderDetail.builder().order(order).product(p).quantity(3).unitPrice(new BigDecimal("100000")).build());
        order.setOrderDetails(new java.util.ArrayList<>(List.of(detail)));
        orders.save(Order.builder().orderType(com.finalproject.enums.OrderType.BUY).user(owner).pickupStore(foreignStore).totalAmount(new BigDecimal("900000"))
                .paymentStatus("PAID").status(com.finalproject.enums.OrderStatus.COMPLETED).createdAt(now).build());
        orders.save(Order.builder().orderType(com.finalproject.enums.OrderType.BUY).user(owner).pickupStore(store).totalAmount(new BigDecimal("900000"))
                .paymentStatus("UNPAID").status(com.finalproject.enums.OrderStatus.PENDING).createdAt(now).build());
        var result = analytics.dashboard(store.getId(), "hour", "products");
        var values = (List<BigDecimal>) result.get("chartValues");
        assertThat(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("3");
        var ranks = (List<OrderDetailRepository.SalesRank>) result.get("topProducts");
        assertThat(ranks).hasSize(1); assertThat(ranks.get(0).getSold()).isEqualTo(3);
        assertThat((BigDecimal) result.get("flowerRevenue")).isEqualByComparingTo("300000");
    }

    @Test void uc025DateOnlyHistoryMustNotAppearAsMidnightRegistration() {
        long before = analytics.getNewUsersChart("hour").getTotal().longValueExact();
        users.save(User.builder().username("audit_date_only").email("date_only@test.local")
                .password("unused").fullName("Audit sample").role(Role.CUSTOMER).createdDate(java.time.LocalDate.now(clock)).build());
        assertThat(analytics.getNewUsersChart("hour").getTotal().longValueExact()).isEqualTo(before);
        assertThat(analytics.getNewUsersChart("day").getTotal()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test void uc025LimitsNewUserSampleTo1000WithinSelectedRange() {
        var now = java.time.LocalDateTime.now(clock);
        var bulk = new java.util.ArrayList<User>();
        for (int i = 0; i < 1001; i++) bulk.add(User.builder().username("audit_sample_" + i).email("sample_" + i + "@test.local")
                .password("unused").fullName("Audit sample").role(Role.CUSTOMER).createdAt(now).createdDate(now.toLocalDate()).creationTimeKnown(true).build());
        users.saveAllAndFlush(bulk);
        assertThat(analytics.getNewUsersChart("hour").getTotal()).isEqualByComparingTo("1000");
    }

    @Autowired com.finalproject.service.CartService cartService;
    @Autowired com.finalproject.service.OrderService orderService;
    @Autowired StoreInventoryRepository inventory;

    @Test void uc018BanBlocksPublicDetailCartAndQuoteEvenWithOtherStoreStock() throws Exception {
        Product p = product("Banned eligibility contract", true);
        inventory.save(StoreInventory.builder().store(store).product(p).quantity(5).build());
        inventory.save(StoreInventory.builder().store(foreignStore).product(p).quantity(5).build());
        var request = new com.finalproject.dto.request.AddToCartRequest(); request.setProductId(p.getId());
        request.setStoreId(foreignStore.getId());
        cartService.addToCart(owner.getUsername(), request);
        store.setStatus(false); stores.saveAndFlush(store);
        mvc.perform(get("/products/" + p.getId())).andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cartService.addToCart(owner.getUsername(), request))
                .isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orderService.quote(owner.getUsername(), null, null, null, "PICKUP", foreignStore.getId(), null))
                .isInstanceOf(IllegalStateException.class);
    }
}
