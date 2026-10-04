package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "momo_payment_attempts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MomoPaymentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(nullable = false, unique = true, length = 100) private String gatewayOrderId;
    @Column(nullable = false, unique = true, length = 100) private String requestId;
    @Column(nullable = false, length = 16) private String environment;
    @Column(nullable = false) private BigDecimal amount;
    @org.hibernate.annotations.Nationalized @Column(nullable = false, length = 255) private String orderInfo;
    @Column(nullable = false, length = 1000) private String extraData;
    @Column(length = 100) private String transactionId;
    @Column(nullable = false) @Builder.Default private boolean completed = false;
    @Column(nullable = false) @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
