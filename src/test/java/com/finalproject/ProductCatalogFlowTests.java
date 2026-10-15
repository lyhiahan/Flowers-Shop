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
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
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
                .andExpect(content().string(containsString("categoriesSubmitted")));
    }
}
