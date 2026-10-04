package com.finalproject.entity;

import com.finalproject.enums.PaymentMethod;
import com.finalproject.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private String transactionCode;
    @Column(length = 16)
    private String momoEnvironment;
    private LocalDateTime paidAt;

    // Phục vụ hoàn cọc tại quầy hoặc chuyển khoản thủ công
    private String refundMethod;
    private String refundNote;
    private LocalDateTime refundedAt;
}
