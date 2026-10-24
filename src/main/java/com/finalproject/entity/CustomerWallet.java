package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customer_wallets")
@org.hibernate.annotations.Check(constraints = "balance >= 0")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerWallet {
    @Id @Column(name = "user_id")
    private Long userId;

    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default private BigDecimal balance = BigDecimal.ZERO;

    @Version @Column(nullable = false)
    private Long version;
}
