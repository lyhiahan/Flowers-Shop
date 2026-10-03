package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 150)
    private String name;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @org.hibernate.annotations.Nationalized
    @Column(length = 1000)
    private String bio;

    @Column(length = 500)
    private String avatar;

    /** public_id trên Cloudinary của avatar cửa hàng */
    @Column(name = "avatar_public_id", length = 255)
    private String avatarPublicId;

    @Column(length = 500)
    private String cover;

    /** public_id trên Cloudinary của cover cửa hàng */
    @Column(name = "cover_public_id", length = 255)
    private String coverPublicId;

    @Column(name = "featured_images", columnDefinition = "NVARCHAR(MAX)")
    private String featuredImages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    private Commission commission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_level_id")
    private StoreLevel storeLevel;

    @Builder.Default
    private Boolean status = true; // true: Hoạt động, false: Tạm ngưng do vi phạm

    @Column(name = "is_open")
    @Builder.Default
    private Boolean isOpen = true; // true: Đang mở cửa bán, false: Tạm đóng cửa

    @Builder.Default
    private Integer point = 0;

    @Builder.Default
    private Double rating = 5.0;

    @Column(name = "e_wallet", precision = 18, scale = 2)
    @Builder.Default
    private java.math.BigDecimal eWallet = java.math.BigDecimal.ZERO;

    private Double latitude;
    private Double longitude;

    private java.time.LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}
