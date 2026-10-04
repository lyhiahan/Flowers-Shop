package com.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, unique = true, length = 100)
    private String name; // GrabExpress Hỏa Tốc, Giao Hàng Tiết Kiệm, AhaMove, Shipper Nội Bộ

    @org.hibernate.annotations.Nationalized
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO; // Phí giao hàng tham chiếu

    @Column(length = 50)
    private String phone; // Số điện thoại / Hotline hỗ trợ

    @org.hibernate.annotations.Nationalized
    @Column(name = "coverage_area", length = 255)
    private String coverageArea; // Phạm vi phục vụ (ví dụ: Toàn TP.HCM, Quận 1 & lân cận)

    @org.hibernate.annotations.Nationalized
    @Column(name = "vehicle_type", length = 100)
    private String vehicleType; // Phương tiện: Xe máy giữ ẩm, Xe tải hoa lạnh, Xe van

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted")
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
