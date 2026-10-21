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
    @Autowired RecentlyViewedRepository recentHistory;
    @Autowired ProductWishlistRepository wishlists;
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

    @Test void checkoutSummaryScrollsNormallyWhileTheHeaderKeepsItsStickyPosition() throws Exception {
        mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("storeId", store.getId().toString())
                        .param("quantity", "1").param("orderType", "BUY"))
                .andExpect(status().is3xxRedirection());
        entityManager.flush(); entityManager.clear();
        String html = page("/checkout");
        var summary = Pattern.compile("<div\\b[^>]*id=\"checkout-order-summary\"[^>]*>").matcher(html);
        assertThat(summary.find()).isTrue();
        assertThat(summary.group()).doesNotContain("sticky", "fixed", "top:", "position:");
        assertThat(html).contains("Sản phẩm đã chọn", "id=\"voucherCollapse\"", "id=\"btnPlaceOrder\"")
                .containsPattern("<header\\b[^>]*class=\"sticky-top ");
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

    @Test void profileTabsHideListMarkersAndKeepTheirPanelTargets() throws Exception {
        for (String tab : List.of("info", "address", "password")) {
            String html = page("/customer/profile?tab=" + tab);
            assertThat(html).containsPattern("\\.profile-nav-pills\\s*\\{[^}]*list-style:\\s*none;")
                    .containsPattern("\\.profile-nav-pills \\.nav-item\\s*\\{[^}]*list-style:\\s*none;")
                    .contains("id=\"profileTab\" role=\"tablist\"", "data-bs-target=\"#tab-info\"",
                            "data-bs-target=\"#tab-address\"", "data-bs-target=\"#tab-password\"");
        }
    }

    @Test void recentlyViewedRemovesTheBannerButKeepsAProtectedClearButton() throws Exception {
        recentHistory.saveAndFlush(RecentlyViewed.builder().user(customer).product(product).build());
        String html = page("/customer/recently-viewed");
        assertThat(html).doesNotContain("Mẫu Hoa Bạn Vừa Xem Gần Đây", "recentCountBadge", "Tiếp tục khám phá",
                        "Lịch sử các sản phẩm bạn đã bấm xem chi tiết.")
                .contains("id=\"recentHistoryActions\"", "id=\"clearHistoryForm\"", "id=\"clearHistoryBtn\"",
                        "action=\"/customer/recently-viewed/clear\"", "method=\"post\"", "name=\"_csrf\"",
                        "Xóa lịch sử đã xem", "id=\"productGrid\"");
    }

    @Test void clearingRecentlyViewedPersistsAndNeverDeletesAnotherCustomersHistory() throws Exception {
        User other = users.saveAndFlush(User.builder().username("other-history").email("other-history@example.test")
                .fullName("Other customer").password("unused").role(Role.USER).isActive(true).build());
        recentHistory.saveAndFlush(RecentlyViewed.builder().user(customer).product(product).build());
        recentHistory.saveAndFlush(RecentlyViewed.builder().user(other).product(product).build());
        for (String route : List.of("/customer/recently-viewed/clear", "/customer/profile/recently-viewed/clear")) {
            mvc.perform(post(route).with(user(customer.getUsername()).roles("USER")).with(csrf())
                            .param("userId", other.getId().toString()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/recently-viewed"))
                    .andExpect(flash().attributeExists("successMessage"));
            entityManager.flush();
            entityManager.clear();
            assertThat(recentHistory.findTop10ByUserIdOrderByViewedAtDesc(customer.getId())).isEmpty();
            assertThat(recentHistory.findTop10ByUserIdOrderByViewedAtDesc(other.getId())).hasSize(1);
            String html = page("/customer/recently-viewed");
            assertThat(html).doesNotContain("id=\"productGrid\"")
                    .containsPattern("<button\\b(?=[^>]*id=\"clearHistoryBtn\")(?=[^>]*disabled)[^>]*>");
        }
        assertThat(products.findById(product.getId())).isPresent();
    }

    @Test void clearingHistoryRequiresAuthenticationCsrfAndPost() throws Exception {
        recentHistory.saveAndFlush(RecentlyViewed.builder().user(customer).product(product).build());
        mvc.perform(post("/customer/recently-viewed/clear").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/customer/recently-viewed/clear").with(anonymous()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/customer/recently-viewed/clear").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recentHistory.findTop10ByUserIdOrderByViewedAtDesc(customer.getId())).hasSize(1);
    }

    @Test void wishlistPromisesKeepResponsiveSpaceFromProductsAndBetweenCards() throws Exception {
        wishlists.saveAndFlush(ProductWishlist.builder().user(customer).product(product).build());
        for (String route : List.of("/customer/profile/wishlist", "/customer/wishlist")) {
            String html = page(route);
            assertThat(html).contains("id=\"wishlistGrid\"");
            var section = Pattern.compile("(?s)<section\\b[^>]*id=\"wishlistPromises\"[^>]*>.*?</section>").matcher(html);
            assertThat(section.find()).isTrue();
            String promises = section.group();
            assertThat(promises).contains("mt-8 md:mt-10", "gap-4 md:gap-6", "Cam Kết Tươi Mới 3 Ngày",
                            "Chụp Ảnh Trước Khi Giao", "Đổi Trả &amp; Hoàn Tiền 100%")
                    .doesNotContain("mt-space-xl", "gap-space-md", "p-space-md", "id=\"wishlistToast\"");
            assertThat(promises.split("class=\"p-5 ", -1)).hasSize(4);
        }
    }

    @Test void customerHeadersNoLongerRenderTheAuxiliaryUtilityStrip() throws Exception {
        for (String route : List.of("/", "/customer/profile", "/products/" + product.getId(), "/customer/following",
                "/customer/followed-stores", "/customer/recently-viewed", "/customer/profile/wishlist")) {
            assertThat(page(route)).as(route).doesNotContain("class=\"top-subbar\"",
                    "Chuỗi Cửa Hàng Hoa Tươi &amp; Điện Hoa Sài Gòn", "Chuỗi Cửa Hàng Hoa Tươi & Điện Hoa Sài Gòn")
                    .contains("HOA TƯƠI SÀI GÒN");
        }
    }

    @Test void wishlistLogoutUsesTheSharedDialogAndKeepsItsCsrfProtectedForm() throws Exception {
        String html = page("/customer/profile/wishlist");
        var form = Pattern.compile("(?s)<form\\b[^>]*id=\"wishlistLogoutForm\"[^>]*>.*?</form>").matcher(html);
        assertThat(form.find()).isTrue();
        assertThat(form.group()).contains("action=\"/logout\"", "method=\"post\"", "name=\"_csrf\"",
                "onsubmit=\"confirmWishlistLogout(event, this)\"");
        assertThat(html).contains("src=\"/js/global-modal.js\"")
                .doesNotContain("id=\"signOutModal\"", "function openModal(", "pt-[124px]");
        assertThat(html).containsPattern("<header\\b[^>]*class=\"sticky top-0 ");
    }

    @Test void bootstrapDialogsAcrossTemplatesRetainTheirOpaqueContentContainers() throws Exception {
        int checked = 0;
        try (var files = Files.walk(Path.of("src/main/resources/templates"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList()) {
                String source = Files.readString(file);
                var modals = Pattern.compile("<div\\b[^>]*\\bclass=\"modal\\s[^\"]*\"[^>]*>").matcher(source);
                java.util.ArrayList<Integer> starts = new java.util.ArrayList<>();
                while (modals.find()) starts.add(modals.start());
                for (int i = 0; i < starts.size(); i++) {
                    String modal = source.substring(starts.get(i), i + 1 < starts.size() ? starts.get(i + 1) : source.length());
                    assertThat(modal).as(file + " modal " + i)
                            .containsPattern("class=\"[^\"]*\\bmodal-dialog\\b[^\"]*\"")
                            .containsPattern("class=\"[^\"]*\\bmodal-content\\b[^\"]*\"");
                    checked++;
                }
            }
        }
        assertThat(checked).isGreaterThan(10);
    }

    @Test void cartThumbnailsUseLocalRosePhotosAndAnInternalFallback() throws Exception {
        product.setImageUrl("/images/demo-rose-bouquet.jpg"); products.saveAndFlush(product);
        for (int day : List.of(1, 2)) {
            mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest").param("productId", product.getId().toString())
                            .param("storeId", store.getId().toString()).param("quantity", "1").param("orderType", "BUY")
                            .param("deliveryDate", java.time.LocalDate.now().plusDays(day).toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
        entityManager.flush(); entityManager.clear();
        String html = page("/cart");
        var images = Pattern.compile("<img\\b[^>]*data-cart-product-image[^>]*>").matcher(html);
        int count = 0;
        while (images.find()) {
            assertThat(images.group()).contains("src=\"/images/demo-rose-bouquet.jpg\"", "width=\"72\"", "height=\"72\"",
                    "data-fallback-src=\"/images/flower-logo.png\"", "alt=\"Storefront bouquet\"");
            count++;
        }
        assertThat(count).isEqualTo(1);
        assertThat(html).contains("src=\"/js/cart-product-images.js\"").doesNotContain("alt=\"Product Image\"");
        Product saved = products.findById(product.getId()).orElseThrow();
        saved.setImageUrl("   "); products.saveAndFlush(saved); entityManager.clear();
        assertThat(page("/cart")).containsPattern("<img\\b(?=[^>]*data-cart-product-image)(?=[^>]*src=\"/images/flower-logo.png\")[^>]*>");
    }

    @Test void localCartImageAssetsAreAvailableWithoutSigningIn() throws Exception {
        byte[] bytes = mvc.perform(get("/images/demo-rose-bouquet.jpg").with(anonymous()))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(800);
        assertThat(image.getHeight()).isGreaterThan(0);
        mvc.perform(get("/images/flower-logo.png").with(anonymous())).andExpect(status().isOk());
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
