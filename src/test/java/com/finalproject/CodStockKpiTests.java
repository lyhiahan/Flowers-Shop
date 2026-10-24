package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.OrderWorkflowService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CodStockKpiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired OrderWorkflowService workflow;
    @Autowired EntityManager em;
    User customer, manager, admin;
    Store branch, otherBranch;
    Product bouquet;
    CartItem item;

    @BeforeEach void fixture() {
        customer = account("cod_stock_customer", Role.USER);
        manager = account("cod_stock_manager", Role.MANAGER);
        admin = account("cod_stock_admin", Role.ADMIN);
        branch = stores.save(Store.builder().name("COD Quận 1").address("Quận 1").owner(manager)
                .status(true).isOpen(true).eWallet(BigDecimal.ZERO).build());
        otherBranch = stores.save(Store.builder().name("COD Quận 7").address("Quận 7")
                .status(true).isOpen(true).build());
        manager.setStore(branch); users.save(manager);
        Category category = categories.save(Category.builder().name("COD stock flowers").build());
        bouquet = product("COD bouquet", category, branch, 10);
        product("Other branch bouquet", category, otherBranch, 7);
        Cart cart = carts.save(Cart.builder().user(customer).items(new ArrayList<>()).build());
        item = items.save(CartItem.builder().cart(cart).product(bouquet).store(branch)
                .quantity(2).orderType(OrderType.BUY).build());
        em.flush();
    }

    User account(String name, Role role) {
        return users.save(User.builder().username(name).email(name + "@test.local").password("unused")
                .fullName(name).address("Quận 1").phone("0900000000").role(role).isActive(true).build());
    }

    Product product(String name, Category category, Store store, int quantity) {
        Product product = products.save(Product.builder().name(name).category(category).store(store)
                .priceBuy(new BigDecimal("100000")).importPrice(new BigDecimal("50000"))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        inventories.save(StoreInventory.builder().store(store).product(product).quantity(quantity).build());
        return product;
    }

    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder checkoutRequest() {
        return post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds", item.getId().toString()).param("paymentMethod", "COD")
                .param("shippingMethod", "DELIVERY").param("deliveryZone", "NEAR").param("recipientName", "Khách COD")
                .param("recipientPhone", "0900000000").param("recipientAddress", "Quận 1")
                .param("deliveryDate", LocalDate.now().plusDays(1).toString()).param("deliveryTimeSlot", "09:00-11:00");
    }

    void submit() throws Exception {
        mvc.perform(checkoutRequest())
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/orders"))
                .andExpect(flash().attributeExists("successMessage"));
        em.flush(); em.clear();
    }

    void assertStock(long own, long all) throws Exception {
        assertThat(inventories.findByStoreIdAndProductId(branch.getId(), bouquet.getId()).orElseThrow().getQuantity()).isEqualTo((int) own);
        mvc.perform(get("/manager/dashboard").param("storeId", branch.getId().toString())
                        .with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(model().attribute("currentStockQuantity", own))
                .andExpect(content().string(containsString("id=\"currentStockQuantity\"")));
        mvc.perform(get("/admin/dashboard").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(model().attribute("currentStockQuantity", all));
        mvc.perform(get("/manager/dashboard/stock").param("storeId", branch.getId().toString())
                        .with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.currentStockQuantity").value(own));
        mvc.perform(get("/admin/dashboard/stock").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(all));
        mvc.perform(get("/").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("customerStockQuantity", all))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"customerStockQuantity\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Sản phẩm còn hàng toàn chuỗi"))))
                .andExpect(content().string(containsString("/js/stock-kpi.js")));
        mvc.perform(get("/api/public/catalog-stock").param("productIds", bouquet.getId().toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.currentStockQuantity").value(all))
                .andExpect(jsonPath("$.products['" + bouquet.getId() + "'].quantity").value(own));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED"})
    void codImmediatelyReservesStockAndCancellationReturnsItExactlyOnce(OrderStatus state) throws Exception {
        assertStock(10, 17);
        submit();
        Order order = orders.findAll().getFirst();
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payments.findByOrderId(order.getId())).allMatch(p -> p.getStatus() == PaymentStatus.PENDING);
        assertStock(8, 15);
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Còn 8 sản phẩm")));
        loginManager();
        if (state != OrderStatus.PENDING) workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        em.flush();
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(post("/customer/orders/cancel/" + order.getId())
                            .with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
            em.flush(); em.clear();
            assertStock(10, 17);
        }
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PREPARING", "DELIVERING", "DELIVERED", "COMPLETED"})
    void preparationDeliveryAndCompletionDoNotDeductAgainAndBlockCustomerCancellation(OrderStatus state) throws Exception {
        submit();
        Order order = orders.findAll().getFirst();
        loginManager();
        workflow.transition(order.getId(), OrderStatus.CONFIRMED);
        workflow.transition(order.getId(), OrderStatus.PREPARING);
        if (state != OrderStatus.PREPARING) workflow.transition(order.getId(), OrderStatus.DELIVERING);
        if (state == OrderStatus.DELIVERED || state == OrderStatus.COMPLETED) workflow.transition(order.getId(), OrderStatus.DELIVERED);
        if (state == OrderStatus.COMPLETED) workflow.transition(order.getId(), OrderStatus.COMPLETED);
        em.flush(); em.clear();
        assertStock(8, 15);
        mvc.perform(post("/customer/orders/cancel/" + order.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertStock(8, 15);
    }

    @Test void stockKpiIsCurrentNotFilteredByProductCreationDate() throws Exception {
        mvc.perform(get("/manager/dashboard").with(user(manager.getUsername()).roles("MANAGER"))
                        .param("timeRange", "custom").param("startDate", "2020-01-01").param("endDate", "2020-01-02"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalProductsCount", 0L))
                .andExpect(model().attribute("currentStockQuantity", 10L));
    }

    @Test void stockEndpointDoesNotExposeAnotherBranchOrAllowCustomerAccess() throws Exception {
        mvc.perform(get("/manager/dashboard/stock").param("storeId", otherBranch.getId().toString())
                        .with(user(manager.getUsername()).roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager/dashboard/stock").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard/stock").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test void submittingCheckoutAgainDoesNotDeductStockTwice() throws Exception {
        submit();
        mvc.perform(checkoutRequest()).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.count()).isEqualTo(1);
        assertStock(8, 15);
    }

    @Test void rejectedCheckoutDoesNotDeductStockOrCreateAnOrder() throws Exception {
        item.setQuantity(11); items.saveAndFlush(item);
        mvc.perform(checkoutRequest()).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.count()).isZero();
        assertStock(10, 17);
    }

    @Test void stockSnapshotSupportsLargeTotalsAndEmptyBranches() throws Exception {
        Store empty = stores.save(Store.builder().name("Empty stock").address("Empty stock").owner(manager).build());
        Category category = bouquet.getCategory();
        product("Large stock first", category, branch, 2_000_000_000);
        product("Large stock second", category, branch, 2_000_000_000);
        em.flush();
        mvc.perform(get("/manager/dashboard/stock").param("storeId", branch.getId().toString())
                        .with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(4_000_000_010L));
        mvc.perform(get("/manager/dashboard/stock").param("storeId", empty.getId().toString())
                        .with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/manager/dashboard/stock").param("storeId", empty.getId().toString())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(0));
    }

    void loginManager() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                manager.getUsername(), "", List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }
}
