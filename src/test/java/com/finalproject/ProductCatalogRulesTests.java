package com.finalproject;

import com.finalproject.dto.request.ProductForm;
import com.finalproject.entity.*;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.service.CategoryService;
import com.finalproject.utils.ProductPrices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCatalogRulesTests {
    @ParameterizedTest @ValueSource(strings={"0.01", "0.99", "0", "-1", "1.01", "100000.5", "10000000000000000"})
    void rejectsFractionalZeroNegativeAndExcessivePromotions(String price) {
        assertThatThrownBy(() -> ProductPrices.validate(new BigDecimal("120000"), new BigDecimal(price)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đồng nguyên");
    }

    @Test void acceptsWholeAmountsAndRejectsPromotionAboveRegularPrice() {
        for (String value : List.of("1", "100000", "100000.00"))
            assertThatCode(() -> ProductPrices.validate(new BigDecimal("120000"), new BigDecimal(value))).doesNotThrowAnyException();
        assertThatCode(() -> ProductPrices.validate(new BigDecimal("120000"), null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProductPrices.validate(new BigDecimal("120000"), new BigDecimal("120001"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void legacyInvalidPromotionNeverBecomesFreeAndValidPromotionStillApplies() {
        var product = Product.builder().priceBuy(new BigDecimal("120000")).build();
        for (String value : List.of("0.01", "0", "-1", "120001", "999.99")) {
            product.setPromotionalPrice(new BigDecimal(value));
            assertThat(product.getSellingPrice()).isEqualByComparingTo("120000");
        }
        product.setPromotionalPrice(new BigDecimal("100000.00"));
        assertThat(product.getSellingPrice()).isEqualByComparingTo("100000");
    }

    @Test void productFormAcceptsJdbcWholeAmountsButRejectsFractionalDong() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator(); var form = new ProductForm();
            form.setImportPrice(new BigDecimal("60000.00")); form.setPriceBuy(new BigDecimal("120000.00"));
            assertThat(validator.validateProperty(form, "priceBuy")).isEmpty();
            assertThat(validator.validateProperty(form, "importPrice")).isEmpty();
            form.setPriceBuy(new BigDecimal("0.01"));
            assertThat(validator.validateProperty(form, "priceBuy")).isNotEmpty();
        }
    }

    @Test void additionalCategoriesAreUniqueAndDoNotRepeatMainCategory() {
        var repository = mock(CategoryRepository.class);
        var service = new CategoryService(repository, mock(jakarta.persistence.EntityManager.class));
        var main = Category.builder().id(1L).name("Hoa Sinh Nhật").build();
        var extra = Category.builder().id(2L).name("Hoa Tình Yêu").build();
        var product = Product.builder().category(main).build();
        when(repository.findAllById(any())).thenReturn(List.of(extra));
        service.assignAdditional(product, List.of(1L, 2L, 2L), true);
        assertThat(product.getAdditionalCategories()).containsExactly(extra);
        assertThat(product.getCatalogCategories()).containsExactly(main, extra);
        service.assignAdditional(product, null, false);
        assertThat(product.getAdditionalCategories()).containsExactly(extra);
        service.assignAdditional(product, null, true);
        assertThat(product.getAdditionalCategories()).isEmpty();
    }

    @Test void hiddenMissingOrInvalidAdditionalCategoriesAreRejected() {
        var repository = mock(CategoryRepository.class);
        var service = new CategoryService(repository, mock(jakarta.persistence.EntityManager.class));
        var product = Product.builder().category(Category.builder().id(1L).build()).build();
        assertThatThrownBy(() -> service.assignAdditional(product, List.of(-1L), true)).isInstanceOf(IllegalArgumentException.class);
        when(repository.findAllById(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.assignAdditional(product, List.of(99L), true)).isInstanceOf(IllegalArgumentException.class);
        when(repository.findAllById(any())).thenReturn(List.of(Category.builder().id(2L).parent(Category.builder().id(3L).isDeleted(true).build()).build()));
        assertThatThrownBy(() -> service.assignAdditional(product, List.of(2L), true)).isInstanceOf(IllegalArgumentException.class);
    }
}
