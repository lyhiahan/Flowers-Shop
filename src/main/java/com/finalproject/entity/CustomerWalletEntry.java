package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_wallet_entries",
        uniqueConstraints = @UniqueConstraint(name = "UK_customer_wallet_order_type", columnNames = {"order_id", "type"}),
        indexes = @Index(name = "IX_customer_wallet_history", columnList = "user_id,id"))
@org.hibernate.annotations.Check(constraints = "amount > 0 and balance_after >= 0 and type in ('PAYMENT','REFUND')")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerWalletEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private CustomerWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 12)
    private String type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
