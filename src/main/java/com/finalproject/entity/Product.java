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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal importPrice; // Giá nhập để chống bán lỗ

    @Column(nullable = false)
    private BigDecimal priceBuy; // Giá bán đứt

    @Column(nullable = false)
    private BigDecimal priceRentPerDay; // Giá thuê theo ngày

    @Column(nullable = false)
    private BigDecimal depositFee; // Tiền cọc tối thiểu khi thuê

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

    private java.time.LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}