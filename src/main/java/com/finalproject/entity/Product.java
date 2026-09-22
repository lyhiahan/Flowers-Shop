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
}