package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(name = "product_style_values", joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "style_value_id"))
    @Builder.Default
    private java.util.Set<StyleValue> styleValues = new java.util.LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToMany
    @JoinTable(name = "product_additional_categories", joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private java.util.Set<Category> additionalCategories = new java.util.LinkedHashSet<>();

    public java.util.List<Category> getCatalogCategories() {
        var result = new java.util.LinkedHashMap<Long, Category>();
        if (category != null && !Boolean.TRUE.equals(category.getIsDeleted())) result.put(category.getId(), category);
        if (additionalCategories != null) for (Category extra : additionalCategories) {
            if (!Boolean.TRUE.equals(extra.getIsDeleted())) result.putIfAbsent(extra.getId(), extra);
        }
        return new java.util.ArrayList<>(result.values());
    }

    public String attributeLabel(String group) {
        if (styleValues == null) return "";
        return styleValues.stream().filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()) && v.getStyle() != null
                && !Boolean.TRUE.equals(v.getStyle().getIsDeleted())
                && com.finalproject.utils.FlowerAttributes.group(group).equalsIgnoreCase(
                    com.finalproject.utils.FlowerAttributes.group(v.getStyle().getName())))
                .map(StyleValue::getName).sorted().collect(java.util.stream.Collectors.joining(", "));
    }

    public java.util.List<Long> getSelectedStyleValueIds() {
        if (styleValues == null) return java.util.List.of();
        return styleValues.stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .map(StyleValue::getId).toList();
    }

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String name;

    @org.hibernate.annotations.Nationalized
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NVARCHAR)
    @Column(length = org.hibernate.Length.LONG32)
    private String description;

    @Column(nullable = false)
    private BigDecimal importPrice; // Giá nhập để chống bán lỗ

    @Column(nullable = false)
    private BigDecimal priceBuy; // Giá bán đứt

    @Column(nullable = false)
    // Legacy database field: retained for historical records; never used by flower checkout.
    private BigDecimal priceRentPerDay;

    @Column(nullable = false)
    // Legacy database field: retained for historical records; never used by flower checkout.
    private BigDecimal depositFee;

    @Column(name = "promotional_price")
    private BigDecimal promotionalPrice;

    @Builder.Default
    private Integer sold = 0;

    @Builder.Default
    private Double rating = 5.0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_selling", nullable = false)
    @Builder.Default
    private Boolean isSelling = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    private java.time.LocalDateTime createdAt;

    public Boolean getIsDeleted() {
        return isDeleted != null ? isDeleted : false;
    }

    public BigDecimal getSellingPrice() {
        boolean validPromotion = com.finalproject.utils.ProductPrices.valid(promotionalPrice);
        if (priceBuy == null) return validPromotion ? promotionalPrice : BigDecimal.ZERO;
        // Ignore invalid legacy promotions, rather than rendering/charging 0đ after rounding.
        return validPromotion && promotionalPrice.compareTo(priceBuy) <= 0 ? promotionalPrice : priceBuy;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}
