package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class StorefrontUiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CategoryRepository categories;
    @Autowired EntityManager entityManager;
    User customer;
    Store store;
    Product product;

    @BeforeEach void setup() {
        customer = users.save(User.builder().username("storefront-ui").email("storefront@test.local")
                .fullName("Storefront customer").password("unused").role(Role.USER).isActive(true).build());
        store = stores.save(Store.builder().name("Storefront branch").address("Test address").status(true).isOpen(true).build());
        var category = categories.save(Category.builder().name("Storefront flowers").build());
        product = products.save(Product.builder().name("Storefront bouquet").category(category).store(store)
                .priceBuy(new BigDecimal("150000")).importPrice(BigDecimal.ONE).priceRentPerDay(BigDecimal.ZERO)
                .depositFee(BigDecimal.ZERO).imageUrl("/images/flower-logo.png").build());
        inventories.saveAndFlush(StoreInventory.builder().store(store).product(product).quantity(10).build());
    }

    @Test void storefrontPagesRenderOneIdenticalFooterIncludingTailwindPages() throws Exception {
        String expected = null;
        for (String route : List.of("/", "/products", "/stores", "/stores/" + store.getId(), "/products/" + product.getId(),
                "/cart", "/customer/profile", "/customer/orders", "/customer/following", "/customer/wishlist",
                "/customer/followed-stores", "/customer/recently-viewed", "/notifications")) {
            String html = page(route);
            int start = html.indexOf("<footer class=\"site-footer\"");
            assertThat(start).as(route).isGreaterThanOrEqualTo(0);
            String footer = html.substring(start, html.indexOf("</footer>", start) + 9);
            assertThat(html.split("<footer\\b", -1)).as(route).hasSize(2);
            assertThat(html).contains("href=\"/css/site-footer.css\"");
            assertThat(footer).contains("Chi nhánh Q.1:", "Chi nhánh Q.7:", "Chi nhánh Thủ Đức:", "atelier@hoatuoisaigon.vn", "60–90 phút");
            if (expected == null) expected = footer;
            else assertThat(footer).as(route).isEqualTo(expected);
            preview(route.equals("/") ? "home-empty.html" : route.replaceAll("[^a-zA-Z0-9]", "_") + ".html", html);
        }
    }

    @Test void ajaxAddReturnsSavedQuantityAndReloadKeepsTheSameCartBadge() throws Exception {
        preview("home-empty.html", page("/"));
        for (int expected : List.of(1, 2, 3)) {
            mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest").param("productId", product.getId().toString())
                            .param("storeId", store.getId().toString()).param("orderType", "BUY").param("quantity", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.count").value(expected));
            mvc.perform(get("/").with(user(customer.getUsername()).roles("USER")))
                    .andExpect(model().attribute("cartItemCount", expected));
        }
        String html = page("/");
        assertThat(html).contains("id=\"headerCartLink\"", "id=\"headerCartBadge\"", "src=\"/js/home-cart.js\"")
                .containsPattern("id=\"headerCartBadge\"[^>]*>3</span>");
        preview("home-filled.html", html);
        // HTTP requests normally use fresh persistence contexts, unlike this transactional fixture.
        entityManager.flush();
        entityManager.clear();
        String cartHtml = page("/cart");
        var summary = Pattern.compile("<div\\b[^>]*id=\"cart-order-summary\"[^>]*>").matcher(cartHtml);
        assertThat(summary.find()).isTrue();
        assertThat(summary.group()).doesNotContain("sticky-top", "top:", "position:");
        assertThat(cartHtml).contains("header-main-bar sticky-top");
        preview("cart-filled.html", cartHtml);
    }

    @Test void everyStorefrontFooterUsesTheSharedFragmentRatherThanAnInlineCopy() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/templates/customer"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList()) {
                String html = Files.readString(file);
                var footers = Pattern.compile("<footer\\b([^>]*)>").matcher(html);
                while (footers.find()) assertThat(footers.group(1)).as(file.toString()).contains("fragments/floral :: footer");
            }
        }
    }

    @Test void productDetailHasOneStableFollowControlAndReloadsTheRealFollowerCount() throws Exception {
        String url="/products/"+product.getId();
        String before=page(url);
        assertThat(before).contains("product-follow-group", "id=\"btnDetailFollow\"")
                .doesNotContain("btnFollowProductTop", "action-sub-row", "Đã lưu yêu thích");
        preview("product-before.html",before);
        mvc.perform(post("/customer/following/toggle/"+product.getId()).with(user(customer.getUsername()).roles("USER"))
                        .with(csrf()).header("X-Requested-With","XMLHttpRequest"))
                .andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.followerCount").value(1));
        String after=page(url);
        assertThat(after).contains("aria-pressed=\"true\"").containsPattern("id=\"followerCount\"[^>]*>1</span>");
        preview("product-after.html",after);
        mvc.perform(post("/customer/following/toggle/"+product.getId()).with(user(customer.getUsername()).roles("USER"))
                        .with(csrf()).header("X-Requested-With","XMLHttpRequest"))
                .andExpect(jsonPath("$.liked").value(false)).andExpect(jsonPath("$.followerCount").value(0));
        assertThat(page("/")).doesNotContain("customer-stock-kpi", "Sản phẩm còn hàng toàn chuỗi");
    }

    private String page(String route) throws Exception {
        return mvc.perform(get(route).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    private void preview(String name, String html) throws Exception {
        Path directory = Path.of("target/storefront-preview");
        Files.createDirectories(directory); Files.writeString(directory.resolve(name), html);
    }
}
