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
    Store first, second, closed, inactive;
    Product available, soldOut, global, missingInventory;
    Category category;
    User customer;

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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"customer-stock-kpi\"")))
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
