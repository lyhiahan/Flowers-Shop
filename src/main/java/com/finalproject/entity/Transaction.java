package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "is_up", nullable = false)
    @Builder.Default
    private Boolean isUp = true; // true: cộng tiền (nạp/thu nhập), false: trừ tiền (rút tiền/chi phí)

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "wallet_bucket", nullable = false, length = 20)
    @Builder.Default
    private String walletBucket = "AVAILABLE";

    @Column(length = 30)
    @Builder.Default
    private String type = "ORDER_PAYMENT"; // ORDER_PAYMENT, WITHDRAWAL, COMMISSION_FEE, REFUND

    @Column(name="earned_points")
    private Integer earnedPoints;

    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String description;

    @Column(length = 20)
    @Builder.Default
    private String status = "COMPLETED"; // PENDING, COMPLETED, REJECTED

    private LocalDateTime createdAt;
    @Column(length = 80) private String requestKey;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
