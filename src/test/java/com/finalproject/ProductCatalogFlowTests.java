package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProductCatalogFlowTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StyleRepository styles;
    @Autowired StyleValueRepository styleValues;
    @Autowired com.finalproject.service.ProductAttributeService attributes;
    @Autowired jakarta.persistence.EntityManager em;
    Category main, extra; Product product;

    @BeforeEach void setup() {
        main = categories.save(Category.builder().name("Hoa Sinh Nhật Catalog Test").build());
        extra = categories.save(Category.builder().name("Hoa Tình Yêu Catalog Test").build());
        var store = stores.save(Store.builder().name("Catalog test store").address("Test address").build());
        users.save(User.builder().username("catalog_manager").password("test-only").email("catalog_manager@example.com")
                .fullName("Catalog manager").role(Role.MANAGER).isActive(true).store(store).build());
        product = products.save(Product.builder().name("Catalog test flower").category(main).store(store)
                .priceBuy(new BigDecimal("120000")).importPrice(new BigDecimal("60000"))
                .build());
    }

    @Autowired ReviewRepository reviews;

    @Test void customerRatingUsesOnlyReviewsOfTheSameProduct() {
        product.setRating(4.9);
        products.saveAndFlush(product);
        em.clear();
        assertThat(products.findById(product.getId()).orElseThrow().getCustomerRating()).isNull();
        var customer = users.save(User.builder().username("rating_customer").password("test-only")
                .email("rating_customer@example.com").fullName("Rating customer").role(Role.USER).isActive(true).build());
        for (int stars : new int[]{3, 5}) {
            var reviewer = users.save(User.builder().username("rating_customer_" + stars).password("test-only")
                    .email("rating_customer_" + stars + "@example.com").fullName("Rating customer").role(Role.USER).isActive(true).build());
            reviews.save(Review.builder().product(product).user(reviewer).rating(stars)
                    .content("Customer review for rating aggregation regression").build());
        }
        var other = products.save(Product.builder().name("Other flower").category(main).store(product.getStore())
                .priceBuy(new BigDecimal("120000")).importPrice(new BigDecimal("60000"))
                .build());
        reviews.saveAndFlush(Review.builder().product(other).user(customer).rating(1)
                .content("Unrelated customer review for another flower").build());
        em.clear();
        assertThat(products.findById(product.getId()).orElseThrow().getCustomerRating()).isEqualTo(4.0);
        assertThat(products.findById(other.getId()).orElseThrow().getCustomerRating()).isEqualTo(1.0);
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void managerCanSaveSeveralCategoriesAndEditFormRestoresThem() throws Exception {
        mvc.perform(post("/manager/products/update/" + product.getId()).with(csrf())
                .param("name", product.getName()).param("categoryId", main.getId().toString())
                .param("priceBuy", "120000").param("promotionalPrice", "100000")
                .param("categoriesSubmitted", "true").param("additionalCategoryIds", main.getId().toString(), extra.getId().toString()))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        var saved = products.findById(product.getId()).orElseThrow();
        assertThat(saved.getAdditionalCategories()).extracting(Category::getId).containsExactly(extra.getId());
        assertThat(saved.getSellingPrice()).isEqualByComparingTo("100000");
        mvc.perform(get("/manager/products/edit/" + product.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Danh mục bổ sung")))
                .andExpect(content().string(containsString("extra-category-" + extra.getId())));
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void fractionalPromotionIsRejectedBeforeAnyProductChange() throws Exception {
        mvc.perform(post("/manager/products/update/" + product.getId()).with(csrf())
                .param("name", "Must not change").param("categoryId", main.getId().toString())
                .param("priceBuy", "120000").param("promotionalPrice", "0.01"))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("đồng nguyên")));
        assertThat(products.findById(product.getId()).orElseThrow().getName()).isEqualTo("Catalog test flower");
    }

    @Test void additionalCategoryFilteringIsPaginatedWithoutDuplicateProducts() {
        product.setAdditionalCategories(new LinkedHashSet<>(List.of(extra)));
        products.saveAndFlush(product); em.clear();
        var filtered = products.findAll(ProductSpecification.filter(List.of(main.getId(), extra.getId()), null, null, null,
                null, null, (String)null, null, null, null), PageRequest.of(0, 8));
        assertThat(filtered.getTotalElements()).isEqualTo(1);
        assertThat(products.findByCategoryId(extra.getId())).extracting(Product::getId).containsExactly(product.getId());
        assertThat(products.searchByCategoryAndKeyword(extra.getId(), "Catalog", PageRequest.of(0,8)).getTotalElements()).isEqualTo(1);
        assertThat(products.findAll(ProductSpecification.filter(null, null, null, null, null, null,
                (String)null, null, null, "Tình yêu"))).extracting(Product::getId).containsExactly(product.getId());
    }

    @Test void priceFilteringIgnoresInvalidLegacyPromotions() {
        product.setPromotionalPrice(new BigDecimal("0.01")); products.saveAndFlush(product);
        var filtered = products.findAll(ProductSpecification.filter(null, "Catalog test flower", new BigDecimal("100000"),
                null, null, null, (String)null, null, null, null));
        assertThat(filtered).extracting(Product::getId).containsExactly(product.getId());
        assertThat(product.getSellingPrice()).isEqualByComparingTo("120000");
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void customerProductDetailsShowAllCategoriesAndManagerCreateHasGroupedAttributes() throws Exception {
        product.setAdditionalCategories(new LinkedHashSet<>(List.of(extra))); products.saveAndFlush(product);
        mvc.perform(get("/product/" + product.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString(extra.getName())));
        mvc.perform(get("/manager/products/create")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Danh mục chính")))
                .andExpect(content().string(containsString("categoriesSubmitted")))
                .andExpect(content().string(containsString("name=\"flowerTypes\"")))
                .andExpect(content().string(containsString("name=\"flowerColors\"")))
                .andExpect(content().string(containsString("name=\"flowerStyles\"")));
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void managerCanSaveMixedBouquetAndReloadAllThreeFreeTextGroups() throws Exception {
        product.setAdditionalCategories(new LinkedHashSet<>(List.of(extra))); products.saveAndFlush(product);
        mvc.perform(post("/manager/products/update/" + product.getId()).with(csrf())
                .param("name", product.getName()).param("categoryId", main.getId().toString()).param("priceBuy", "120000")
                .param("flowerAttributesSubmitted", "true")
                .param("flowerTypes", "Hoa hồng, Cẩm tú cầu; Baby\nThanh liễu, hoa HỒNG")
                .param("flowerColors", " Hồng  đào, trắng kem")
                .param("flowerStyles", "Hàn Quốc, Phối tự nhiên"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        var saved = products.findById(product.getId()).orElseThrow();
        assertThat(saved.getStyleValues()).hasSize(8);
        assertThat(saved.attributeLabel("Loại hoa")).contains("Hoa hồng", "Cẩm tú cầu", "Baby", "Thanh liễu");
        assertThat(saved.attributeLabel("Màu sắc")).contains("Hồng đào", "trắng kem");
        assertThat(saved.getAdditionalCategories()).extracting(Category::getId).containsExactly(extra.getId());
        mvc.perform(get("/manager/products/edit/" + saved.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("flower-attribute-source")))
                .andExpect(content().string(containsString("Hồng đào")))
                .andExpect(content().string(containsString("Phối tự nhiên")));
        mvc.perform(get("/product/" + saved.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hồng đào")))
                .andExpect(content().string(containsString("Phối tự nhiên")));
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void managerCreateAcceptsValuesNotPresentInAnySuggestion() throws Exception {
        mvc.perform(post("/manager/products/add").with(csrf())
                .param("name", "Bó phối tự nhập").param("categoryId", main.getId().toString())
                .param("priceBuy", "150000").param("initialStock", "3")
                .param("flowerAttributesSubmitted", "true").param("flowerTypes", "Hoa mao lương, Thanh liễu")
                .param("flowerColors", "Cam đất").param("flowerStyles", "Bất đối xứng"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        var created = products.findAll().stream().filter(p -> p.getName().equals("Bó phối tự nhập")).findFirst().orElseThrow();
        assertThat(created.attributeLabel("Loại hoa")).contains("Hoa mao lương", "Thanh liễu");
        assertThat(created.attributeLabel("Phong cách")).isEqualTo("Bất đối xứng");
        assertThat(created.getStore().getId()).isEqualTo(product.getStore().getId());
    }

    @Test void clearingOptionalGroupsPreservesOtherLegacyAttributesAndOtherProducts() {
        Style shape = styles.save(Style.builder().name("Kiểu dáng").build());
        StyleValue basket = styleValues.save(StyleValue.builder().style(shape).name("Giỏ hoa").build());
        product.getStyleValues().add(basket);
        attributes.assignText(product, "Baby", "Trắng", "Tối giản", true);
        Product second = Product.builder().build();
        attributes.assignText(second, "baby", "trắng", "tối giản", true);
        long valueCount = styleValues.count();
        attributes.assignText(product, "", "", "", true);
        assertThat(product.getStyleValues()).containsExactly(basket);
        assertThat(second.attributeLabel("Loại hoa")).isEqualTo("Baby");
        assertThat(styleValues.count()).isEqualTo(valueCount);
    }

    @Test void legacyAliasesAreVisibleAndMissingSubmissionDoesNotClearValues() {
        Style color = styles.save(Style.builder().name("Tone màu thiết kế").build());
        Style design = styles.save(Style.builder().name("Phong cách thiết kế").build());
        product.getStyleValues().add(styleValues.save(StyleValue.builder().style(color).name("Xanh nhạt").build()));
        product.getStyleValues().add(styleValues.save(StyleValue.builder().style(design).name("Tự nhiên").build()));
        assertThat(product.attributeLabel("Màu sắc")).isEqualTo("Xanh nhạt");
        assertThat(product.attributeLabel("Phong cách")).isEqualTo("Tự nhiên");
        attributes.assignText(product, null, null, null, false);
        assertThat(product.getStyleValues()).hasSize(2);
        attributes.assignText(product, "", "Xanh nhạt", "Tự nhiên", true);
        assertThat(styleValues.count()).isEqualTo(2);
    }

    @Test void invalidFreeTextIsRejectedBeforeCreatingAnySuggestions() {
        long count = styleValues.count();
        assertThatThrownBy(() -> attributes.assignText(product, "Baby", "x".repeat(33), "", true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32");
        assertThat(styleValues.count()).isEqualTo(count);
        assertThat(product.getStyleValues()).isEmpty();
        assertThatThrownBy(() -> com.finalproject.utils.FlowerAttributes.parse(
                java.util.stream.IntStream.range(0,16).mapToObj(i -> "Hoa " + i).collect(java.util.stream.Collectors.joining(","))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("15");
    }

    @Test @WithMockUser(username="catalog_manager", roles="MANAGER")
    void freeTextCannotBeUsedToEditAnotherBranchProduct() throws Exception {
        Store other = stores.save(Store.builder().name("Other branch").address("Other").build());
        product.setStore(other); products.saveAndFlush(product);
        mvc.perform(post("/manager/products/update/" + product.getId()).with(csrf())
                .param("name", product.getName()).param("categoryId", main.getId().toString()).param("priceBuy", "120000")
                .param("flowerAttributesSubmitted", "true").param("flowerTypes", "Hoa lạ"))
                .andExpect(status().isForbidden());
        assertThat(product.getStyleValues()).isEmpty();
    }

    @Test @WithMockUser(username="catalog_admin", roles="ADMIN")
    void adminFormAlsoSavesAndRestoresFreeTextAndEscapesMarkup() throws Exception {
        users.save(User.builder().username("catalog_admin").password("test-only").email("catalog_admin@example.com")
                .fullName("Catalog administrator").role(Role.ADMIN).isActive(true).build());
        mvc.perform(post("/admin/products/edit/" + product.getId()).with(csrf())
                .param("name", product.getName()).param("categoryId", main.getId().toString())
                .param("priceBuy", "120000").param("importPrice", "60000")
                .param("storeId", product.getStore().getId().toString())
                .param("flowerAttributesSubmitted", "true").param("flowerTypes", "Baby")
                .param("flowerColors", "Trắng kem").param("flowerStyles", "<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        assertThat(products.findById(product.getId()).orElseThrow().attributeLabel("Màu sắc")).isEqualTo("Trắng kem");
        mvc.perform(get("/admin/products/edit/" + product.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Trắng kem")))
                .andExpect(content().string(containsString("flower-attribute-source")));
        String html = mvc.perform(get("/product/" + product.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;").doesNotContain("<script>alert(1)</script>");
    }
}
