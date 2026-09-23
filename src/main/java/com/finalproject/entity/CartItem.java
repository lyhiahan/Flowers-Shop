package com.finalproject.entity;

import com.finalproject.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cart_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType orderType;

    @Column(nullable = false)
    private Integer quantity;

    private LocalDate rentStartDate;
    private LocalDate rentEndDate;

    private LocalDate deliveryDate; // Ngày nhận/giao hoa
    @Column(length = 500)
    private String greetingMessage; // Lời chúc trên thiệp/banner hoa tươi

    public long getRentalDays() {
        if (orderType == OrderType.RENT && rentStartDate != null && rentEndDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(rentStartDate, rentEndDate);
            return days <= 0 ? 1 : days;
        }
        return 0;
    }

    public java.math.BigDecimal getDailyRate() {
        return product != null && product.getPriceRentPerDay() != null ? product.getPriceRentPerDay() : java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal getRentAmount() {
        if (orderType == OrderType.RENT) {
            return getDailyRate()
                    .multiply(java.math.BigDecimal.valueOf(getRentalDays()))
                    .multiply(java.math.BigDecimal.valueOf(quantity != null ? quantity : 1));
        }
        return java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal getDepositAmount() {
        if (orderType == OrderType.RENT && product != null && product.getDepositFee() != null) {
            return product.getDepositFee().multiply(java.math.BigDecimal.valueOf(quantity != null ? quantity : 1));
        }
        return java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal getBuyAmount() {
        if (orderType == OrderType.BUY && product != null && product.getPriceBuy() != null) {
            return product.getPriceBuy().multiply(java.math.BigDecimal.valueOf(quantity != null ? quantity : 1));
        }
        return java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal getTotalAmount() {
        if (orderType == OrderType.BUY) {
            return getBuyAmount();
        } else {
            return getRentAmount().add(getDepositAmount());
        }
    }
}