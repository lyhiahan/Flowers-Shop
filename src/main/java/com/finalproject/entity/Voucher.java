package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // FLOWER10, FREESHIP, TET2026

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // PERCENT, FIXED_AMOUNT, FREE_SHIP

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_value")
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;

    // NULL nếu là voucher toàn hệ thống của Admin, có store nếu là voucher riêng của Vendor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 100;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
