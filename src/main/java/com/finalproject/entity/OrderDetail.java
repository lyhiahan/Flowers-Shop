package com.finalproject.entity;

import com.finalproject.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice; // Giá bán hoặc đơn giá thuê/ngày tại thời điểm chốt

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OrderType orderType;

    private BigDecimal depositFee;
    private LocalDate rentStartDate;
    private LocalDate rentEndDate;
    private LocalDate actualReturnDate;

    public long getRentalDays() {
        if (orderType == OrderType.RENT && rentStartDate != null && rentEndDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(rentStartDate, rentEndDate);
            return days <= 0 ? 1 : days;
        }
        return 0;
    }

    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        if (orderType == OrderType.RENT) {
            long days = getRentalDays();
            return unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(days > 0 ? days : 1));
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}