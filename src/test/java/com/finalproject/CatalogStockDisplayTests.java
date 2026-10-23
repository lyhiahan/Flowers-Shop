package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.CatalogStockService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogStockDisplayTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired CatalogStockService stockService;
    @Autowired com.finalproject.service.OrderWorkflowService workflow;
    @Autowired OrderRepository orders;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired com.finalproject.service.OrderService orderService;
    Store first, second, closed, inactive;
    Product available, soldOut, global, missingInventory;
    Category category;
    User customer;

    @Test void pausedProductStaysViewableAndCannotBePurchasedFromCatalogOrOldCart() throws Exception {
        var cart = carts.save(Cart.builder().user(customer).build());
        var line = cartItems.save(CartItem.builder().cart(cart).product(available).store(first)
                .orderType(com.finalproject.enums.OrderType.BUY).quantity(1).build());
        cart.getItems().add(line);
        available.setIsSelling(false);
        products.saveAndFlush(available);

        String html = mvc.perform(get("/products").param("keyword", available.getName()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Sản phẩm tạm ngưng bán");
        assertThat(cartForm(html, available.getId())).containsPattern("<button[^>]*disabled");
        mvc.perform(get("/products/" + available.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/product-detail"))
                .andExpect(model().attribute("salePaused", true));
        String cartHtml = mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(cartHtml).contains("cart-paused-tag sale-paused-tag", "Sản phẩm tạm ngưng bán")
                .containsPattern("<input[^>]*class=\"[^\"]*item-check[^\"]*\"[^>]*disabled");
        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("selectedItemIds", line.getId().toString()).param("shippingMethod", "PICKUP")
                        .param("pickupStoreId", first.getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("selectedItemIds", line.getId().toString()).param("paymentMethod", "COD")
                        .param("recipientName", "Người nhận").param("recipientPhone", "0901234567")
                        .param("shippingMethod", "PICKUP").param("pickupStoreId", first.getId().toString())
                        .param("deliveryDate", java.time.LocalDate.now().plusDays(1).toString()).param("deliveryTime", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("ngừng bán")));
        assertThat(inventories.findByStoreIdAndProductId(first.getId(), available.getId()).orElseThrow().getQuantity()).isEqualTo(17);
    }

    @Test void branchPauseDoesNotBlockOtherBranchesAndResumingRestoresPurchase() {
        var row = inventories.findByStoreIdAndProductId(first.getId(), global.getId()).orElseThrow();
        row.setIsSelling(false); inventories.saveAndFlush(row);
        assertThat(stockService.summarizeIds(List.of(global.getId()), first.getId()).get(global.getId()).paused()).isTrue();
        var other = stockService.summarizeIds(List.of(global.getId()), null).get(global.getId());
        assertThat(other.paused()).isFalse();
        assertThat(other.quantity()).isEqualTo(4);
        row.setIsSelling(true); inventories.saveAndFlush(row);
        assertThat(stockService.summarizeIds(List.of(global.getId()), first.getId()).get(global.getId()).paused()).isFalse();
    }

    @Test void orderPlacedBeforePauseCanStillBeConfirmedPreparedAndCompleted() {
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                customer.getUsername(), "unused", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var cart = carts.save(Cart.builder().user(customer).build());
            var line = cartItems.save(CartItem.builder().cart(cart).product(available).store(first)
                    .orderType(com.finalproject.enums.OrderType.BUY).quantity(1).build());
            cart.getItems().add(line);
            var placed = orderService.checkout(customer.getUsername(), List.of(line.getId()), com.finalproject.enums.PaymentMethod.COD,
                    "Người nhận", "0901234567", "Địa chỉ thử", null, null, java.time.LocalDate.now().plusDays(1), "DELIVERY", null).get(0);
            available.setIsSelling(false); products.saveAndFlush(available);
            users.saveAndFlush(User.builder().username("paused_order_manager").email("paused_order_manager@test.local")
                    .password("unused").fullName("Manager").role(Role.MANAGER).store(first).isActive(true).build());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("paused_order_manager", "unused",
                            org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_MANAGER")));
            workflow.transition(placed.getId(), com.finalproject.enums.OrderStatus.CONFIRMED);
            workflow.transition(placed.getId(), com.finalproject.enums.OrderStatus.PREPARING);
            workflow.transition(placed.getId(), com.finalproject.enums.OrderStatus.DELIVERING);
            workflow.transition(placed.getId(), com.finalproject.enums.OrderStatus.DELIVERED);
            workflow.transition(placed.getId(), com.finalproject.enums.OrderStatus.COMPLETED);
            assertThat(orders.findById(placed.getId()).orElseThrow().getStatus()).isEqualTo(com.finalproject.enums.OrderStatus.COMPLETED);
            assertThat(inventories.findByStoreIdAndProductId(first.getId(), available.getId()).orElseThrow().getQuantity()).isEqualTo(16);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @BeforeEach void fixture() {
        first = branch("Stock branch 1", true, true);
        second = branch("Stock branch 2", true, true);
        closed = branch("Stock closed branch", true, false);
        inactive = branch("Stock inactive branch", false, true);
        category = categories.save(Category.builder().name("Catalog stock flowers").build());
        available = product("Hoa còn hàng", first);
        soldOut = product("Hoa hết hàng", first);
        global = product("Hoa toàn chuỗi", null);
        missingInventory = product("Hoa chưa nhập kho", first);
        stock(available, first, 17); stock(available, second, 83);
        stock(soldOut, first, 0);
        stock(global, first, 3); stock(global, second, 4);
        stock(global, closed, 50); stock(global, inactive, 75);
        customer = users.save(User.builder().username("stock_customer").email("stock_customer@test.local")
                .fullName("Stock Customer").password("unused").role(Role.USER).isActive(true).build());
        inventories.flush();
    }

    Store branch(String name, boolean active, boolean open) {
        return stores.save(Store.builder().name(name).address(name).status(active).isOpen(open).build());
    }
    Product product(String name, Store store) {
        return products.save(Product.builder().name(name).category(category).store(store).sold(100)
                .importPrice(BigDecimal.ONE).priceBuy(new BigDecimal("150000"))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
    }
    void stock(Product product, Store store, int quantity) {
        inventories.save(StoreInventory.builder().product(product).store(store).quantity(quantity).build());
    }

    @Test void stockSummaryCountsOnlyBuyableBranchesAndDefaultsMissingStockToZero() {
        var stock = stockService.summarize(List.of(available, soldOut, global, missingInventory), null);
        assertThat(stock.get(available.getId()).quantity()).isEqualTo(17);
        assertThat(stock.get(global.getId()).quantity()).isEqualTo(7);
        assertThat(stock.get(global.getId()).purchaseStoreId()).isEqualTo(first.getId());
        assertThat(stock.get(soldOut.getId()).quantity()).isZero();
        assertThat(stock.get(missingInventory.getId()).quantity()).isZero();
        assertThat(stockService.summarize(List.of(), null)).isEmpty();
    }

    @Test void homepageShowsStockForGuestsInBothFeaturedAndCatalogCards() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Còn 17 sản phẩm", "Còn 7 sản phẩm", "Hết hàng", "is-out-of-stock");
        String featured = html.substring(html.indexOf("id=\"best-seller\""), html.indexOf("id=\"product-catalog\""));
        String catalog = html.substring(html.indexOf("id=\"productGrid\""));
        assertThat(featured).contains("Còn 17 sản phẩm", "Hết hàng");
        assertThat(catalog).contains("Còn 17 sản phẩm", "Hết hàng");
        assertThat(catalog).containsPattern("<button[^>]*class=\"btn-add-cart\"[^>]*disabled");
    }

    @Test void customerCannotClickBuyForSoldOutProductAndGlobalProductUsesRealStockBranch() throws Exception {
        String html = mvc.perform(get("/").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(cartForm(html, soldOut.getId())).containsPattern("<button[^>]*disabled");
        assertThat(cartForm(html, missingInventory.getId())).containsPattern("<button[^>]*disabled");
        assertThat(cartForm(html, global.getId())).contains("name=\"storeId\" value=\"" + first.getId() + "\"");
        assertThat(cartForm(html, available.getId())).doesNotContain("disabled");
    }

    @Test void branchFilterShowsThatBranchQuantityAndPurchaseTarget() throws Exception {
        String html = mvc.perform(get("/products").param("storeId", second.getId().toString())
                        .with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Còn 4 sản phẩm").doesNotContain("Còn 7 sản phẩm", "id=\"best-seller\"");
        assertThat(cartForm(html, global.getId())).contains("name=\"storeId\" value=\"" + second.getId() + "\"");
        var filtered = stockService.summarize(List.of(available, global), second.getId());
        assertThat(filtered.get(available.getId()).quantity()).isZero();
        assertThat(filtered.get(global.getId()).quantity()).isEqualTo(4);
    }

    static String cartForm(String html, Long productId) {
        var matcher = Pattern.compile("<form[^>]*class=\"[^\"]*ajax-add-to-cart[^\"]*\"[^>]*>(.*?)</form>", Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            if (matcher.group(1).contains("name=\"productId\" value=\"" + productId + "\"")) return matcher.group(1);
        }
        throw new AssertionError("Missing cart form for product " + productId);
    }

    @Test void customerKpiAndPublicSnapshotCountOnlyStockActuallyAvailableToCustomers() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(model().attribute("customerStockQuantity", 24L))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"customer-stock-kpi\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-stock-catalog=\"true\"")));
        mvc.perform(get("/api/public/catalog-stock").param("productIds", available.getId() + "," + global.getId() + "," + soldOut.getId()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.currentStockQuantity").value(24))
                .andExpect(jsonPath("$.products['" + available.getId() + "'].quantity").value(17))
                .andExpect(jsonPath("$.products['" + global.getId() + "'].quantity").value(7))
                .andExpect(jsonPath("$.products['" + soldOut.getId() + "'].quantity").value(0));
    }

    @Test void publicStockDoesNotExposePendingDeletedOrStoppedProducts() throws Exception {
        Product pending = product("Private pending bouquet", first); pending.setIsActive(false); products.save(pending); stock(pending, first, 500);
        Product deleted = product("Deleted bouquet", first); deleted.setIsDeleted(true); products.save(deleted); stock(deleted, first, 600);
        Product stopped = product("Stopped bouquet", first); stopped.setIsSelling(false); products.save(stopped); stock(stopped, first, 700);
        inventories.flush();
        mvc.perform(get("/api/public/catalog-stock").param("productIds", pending.getId() + "," + deleted.getId() + "," + stopped.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(24))
                .andExpect(jsonPath("$.products['" + pending.getId() + "'].quantity").value(0))
                .andExpect(jsonPath("$.products['" + deleted.getId() + "'].quantity").value(0))
                .andExpect(jsonPath("$.products['" + stopped.getId() + "'].quantity").value(0));
    }

    @Test void publicSnapshotRespectsSelectedBranchAndReturnsZeroForUnknownIds() throws Exception {
        mvc.perform(get("/api/public/catalog-stock").param("storeId", second.getId().toString())
                        .param("productIds", global.getId() + "," + available.getId() + "," + Long.MAX_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(4))
                .andExpect(jsonPath("$.products['" + global.getId() + "'].quantity").value(4))
                .andExpect(jsonPath("$.products['" + global.getId() + "'].purchaseStoreId").value(second.getId()))
                .andExpect(jsonPath("$.products['" + available.getId() + "'].quantity").value(0))
                .andExpect(jsonPath("$.products['" + Long.MAX_VALUE + "'].quantity").value(0));
        mvc.perform(get("/products").param("storeId", second.getId().toString()))
                .andExpect(status().isOk()).andExpect(model().attribute("customerStockQuantity", 4L));
    }

    @Test void publicStockBoundsUntrustedParameters() throws Exception {
        mvc.perform(get("/api/public/catalog-stock").param("productIds", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/catalog-stock").param("storeId", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/catalog-stock").param("productIds", "invalid")).andExpect(status().isBadRequest());
        String tooMany = java.util.stream.LongStream.rangeClosed(1, 33).mapToObj(Long::toString)
                .collect(java.util.stream.Collectors.joining(","));
        mvc.perform(get("/api/public/catalog-stock").param("productIds", tooMany)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/catalog-stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStockQuantity").value(24));
    }

    @Test void customerKpiUsesAllPublicProductsNotJustTheEightCardsOnTheCurrentPage() throws Exception {
        for (int i = 0; i < 10; i++) stock(product("Additional bouquet " + i, first), first, 1);
        inventories.flush();
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(model().attribute("customerStockQuantity", 34L));
        mvc.perform(get("/api/public/catalog-stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStockQuantity").value(34));
    }

    @Test void customerStockTotalDoesNotOverflow32BitQuantities() throws Exception {
        stock(product("Large available stock 1", first), first, 2_000_000_000);
        stock(product("Large available stock 2", first), first, 2_000_000_000);
        inventories.flush();
        mvc.perform(get("/api/public/catalog-stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStockQuantity").value(4_000_000_024L));
    }
}
