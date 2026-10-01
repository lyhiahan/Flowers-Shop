package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String imageUrl;

    /** public_id trên Cloudinary – dùng để xóa ảnh cũ khi thay/xóa sản phẩm */
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Builder.Default
    private Boolean isMain = false; // Đánh dấu ảnh đại diện
}