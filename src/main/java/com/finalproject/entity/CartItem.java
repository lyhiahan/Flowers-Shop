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


    private LocalDate deliveryDate; // Ngày nhận/giao hoa
    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String greetingMessage; // Lời chúc trên thiệp/banner hoa tươi

    public java.math.BigDecimal getBuyAmount() {
        if (orderType != OrderType.BUY || product == null) return java.math.BigDecimal.ZERO;
        return product.getSellingPrice().multiply(java.math.BigDecimal.valueOf(quantity != null ? quantity : 1));
    }

    public java.math.BigDecimal getTotalAmount() {
        return getBuyAmount();
    }
}
