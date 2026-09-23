package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_levels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String name; // Đồng, Bạc, Vàng, Kim Cương

    @Column(name = "min_point", nullable = false, unique = true)
    private Integer minPoint;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
