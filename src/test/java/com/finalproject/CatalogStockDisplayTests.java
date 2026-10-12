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
}
