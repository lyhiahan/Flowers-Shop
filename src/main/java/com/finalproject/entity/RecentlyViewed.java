package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recently_viewed")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecentlyViewed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @PrePersist
    public void prePersist() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }
}
