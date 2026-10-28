package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews") // Filtered unique index is installed by ReviewIntegrityMigration; legacy NULL order IDs remain valid.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 1000)
    private String content; // Tối thiểu 50 ký tự

    @Column(nullable = false)
    private Integer rating; // 1 đến 5 sao

    @Column(name = "image_url", length = 500)
    private String imageUrl;

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

    /** Sample fixtures are visible as demo content, never as verified customer feedback. */
    @Transient
    public boolean isDemo() {
        return user != null && user.getUsername() != null && user.getUsername().startsWith("demo_review_")
                && content != null && content.startsWith("[Đánh giá mẫu] ");
    }

    @Transient
    public Integer getStars() {
        return rating != null ? rating : 5;
    }
}
